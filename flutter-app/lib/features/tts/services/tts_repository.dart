import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show kIsWeb;

import '../../../core/config/app_config.dart';
import '../../../core/platform/file_storage.dart';

/// TTS API 호출 및 음성 파일 관리.
///
/// Voice Studio REST API(`/qt/read`)를 호출하여 QT 본문을 음성으로 변환하고,
/// 생성된 파일을 로컬에 캐시한다.
class TtsRepository {
  final Dio _dio;
  final String _ttsToken;

  /// qtai-server(인증) 호출용 dio — 서버 캐시 음성(/qt/passages/{id}/audio) 다운로드에 쓴다.
  final Dio _apiDio;

  TtsRepository({required Dio dio, required String ttsToken, required Dio apiDio})
      : _dio = dio,
        _ttsToken = ttsToken,
        _apiDio = apiDio;

  /// TTS 서버 베이스 URL.
  String get _baseUrl => AppConfig.instance.ttsBaseUrl;

  /// qtai-server 베이스 URL(/api/v1).
  String get _apiBaseUrl => AppConfig.instance.baseUrl;

  /// 서버에 미리 만들어 캐시된 '오늘 QT 본문' 음성을 받아 로컬 파일 경로를 반환한다.
  ///
  /// 서버(service-bible)가 (QT 본문, 목소리)별로 생성·DB 캐시하므로, 앱은 본문 텍스트를
  /// 만들지 않고 이 엔드포인트만 받는다. 무료 호스팅 콜드스타트/재생성을 그날 1회로 줄인다.
  /// 본문(한글 절 범위)만 읽으며 노트·해설·영상·영어는 포함하지 않는다.
  Future<String> getCachedQtPassageAudio({
    required int qtPassageId,
    String voice = '선희 (여성)',
    String format = 'mp3',
    CancelToken? cancelToken,
  }) async {
    final url = '$_apiBaseUrl/qt/passages/$qtPassageId/audio';
    final query = {'voice': voice};

    // 웹: 파일 저장 불가 → 바이트를 data URI로.
    if (kIsWeb) {
      final response = await _apiDio.get(
        url,
        queryParameters: query,
        cancelToken: cancelToken,
        options: Options(responseType: ResponseType.bytes),
      );
      final bytes = response.data as List<int>;
      return 'data:audio/$format;base64,${base64Encode(bytes)}';
    }

    final voiceHash = voice.hashCode.toRadixString(16);
    final cacheKey = 'qtaudio_${qtPassageId}_$voiceHash';
    final cached = await _getCachedFile(cacheKey, format);
    if (cached != null) return cached;

    final dirPath = await ensureCacheDir('tts_cache');
    final savePath = '$dirPath/$cacheKey.$format';
    await _apiDio.download(
      url,
      savePath,
      queryParameters: query,
      cancelToken: cancelToken,
      options: Options(responseType: ResponseType.bytes),
    );
    return savePath;
  }

  /// 사용 가능한 목소리 목록 조회.
  Future<List<TtsVoice>> getVoices() async {
    final response = await _dio.get(
      '$_baseUrl/voices',
      options: Options(headers: {'Authorization': 'Bearer $_ttsToken'}),
    );
    final List<dynamic> data = response.data;
    return data.map((e) => TtsVoice.fromJson(e)).toList();
  }

  /// QT 본문을 음성으로 변환하고 로컬 파일 경로를 반환.
  ///
  /// [cacheKey]가 있으면 캐시된 파일이 있는지 먼저 확인한다.
  Future<String> generateQtAudio({
    required String text,
    String voice = '선희 (여성)',
    double tau = 0.7,
    String format = 'mp3',
    String? cacheKey,
    CancelToken? cancelToken,
  }) async {
    // 웹(브라우저)은 파일 저장이 불가하므로, 음성을 메모리(bytes)로 받아
    // data URI로 만들어 그대로 재생한다. (TTS 서버의 CORS 허용이 필요)
    if (kIsWeb) {
      final response = await _dio.post(
        '$_baseUrl/qt/read',
        data: {
          'text': text,
          'voice': voice,
          'tau': tau,
          'format': format,
        },
        cancelToken: cancelToken,
        options: Options(
          headers: {
            'Authorization': 'Bearer $_ttsToken',
            'Content-Type': 'application/json',
          },
          responseType: ResponseType.bytes,
        ),
      );
      final bytes = response.data as List<int>;
      return 'data:audio/$format;base64,${base64Encode(bytes)}';
    }

    // 캐시 확인
    if (cacheKey != null) {
      final cached = await _getCachedFile(cacheKey, format);
      if (cached != null) return cached;
    }

    // API 호출 — 음성 파일을 직접 다운로드
    final dirPath = await ensureCacheDir('tts_cache');
    final filename = cacheKey != null
        ? '$cacheKey.$format'
        : 'qt_${DateTime.now().millisecondsSinceEpoch}.$format';
    final savePath = '$dirPath/$filename';

    await _dio.download(
      '$_baseUrl/qt/read',
      savePath,
      data: {
        'text': text,
        'voice': voice,
        'tau': tau,
        'format': format,
      },
      cancelToken: cancelToken,
      options: Options(
        method: 'POST',
        headers: {
          'Authorization': 'Bearer $_ttsToken',
          'Content-Type': 'application/json',
        },
        responseType: ResponseType.bytes,
      ),
    );

    return savePath;
  }

  /// TTS 서버 상태 확인.
  Future<bool> isServerAvailable() async {
    try {
      final response = await _dio.get(
        '$_baseUrl/',
        options: Options(
          sendTimeout: const Duration(seconds: 3),
          receiveTimeout: const Duration(seconds: 3),
        ),
      );
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  /// 캐시된 파일 경로 반환. 없으면 null. (웹은 캐시 미지원 → 항상 null)
  Future<String?> _getCachedFile(String key, String format) async {
    if (kIsWeb) return null;
    final dirPath = await ensureCacheDir('tts_cache');
    final path = '$dirPath/$key.$format';
    return await fileExists(path) ? path : null;
  }

  /// 캐시 전체 삭제. (웹은 no-op)
  Future<void> clearCache() async {
    if (kIsWeb) return;
    final dirPath = await ensureCacheDir('tts_cache');
    await deleteDirIfExists(dirPath);
  }
}

/// 목소리 정보.
class TtsVoice {
  final String name;
  final String displayName;
  final String type; // "edge", "custom"
  final bool hasRecording;
  final bool hasFinetuned;

  TtsVoice({
    required this.name,
    required this.displayName,
    required this.type,
    this.hasRecording = false,
    this.hasFinetuned = false,
  });

  factory TtsVoice.fromJson(Map<String, dynamic> json) {
    return TtsVoice(
      name: json['name'] ?? '',
      displayName: json['display_name'] ?? json['name'] ?? '',
      type: json['type'] ?? 'edge',
      hasRecording: json['has_recording'] ?? false,
      hasFinetuned: json['has_finetuned'] ?? false,
    );
  }
}
