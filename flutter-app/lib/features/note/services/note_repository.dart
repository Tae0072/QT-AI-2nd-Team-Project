import 'package:dio/dio.dart';

import '../models/note_models.dart';

/// 노트 API 호출 레이어.
///
/// 기존 `sharing_repository.dart`와 동일한 방식:
/// Dio로 서버를 호출하고, 공통 응답 봉투(`{success, data, error}`)에서
/// `data`만 꺼내 모델로 변환한다.
class NoteRepository {
  final Dio _dio;

  NoteRepository(this._dio);

  /// QT 묵상 노트 생성 (POST /api/v1/notes).
  Future<void> createQtNote({
    required int? qtPassageId,
    required String title,
    required String body,
    required List<int> verseIds,
    String status = 'SAVED',
  }) async {
    await _dio.post('/notes', data: {
      'category': 'MEDITATION',
      'qtPassageId': qtPassageId,
      'title': title,
      'body': body,
      'verseIds': verseIds,
      'status': status,
      'visibility': 'PRIVATE',
    });
  }

  /// 노트 목록 조회 (GET /api/v1/notes).
  ///
  /// [status]는 서버 `notes.status`(DRAFT/SAVED) 필터. null이면 전체(04 §4.3.1).
  Future<NoteListResponse> getNotes({
    String? category,
    String? status,
    int page = 0,
  }) async {
    final response = await _dio.get('/notes', queryParameters: {
      if (category != null) 'category': category,
      if (status != null) 'status': status,
      'page': page,
      'size': 20,
    });
    final data = response.data['data'] as Map<String, dynamic>;
    return NoteListResponse.fromJson(data);
  }

  /// 노트 생성 (POST /api/v1/notes).
  ///
  /// 자유 노트(기도, 회개, 감사)는 qtPassageId 없이 저장한다.
  /// [verseIds]는 본문에 인용한 절(설교 노트·@멘션) — `note_verses` 메타데이터로 저장된다
  /// (04 §4.3.4·§6.4.1). null이면 키를 생략(인용 절 없음), 빈 배열이면 빈 목록으로 전송한다.
  Future<NoteCreateResponse> create({
    required String category,
    required String title,
    required String body,
    List<int>? verseIds,
    String status = 'SAVED',
    String visibility = 'PRIVATE',
  }) async {
    final response = await _dio.post('/notes', data: {
      'category': category,
      'title': title,
      'body': body,
      if (verseIds != null) 'verseIds': verseIds,
      'status': status,
      'visibility': visibility,
    });
    final data = response.data['data'] as Map<String, dynamic>;
    return NoteCreateResponse.fromJson(data);
  }

  /// 노트 상세 조회 (GET /api/v1/notes/{id}).
  Future<NoteDetail> getDetail(int noteId) async {
    final response = await _dio.get('/notes/$noteId');
    final data = response.data['data'] as Map<String, dynamic>;
    return NoteDetail.fromJson(data);
  }

  /// 노트 수정 (PATCH /api/v1/notes/{id}).
  ///
  /// ⚠️ [verseIds] 시맨틱(04 §4.3.6):
  /// - `null`(기본): 키를 **생략** → 서버가 기존 `note_verses`를 **건드리지 않음**(안전).
  /// - `[]`(빈 배열): 인용 절을 **전부 비움**(전체 교체).
  /// - 값 있음: 그 배열로 **교체**.
  ///
  /// 따라서 인용 절이 있는 노트(설교 등)를 수정할 때 절을 보존하려면 호출부가 현재 절
  /// 목록을 명시적으로 다시 넘겨야 하고, 절을 건드리지 않으려면 아예 넘기지 말아야 한다.
  /// 기본을 null로 둬서 "verseIds 안 줬는데 전체 삭제되는" 함정을 막는다.
  Future<void> update(
    int noteId, {
    required String title,
    required String body,
    List<int>? verseIds,
    String status = 'SAVED',
    String visibility = 'PRIVATE',
  }) async {
    await _dio.patch('/notes/$noteId', data: {
      'title': title,
      'body': body,
      if (verseIds != null) 'verseIds': verseIds,
      'status': status,
      'visibility': visibility,
    });
  }

  /// 묵상 달력 조회 (GET /api/v1/me/meditation-calendar).
  ///
  /// month는 "yyyy-MM" 형식. 생략 시 서버가 이번 달로 처리한다.
  Future<MeditationCalendar> getMeditationCalendar(String month) async {
    final response = await _dio
        .get('/me/meditation-calendar', queryParameters: {'month': month});
    final data = response.data['data'] as Map<String, dynamic>;
    return MeditationCalendar.fromJson(data);
  }

  /// 노트 삭제 (DELETE /api/v1/notes/{id}).
  Future<void> delete(int noteId) async {
    await _dio.delete('/notes/$noteId');
  }
}
