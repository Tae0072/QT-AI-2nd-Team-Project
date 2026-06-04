import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/tts_repository.dart';

/// TTS API 토큰. 앱 설정 또는 환경 변수에서 주입.
/// `--dart-define=TTS_TOKEN=vs_xxxxx` 로 설정.
final ttsTokenProvider = Provider<String>((ref) {
  const token = String.fromEnvironment('TTS_TOKEN', defaultValue: '');
  return token;
});

/// TTS 전용 Dio 인스턴스 (qtai-server와 별도).
final ttsDioProvider = Provider<Dio>((ref) {
  return Dio(BaseOptions(
    connectTimeout: const Duration(seconds: 30),
    receiveTimeout: const Duration(seconds: 120), // 긴 음성 생성 대기
    headers: {'Content-Type': 'application/json'},
  ));
});

/// TTS Repository.
final ttsRepositoryProvider = Provider<TtsRepository>((ref) {
  return TtsRepository(
    dio: ref.watch(ttsDioProvider),
    ttsToken: ref.watch(ttsTokenProvider),
  );
});

/// TTS 서버 연결 상태.
final ttsServerStatusProvider = FutureProvider<bool>((ref) async {
  final repo = ref.watch(ttsRepositoryProvider);
  return repo.isServerAvailable();
});

/// 사용 가능한 목소리 목록.
final ttsVoicesProvider = FutureProvider<List<TtsVoice>>((ref) async {
  final repo = ref.watch(ttsRepositoryProvider);
  final token = ref.watch(ttsTokenProvider);
  if (token.isEmpty) return [];
  try {
    return await repo.getVoices();
  } catch (_) {
    return [];
  }
});

/// 현재 선택된 목소리.
final selectedVoiceProvider = StateProvider<String>((ref) => '선희 (여성)');

/// 음성 생성 상태.
enum TtsState { idle, generating, playing, paused, error }

final ttsStateProvider = StateProvider<TtsState>((ref) => TtsState.idle);

/// 현재 재생 중인 파일 경로.
final currentAudioPathProvider = StateProvider<String?>((ref) => null);
