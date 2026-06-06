import 'dart:io';
import 'package:dio/dio.dart';
import 'package:path_provider/path_provider.dart';
import '../../../core/config/app_config.dart';

/// TTS API 호출 및 음성 파일 관리.
///
/// Voice Studio REST API(`/qt/read`)를 호출하여 QT 본문을 음성으로 변환하고,
/// 생성된 파일을 로컬에 캐시한다.
class TtsRepository {
  final Dio _dio;
  final String _ttsToken;

  TtsRepository({required Dio dio, required String ttsToken})
      : _dio = dio,
        _ttsToken = ttsToken;

  /// TTS 서버 베이스 URL.
  String get _baseUrl => AppConfig.instance.ttsBaseUrl;

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
  }) async {
    // 캐시 확인
    if (cacheKey != null) {
      final cached = await _getCachedFile(cacheKey, format);
      if (cached != null) return cached;
    }

    // API 호출 — 음성 파일을 직접 다운로드
    final dir = await _cacheDir();
    final filename = cacheKey != null
        ? '$cacheKey.$format'
        : 'qt_${DateTime.now().millisecondsSinceEpoch}.$format';
    final savePath = '${dir.path}/$filename';

    await _dio.download(
      '$_baseUrl/qt/read',
      savePath,
      data: {
        'text': text,
        'voice': voice,
        'tau': tau,
        'format': format,
      },
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

  /// 캐시 디렉토리.
  Future<Directory> _cacheDir() async {
    final appDir = await getApplicationDocumentsDirectory();
    final dir = Directory('${appDir.path}/tts_cache');
    if (!await dir.exists()) {
      await dir.create(recursive: true);
    }
    return dir;
  }

  /// 캐시된 파일 경로 반환. 없으면 null.
  Future<String?> _getCachedFile(String key, String format) async {
    final dir = await _cacheDir();
    final file = File('${dir.path}/$key.$format');
    if (await file.exists()) {
      return file.path;
    }
    return null;
  }

  /// 캐시 전체 삭제.
  Future<void> clearCache() async {
    final dir = await _cacheDir();
    if (await dir.exists()) {
      await dir.delete(recursive: true);
    }
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
