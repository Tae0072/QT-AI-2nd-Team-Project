import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/sharing/models/sharing_post_response.dart';
import 'package:qtai_app/features/sharing/providers/sharing_providers.dart';
import 'package:qtai_app/features/sharing/services/sharing_repository.dart';

/// 나눔 상세(S-02) 좋아요의 낙관적 업데이트(즉시 갱신 + 실패 롤백)를 검증한다(QA ③).
SharingPostDetail _detail({required bool liked, required int count}) =>
    SharingPostDetail(
      id: 1,
      noteId: 10,
      memberId: 20,
      nicknameSnapshot: '하늘QT',
      titleSnapshot: '제목',
      bodySnapshot: '본문',
      category: 'PRAYER',
      commentsEnabled: true,
      likeCount: count,
      commentCount: 1,
      likedByMe: liked,
      ownedByMe: false,
    );

class _FakeSharingRepository extends SharingRepository {
  _FakeSharingRepository({required this.detail, this.throwOnAction = false})
      : super(Dio());

  final SharingPostDetail detail;
  final bool throwOnAction;
  final List<int> likeCalls = [];
  final List<int> unlikeCalls = [];

  @override
  Future<SharingPostDetail> getSharingPostDetail(int postId) async => detail;

  @override
  Future<List<CommentItem>> getComments(int postId) async =>
      [CommentItem(id: 99, nickname: '은혜', body: '댓글', ownedByMe: false)];

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

SharingDetailData _read(ProviderContainer c) =>
    c.read(sharingPostDetailProvider(1)).value!;

void main() {
  test('좋아요 누르면 즉시 +1·채움하고 서버에 like를 보낸다(댓글은 유지)', () async {
    final fake = _FakeSharingRepository(detail: _detail(liked: false, count: 5));
    final c = _container(fake);
    await c.read(sharingPostDetailProvider(1).future);

    final future =
        c.read(sharingPostDetailProvider(1).notifier).toggleLike();
    final optimistic = _read(c);
    expect(optimistic.detail.likedByMe, isTrue);
    expect(optimistic.detail.likeCount, 6);
    expect(optimistic.comments, hasLength(1)); // 댓글 재조회 없이 유지

    await future;
    expect(fake.likeCalls, [1]);
    expect(fake.unlikeCalls, isEmpty);
  });

  test('이미 좋아요한 글은 unlike를 보내고 수를 줄인다', () async {
    final fake = _FakeSharingRepository(detail: _detail(liked: true, count: 5));
    final c = _container(fake);
    await c.read(sharingPostDetailProvider(1).future);

    await c.read(sharingPostDetailProvider(1).notifier).toggleLike();
    final after = _read(c);
    expect(after.detail.likedByMe, isFalse);
    expect(after.detail.likeCount, 4);
    expect(fake.unlikeCalls, [1]);
  });

  test('서버 실패 시 원래 상태로 롤백한다(댓글 유지)', () async {
    final fake = _FakeSharingRepository(
      detail: _detail(liked: false, count: 5),
      throwOnAction: true,
    );
    final c = _container(fake);
    await c.read(sharingPostDetailProvider(1).future);

    await expectLater(
      c.read(sharingPostDetailProvider(1).notifier).toggleLike(),
      throwsA(isA<DioException>()),
    );

    final after = _read(c);
    expect(after.detail.likedByMe, isFalse); // 롤백
    expect(after.detail.likeCount, 5);
    expect(after.comments, hasLength(1));
  });
}
