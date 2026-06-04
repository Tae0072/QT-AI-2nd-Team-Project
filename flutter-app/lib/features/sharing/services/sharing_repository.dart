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
  }) async {
    final response = await _dio.get('/sharing-posts', queryParameters: {
      if (category != null) 'category': category,
      if (query != null && query.isNotEmpty) 'q': query,
      'page': page,
      'size': 20,
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

  /// 나눔 글 삭제.
  Future<void> deletePost(int postId) async {
    await _dio.delete('/sharing-posts/$postId');
  }

  /// 노트를 나눔에 공유.
  Future<void> publishNote(int noteId, {bool commentsEnabled = true}) async {
    await _dio.post('/notes/$noteId/share', data: {
      'confirmNicknamePublic': true,
      'commentsEnabled': commentsEnabled,
    });
  }

  // TODO: 댓글 작성 — POST /sharing-posts/{postId}/comments
  // TODO: 신고 — POST /reports
}
