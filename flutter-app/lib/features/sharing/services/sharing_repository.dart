import 'package:dio/dio.dart';

import '../models/sharing_post_response.dart';

/// 나눔 API 호출 레이어.
class SharingRepository {
  final Dio _dio;

  SharingRepository(this._dio);

  /// 나눔 피드 목록 조회.
  Future<SharingPostListResponse> getSharingPosts({
    String? category,
    String? query,
    int page = 0,
    int size = 10,
  }) async {
    final response = await _dio.get('/sharing-posts', queryParameters: {
      if (category != null) 'category': category,
      if (query != null && query.isNotEmpty) 'q': query,
      'page': page,
      'size': size,
    });
    final data = response.data['data'] as Map<String, dynamic>;
    return SharingPostListResponse.fromJson(data);
  }

  /// 나눔 글 상세 조회.
  Future<SharingPostDetail> getSharingPostDetail(int postId) async {
    final response = await _dio.get('/sharing-posts/$postId');
    final data = response.data['data'] as Map<String, dynamic>;
    return SharingPostDetail.fromJson(data);
  }

  /// 좋아요.
  Future<void> like(int postId) async {
    await _dio.post('/sharing-posts/$postId/like');
  }

  /// 좋아요 취소.
  Future<void> unlike(int postId) async {
    await _dio.delete('/sharing-posts/$postId/like');
  }

  /// 저장(북마크). POST /sharing-posts/{id}/bookmark (201). 멱등.
  Future<void> bookmark(int postId) async {
    await _dio.post('/sharing-posts/$postId/bookmark');
  }

  /// 저장 해제. DELETE /sharing-posts/{id}/bookmark (204). 멱등.
  Future<void> unbookmark(int postId) async {
    await _dio.delete('/sharing-posts/$postId/bookmark');
  }

  /// 내 저장 목록 (GET /api/v1/me/bookmarks). 피드와 동일한 형식, 최근 저장순.
  Future<SharingPostListResponse> getBookmarks({int page = 0}) async {
    final response = await _dio.get('/me/bookmarks', queryParameters: {
      'page': page,
      'size': 20,
    });
    final data = response.data['data'] as Map<String, dynamic>;
    return SharingPostListResponse.fromJson(data);
  }

  /// 나눔 글 삭제.
  Future<void> deletePost(int postId) async {
    await _dio.delete('/sharing-posts/$postId');
  }

  /// 내 나눔 글 목록 (GET /api/v1/me/sharing-posts, 04 §4.4.5, 화면 M-05).
  ///
  /// status 생략 시 서버가 공개+숨김을 함께 반환한다(삭제 제외).
  Future<MySharingPostListResponse> getMySharingPosts({
    String? status,
    int page = 0,
  }) async {
    final response = await _dio.get('/me/sharing-posts', queryParameters: {
      if (status != null) 'status': status,
      'page': page,
      'size': 20,
    });
    final data = response.data['data'] as Map<String, dynamic>;
    return MySharingPostListResponse.fromJson(data);
  }

  /// 나눔 글 숨김 (PATCH /sharing-posts/{id}/hide, 204). PUBLISHED→HIDDEN.
  Future<void> hidePost(int postId) async {
    await _dio.patch('/sharing-posts/$postId/hide');
  }

  /// 나눔 글 되돌리기 (PATCH /sharing-posts/{id}/show, 204). HIDDEN→PUBLISHED.
  Future<void> showPost(int postId) async {
    await _dio.patch('/sharing-posts/$postId/show');
  }

  /// 노트를 나눔에 공유.
  Future<void> publishNote(int noteId, {bool commentsEnabled = true}) async {
    await _dio.post('/notes/$noteId/share', data: {
      'confirmNicknamePublic': true,
      'commentsEnabled': commentsEnabled,
    });
  }

  /// 댓글 목록 조회 (GET /sharing-posts/{postId}/comments, 04 §4.4.4).
  Future<List<CommentItem>> getComments(int postId) async {
    final response = await _dio.get('/sharing-posts/$postId/comments');
    final data = response.data['data'] as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content
        .map((e) => CommentItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// 댓글 작성 (POST /sharing-posts/{postId}/comments, 04 §4.4.4).
  Future<void> createComment(int postId, String body) async {
    await _dio.post('/sharing-posts/$postId/comments', data: {'body': body});
  }

  /// 댓글 삭제 (DELETE /comments/{commentId}, 04 §4.4.4).
  Future<void> deleteComment(int commentId) async {
    await _dio.delete('/comments/$commentId');
  }

  /// 신고 접수 (POST /reports, 04 §4.4.7).
  ///
  /// targetType: POST/COMMENT, reason: 자유 코드(SPAM/HATE/SEXUAL/ETC 등).
  Future<void> report({
    required String targetType,
    required int targetId,
    required String reason,
    String? detail,
  }) async {
    await _dio.post('/reports', data: {
      'targetType': targetType,
      'targetId': targetId,
      'reason': reason,
      if (detail != null && detail.isNotEmpty) 'detail': detail,
    });
  }
}
