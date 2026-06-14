import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../models/sharing_post_response.dart';
import '../services/sharing_repository.dart';

final sharingRepositoryProvider = Provider<SharingRepository>((ref) {
  return SharingRepository(ref.watch(dioProvider));
});

/// 카테고리 필터 상태.
final sharingCategoryFilterProvider = StateProvider<String?>((ref) => null);

/// 검색어 상태.
final sharingQueryProvider = StateProvider<String?>((ref) => null);

/// 나눔 피드 목록 + 낙관적 좋아요.
///
/// FutureProvider 대신 AsyncNotifier로 둬서, 좋아요 시 전체 재조회(invalidate) 없이
/// 해당 글의 likeCount/likedByMe만 즉시 갱신한다(낙관적). 실패하면 롤백한다.
class SharingFeedNotifier
    extends AutoDisposeAsyncNotifier<SharingPostListResponse> {
  @override
  Future<SharingPostListResponse> build() {
    final repository = ref.watch(sharingRepositoryProvider);
    final category = ref.watch(sharingCategoryFilterProvider);
    final query = ref.watch(sharingQueryProvider);
    return repository.getSharingPosts(category: category, query: query);
  }

  /// 좋아요 토글 — 즉시 로컬 갱신 후 서버 반영. 실패 시 원래 상태로 롤백한다.
  Future<void> toggleLike(int postId) async {
    final current = state.valueOrNull;
    if (current == null) return;
    final index = current.items.indexWhere((e) => e.id == postId);
    if (index < 0) return;
    final original = current.items[index];
    final liked = original.likedByMe;

    // 1) 낙관적 갱신
    state = AsyncData(_replace(
      current,
      index,
      original.copyWith(
        likedByMe: !liked,
        likeCount: original.likeCount + (liked ? -1 : 1),
      ),
    ));

    // 2) 서버 반영
    try {
      final repo = ref.read(sharingRepositoryProvider);
      liked ? await repo.unlike(postId) : await repo.like(postId);
    } catch (_) {
      // 3) 실패 → 롤백(현재 인덱스를 다시 찾아 원본 복원)
      final now = state.valueOrNull;
      if (now != null) {
        final i = now.items.indexWhere((e) => e.id == postId);
        if (i >= 0) state = AsyncData(_replace(now, i, original));
      }
      rethrow; // 화면이 실패 안내할 수 있게 전파
    }
  }

  /// 저장(북마크) 토글 — 즉시 로컬 갱신 후 서버 반영. 실패 시 롤백. (좋아요와 동일 패턴)
  Future<void> toggleBookmark(int postId) async {
    final current = state.valueOrNull;
    if (current == null) return;
    final index = current.items.indexWhere((e) => e.id == postId);
    if (index < 0) return;
    final original = current.items[index];
    final bookmarked = original.bookmarkedByMe;

    state = AsyncData(_replace(
      current,
      index,
      original.copyWith(bookmarkedByMe: !bookmarked),
    ));

    try {
      final repo = ref.read(sharingRepositoryProvider);
      bookmarked ? await repo.unbookmark(postId) : await repo.bookmark(postId);
    } catch (_) {
      final now = state.valueOrNull;
      if (now != null) {
        final i = now.items.indexWhere((e) => e.id == postId);
        if (i >= 0) state = AsyncData(_replace(now, i, original));
      }
      rethrow;
    }
  }

  SharingPostListResponse _replace(
      SharingPostListResponse res, int index, SharingPostItem item) {
    final items = [...res.items];
    items[index] = item;
    return SharingPostListResponse(items: items, hasNext: res.hasNext);
  }
}

final sharingPostsProvider = AsyncNotifierProvider.autoDispose<
    SharingFeedNotifier, SharingPostListResponse>(SharingFeedNotifier.new);

/// 내 저장(북마크) 목록 + 낙관적 저장 해제.
///
/// 저장 목록 화면에서 저장 해제 시 해당 카드를 즉시 목록에서 빼고 서버에 반영한다.
/// 실패하면 원래 자리로 되돌린다.
class BookmarksNotifier
    extends AutoDisposeAsyncNotifier<SharingPostListResponse> {
  @override
  Future<SharingPostListResponse> build() {
    return ref.watch(sharingRepositoryProvider).getBookmarks();
  }

  /// 저장 해제 — 목록에서 즉시 제거 후 서버 반영. 실패 시 원위치로 복원한다.
  Future<void> removeBookmark(int postId) async {
    final current = state.valueOrNull;
    if (current == null) return;
    final index = current.items.indexWhere((e) => e.id == postId);
    if (index < 0) return;
    final original = current.items[index];

    final remaining = [...current.items]..removeAt(index);
    state = AsyncData(
        SharingPostListResponse(items: remaining, hasNext: current.hasNext));

    try {
      await ref.read(sharingRepositoryProvider).unbookmark(postId);
    } catch (_) {
      final now = state.valueOrNull;
      if (now != null) {
        final restored = [...now.items];
        restored.insert(index.clamp(0, restored.length), original);
        state = AsyncData(
            SharingPostListResponse(items: restored, hasNext: now.hasNext));
      }
      rethrow;
    }
  }
}

final bookmarksProvider = AsyncNotifierProvider.autoDispose<BookmarksNotifier,
    SharingPostListResponse>(BookmarksNotifier.new);

/// 내 나눔 글 목록 (M-05).
///
/// 왜 이렇게 짰냐면:
/// status 미지정으로 공개+숨김을 한 목록에 받아 상태 뱃지로 구분한다(v1).
/// autoDispose = 화면 떠나면 캐시 정리. 숨김/되돌리기/삭제 후 invalidate로 새로고침한다.
final mySharingPostsProvider =
    FutureProvider.autoDispose<MySharingPostListResponse>((ref) {
  return ref.watch(sharingRepositoryProvider).getMySharingPosts();
});

/// 나눔 상세 + 댓글 묶음 (S-02 화면 데이터).
class SharingDetailData {
  final SharingPostDetail detail;
  final List<CommentItem> comments;

  const SharingDetailData({required this.detail, required this.comments});
}

/// 나눔 상세(S-02) 데이터 — 상세 + (댓글 허용 시) 댓글 목록.
///
/// ② Riverpod 일관화: 화면이 직접 setState로 조회하던 것을 Provider로 일원화한다.
/// 댓글/삭제/신고 후 `ref.invalidate(sharingPostDetailProvider(postId))`로 새로고침한다.
/// 좋아요는 [toggleLike]로 낙관적 갱신 — 본문·댓글 재조회 없이 좋아요 수/하트만 즉시 바꾼다(피드와 동일).
class SharingDetailNotifier
    extends AutoDisposeFamilyAsyncNotifier<SharingDetailData, int> {
  @override
  Future<SharingDetailData> build(int postId) async {
    final repo = ref.watch(sharingRepositoryProvider);
    final detail = await repo.getSharingPostDetail(postId);
    final comments = detail.commentsEnabled
        ? await repo.getComments(postId)
        : <CommentItem>[];
    return SharingDetailData(detail: detail, comments: comments);
  }

  /// 좋아요 토글 — 즉시 로컬 갱신(낙관적) 후 서버 반영. 실패 시 원래 상태로 롤백한다.
  /// 댓글 목록은 그대로 두고 detail의 좋아요 상태/수만 바꾸므로 화면이 깜빡이지 않는다.
  Future<void> toggleLike() async {
    final current = state.valueOrNull;
    if (current == null) return;
    final original = current.detail;
    final liked = original.likedByMe;

    // 1) 낙관적 갱신
    state = AsyncData(SharingDetailData(
      detail: original.copyWith(
        likedByMe: !liked,
        likeCount: original.likeCount + (liked ? -1 : 1),
      ),
      comments: current.comments,
    ));

    // 2) 서버 반영
    try {
      final repo = ref.read(sharingRepositoryProvider);
      liked ? await repo.unlike(original.id) : await repo.like(original.id);
    } catch (_) {
      // 3) 실패 → 롤백(댓글은 현재 상태 유지)
      final now = state.valueOrNull;
      if (now != null) {
        state = AsyncData(
            SharingDetailData(detail: original, comments: now.comments));
      }
      rethrow; // 화면이 실패 안내할 수 있게 전파
    }
  }

  /// 저장(북마크) 토글 — 낙관적 갱신 후 서버 반영. 실패 시 롤백. (좋아요와 동일 패턴)
  Future<void> toggleBookmark() async {
    final current = state.valueOrNull;
    if (current == null) return;
    final original = current.detail;
    final bookmarked = original.bookmarkedByMe;

    state = AsyncData(SharingDetailData(
      detail: original.copyWith(bookmarkedByMe: !bookmarked),
      comments: current.comments,
    ));

    try {
      final repo = ref.read(sharingRepositoryProvider);
      bookmarked
          ? await repo.unbookmark(original.id)
          : await repo.bookmark(original.id);
    } catch (_) {
      final now = state.valueOrNull;
      if (now != null) {
        state = AsyncData(
            SharingDetailData(detail: original, comments: now.comments));
      }
      rethrow;
    }
  }
}

final sharingPostDetailProvider = AsyncNotifierProvider.autoDispose
    .family<SharingDetailNotifier, SharingDetailData, int>(
        SharingDetailNotifier.new);
