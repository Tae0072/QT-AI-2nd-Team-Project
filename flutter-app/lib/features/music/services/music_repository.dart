import 'package:dio/dio.dart';

import '../models/music_track.dart';

/// 배경음악 API 호출 레이어 (qtai-server `/music/**`).
///
/// 인증 Dio(dioProvider)를 주입받아 호출한다 — Authorization 헤더는 AuthInterceptor가 첨부.
class MusicRepository {
  final Dio _dio;

  MusicRepository(this._dio);

  /// 활성 음원 목록 조회. GET /music/tracks
  Future<List<MusicTrack>> getTracks() async {
    final response = await _dio.get('/music/tracks');
    final data = response.data['data'] as List<dynamic>;
    return data
        .map((e) => MusicTrack.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
