import 'package:dio/dio.dart';

class NoteRepository {
  final Dio _dio;

  NoteRepository(this._dio);

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
}
