import 'package:dio/dio.dart';

import '../models/note_models.dart';

/// 노트 API 호출 레이어 (주방).
///
/// 기존 `sharing_repository.dart`와 동일한 방식:
/// Dio로 서버를 호출하고, 공통 응답 봉투(`{success, data, error}`)에서
/// `data`만 꺼내(unwrap) 모델로 변환한다.
class NoteRepository {
  final Dio _dio;

  NoteRepository(this._dio);

  /// 노트 목록 조회 (GET /api/v1/notes).
  Future<NoteListResponse> getNotes({
    String? category,
    int page = 0,
  }) async {
    final response = await _dio.get('/notes', queryParameters: {
      if (category != null) 'category': category,
      'page': page,
      'size': 20,
    });
    // ✏️ 왜 이렇게 짰냐면:
    // 서버는 항상 {success, data, error} 봉투로 감싸 보내므로
    // 우리가 쓸 알맹이인 ['data']만 꺼내 모델에 넘긴다. (기존 나눔과 동일)
    final data = response.data['data'] as Map<String, dynamic>;
    return NoteListResponse.fromJson(data);
  }

  /// 노트 생성 (POST /api/v1/notes).
  ///
  /// 자유 노트(기도·회개·감사)는 qtPassageId 없이 저장한다.
  Future<NoteCreateResponse> create({
    required String category,
    required String title,
    required String body,
    String status = 'SAVED',
    String visibility = 'PRIVATE',
  }) async {
    // ✏️ 왜 이렇게 짰냐면:
    // 보낼 필드를 함수 인자로 받아 여기서 Map(JSON 바디)으로 조립한다.
    // status 기본값 SAVED = "저장" 버튼(자동저장 금지 원칙), visibility PRIVATE = 기본 비공개.
    final response = await _dio.post('/notes', data: {
      'category': category,
      'title': title,
      'body': body,
      'status': status,
      'visibility': visibility,
    });
    final data = response.data['data'] as Map<String, dynamic>;
    return NoteCreateResponse.fromJson(data);
  }
}
