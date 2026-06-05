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
    // 왜 이렇게 짰냐면:
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
    // 왜 이렇게 짰냐면:
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

  /// 노트 상세 조회 (GET /api/v1/notes/{id}, 04 §4.3.5).
  Future<NoteDetail> getDetail(int noteId) async {
    // 왜 이렇게 짰냐면:
    // 목록(getNotes)과 똑같이 봉투에서 ['data']만 꺼내 상세 모델로 변환한다.
    // 'i 방식'(id로 다시 조회)이라 편집화면도 이 메서드를 그대로 재사용한다.
    final response = await _dio.get('/notes/$noteId');
    final data = response.data['data'] as Map<String, dynamic>;
    return NoteDetail.fromJson(data);
  }

  /// 노트 수정 (PATCH /api/v1/notes/{id}, 04 §4.3.6).
  ///
  /// N-03 편집모드에서 자유노트(title/body)를 고칠 때 사용한다.
  Future<void> update(
    int noteId, {
    required String title,
    required String body,
    String status = 'SAVED',
    String visibility = 'PRIVATE',
  }) async {
    // 왜 이렇게 짰냐면:
    // PATCH는 보낸 필드만 바꾼다(부분 수정). 자유노트 편집은 title/body/status가 전부라
    // 그것만 담아 보낸다. 응답(NoteUpdateResponse)은 화면이 어차피 목록·상세를 다시
    // 불러오므로(invalidate) 파싱하지 않고 void로 둔다 — 성공/실패만 try-catch로 판단.
    await _dio.patch('/notes/$noteId', data: {
      'title': title,
      'body': body,
      'status': status,
      'visibility': visibility,
    });
  }

  /// 묵상 달력 조회 (GET /api/v1/me/meditation-calendar, 04 §4.6.2).
  ///
  /// month는 "yyyy-MM" 형식. 생략 시 서버가 이번 달로 처리한다.
  Future<MeditationCalendar> getMeditationCalendar(String month) async {
    // ✏️ 목록과 동일하게 ['data']만 꺼내 달력 모델로 변환한다.
    final response = await _dio.get('/me/meditation-calendar',
        queryParameters: {'month': month});
    final data = response.data['data'] as Map<String, dynamic>;
    return MeditationCalendar.fromJson(data);
  }

  /// 노트 삭제 (DELETE /api/v1/notes/{id}, 04 §4.3.7).
  Future<void> delete(int noteId) async {
    // 왜 이렇게 짰냐면:
    // 서버가 204 No Content(본문 없음)로 응답하므로 ['data'] 언랩이 필요 없다.
    // 소프트 삭제(status=DELETED)는 서버가 처리 — 앱은 호출만 하고 목록/달력을 새로고침한다.
    await _dio.delete('/notes/$noteId');
  }
}
