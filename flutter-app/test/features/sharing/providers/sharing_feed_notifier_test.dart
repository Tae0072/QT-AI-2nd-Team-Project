import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/sharing/models/sharing_post_response.dart';
import 'package:qtai_app/features/sharing/providers/sharing_providers.dart';
import 'package:qtai_app/features/sharing/services/sharing_repository.dart';

/// 나눔 피드 좋아요의 낙관적 업데이트(즉시 갱신 + 실패 롤백)를 검증한다(QA ③).
SharingPostItem _post(int id, {required bool liked, required int count}) =>
    SharingPostItem(
      id: id,
      nicknameSnapshot: '하늘QT',
      titleSnapshot: '제목',
      category: 'PRAYER',
      bodyPreview: '본문',
      likeCount: count,
      commentCount: 0,
      likedByMe: liked,
    );

class _FakeSharingRepository extends SharingRepository {
  _FakeSharingRepository({required this.posts, this.throwOnAction = false})
      : super(Dio());

  final List<SharingPostItem> posts;
  final bool throwOnAction;
  final List<int> likeCalls = [];
  final List<int> unlikeCalls = [];

  @override
  Future<SharingPostListResponse> getSharingPosts(
          {String? category, String? query, int page = 0, int size = 10}) async =>
      SharingPostListResponse(items: posts, hasNext: false);

  @override
  Future<void> like(int postId) async {
    if (throwOnAction) {
      throw DioException(requestOptions: RequestOptions(path: '/like'));
    }
    likeCalls.add(postId);
  }

  @override
  Future<void> unlike(int postId) async {
    if (throwOnAction) {
      throw DioException(requestOptions: RequestOptions(path: '/like'));
    }
    unlikeCalls.add(postId);
  }
}

ProviderContainer _container(_FakeSharingRepository fake) {
  final c = ProviderContainer(
    overrides: [sharingRepositoryProvider.overrideWithValue(fake)],
  );
  addTearDown(c.dispose);
  return c;
}

SharingPostItem _first(ProviderContainer c) =>
    c.read(sharingPostsProvider).value!.items.first;

void main() {
  test('좋아요 누르면 즉시 낙관적 갱신(+1, 채움)하고 서버에 like를 보낸다', () async {
    final fake = _FakeSharingRepository(posts: [_post(1, liked: false, count: 5)]);
    final c = _container(fake);
    await c.read(sharingPostsProvider.future);

    // toggleLike의 낙관적 갱신은 await 이전에 동기로 일어난다.
    final future = c.read(sharingPostsProvider.notifier).toggleLike(1);
    final optimistic = _first(c);
    expect(optimistic.likedByMe, isTrue);
    expect(optimistic.likeCount, 6);

    await future;
    expect(fake.likeCalls, [1]);
    expect(fake.unlikeCalls, isEmpty);
  });

  test('이미 좋아요한 글은 unlike를 보내고 수를 줄인다', () async {
    final fake = _FakeSharingRepository(posts: [_post(1, liked: true, count: 5)]);
    final c = _container(fake);
    await c.read(sharingPostsProvider.future);

    await c.read(sharingPostsProvider.notifier).toggleLike(1);
    final after = _first(c);
    expect(after.likedByMe, isFalse);
    expect(after.likeCount, 4);
    expect(fake.unlikeCalls, [1]);
  });

  test('서버 실패 시 원래 상태로 롤백한다', () async {
    final fake = _FakeSharingRepository(
      posts: [_post(1, liked: false, count: 5)],
      throwOnAction: true,
    );
    final c = _container(fake);
    await c.read(sharingPostsProvider.future);

    await expectLater(
      c.read(sharingPostsProvider.notifier).toggleLike(1),
      throwsA(isA<DioException>()),
    );

    final after = _first(c);
    expect(after.likedByMe, isFalse); // 롤백
    expect(after.likeCount, 5);
  });
}
