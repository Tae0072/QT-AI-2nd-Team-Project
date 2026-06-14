import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/sharing/models/sharing_post_response.dart';
import 'package:qtai_app/features/sharing/providers/sharing_providers.dart';
import 'package:qtai_app/features/sharing/services/sharing_repository.dart';

/// 나눔 저장(북마크)의 낙관적 업데이트(즉시 갱신 + 실패 롤백)를 검증한다.
SharingPostItem _post(int id, {required bool bookmarked}) => SharingPostItem(
      id: id,
      nicknameSnapshot: '하늘QT',
      titleSnapshot: '제목',
      category: 'PRAYER',
      bodyPreview: '본문',
      likeCount: 0,
      commentCount: 0,
      likedByMe: false,
      bookmarkedByMe: bookmarked,
    );

class _FakeSharingRepository extends SharingRepository {
  _FakeSharingRepository({
    this.feed = const [],
    this.bookmarks = const [],
    this.throwOnAction = false,
  }) : super(Dio());

  final List<SharingPostItem> feed;
  final List<SharingPostItem> bookmarks;
  final bool throwOnAction;
  final List<int> bookmarkCalls = [];
  final List<int> unbookmarkCalls = [];

  @override
  Future<SharingPostListResponse> getSharingPosts(
          {String? category, String? query, int page = 0}) async =>
      SharingPostListResponse(items: feed, hasNext: false);

  @override
  Future<SharingPostListResponse> getBookmarks({int page = 0}) async =>
      SharingPostListResponse(items: bookmarks, hasNext: false);

  @override
  Future<void> bookmark(int postId) async {
    if (throwOnAction) {
      throw DioException(requestOptions: RequestOptions(path: '/bookmark'));
    }
    bookmarkCalls.add(postId);
  }

  @override
  Future<void> unbookmark(int postId) async {
    if (throwOnAction) {
      throw DioException(requestOptions: RequestOptions(path: '/bookmark'));
    }
    unbookmarkCalls.add(postId);
  }
}

ProviderContainer _container(_FakeSharingRepository fake) {
  final c = ProviderContainer(
    overrides: [sharingRepositoryProvider.overrideWithValue(fake)],
  );
  addTearDown(c.dispose);
  return c;
}

void main() {
  test('피드: 저장 누르면 즉시 채워지고 서버에 bookmark를 보낸다', () async {
    final fake = _FakeSharingRepository(feed: [_post(1, bookmarked: false)]);
    final c = _container(fake);
    await c.read(sharingPostsProvider.future);

    final future = c.read(sharingPostsProvider.notifier).toggleBookmark(1);
    final optimistic = c.read(sharingPostsProvider).value!.items.first;
    expect(optimistic.bookmarkedByMe, isTrue);

    await future;
    expect(fake.bookmarkCalls, [1]);
    expect(fake.unbookmarkCalls, isEmpty);
  });

  test('피드: 이미 저장한 글은 unbookmark를 보낸다', () async {
    final fake = _FakeSharingRepository(feed: [_post(1, bookmarked: true)]);
    final c = _container(fake);
    await c.read(sharingPostsProvider.future);

    await c.read(sharingPostsProvider.notifier).toggleBookmark(1);
    expect(c.read(sharingPostsProvider).value!.items.first.bookmarkedByMe, isFalse);
    expect(fake.unbookmarkCalls, [1]);
  });

  test('피드: 서버 실패 시 저장 상태를 롤백한다', () async {
    final fake = _FakeSharingRepository(
      feed: [_post(1, bookmarked: false)],
      throwOnAction: true,
    );
    final c = _container(fake);
    await c.read(sharingPostsProvider.future);

    await expectLater(
      c.read(sharingPostsProvider.notifier).toggleBookmark(1),
      throwsA(isA<DioException>()),
    );
    expect(c.read(sharingPostsProvider).value!.items.first.bookmarkedByMe, isFalse);
  });

  test('저장목록: 해제하면 목록에서 즉시 제거되고 서버에 unbookmark를 보낸다', () async {
    final fake = _FakeSharingRepository(
        bookmarks: [_post(1, bookmarked: true), _post(2, bookmarked: true)]);
    final c = _container(fake);
    await c.read(bookmarksProvider.future);

    await c.read(bookmarksProvider.notifier).removeBookmark(1);
    final items = c.read(bookmarksProvider).value!.items;
    expect(items.map((e) => e.id), [2]);
    expect(fake.unbookmarkCalls, [1]);
  });

  test('저장목록: 서버 실패 시 제거를 되돌린다', () async {
    final fake = _FakeSharingRepository(
      bookmarks: [_post(1, bookmarked: true), _post(2, bookmarked: true)],
      throwOnAction: true,
    );
    final c = _container(fake);
    await c.read(bookmarksProvider.future);

    await expectLater(
      c.read(bookmarksProvider.notifier).removeBookmark(1),
      throwsA(isA<DioException>()),
    );
    final ids = c.read(bookmarksProvider).value!.items.map((e) => e.id).toList();
    expect(ids, containsAll(<int>[1, 2]));
  });
}
