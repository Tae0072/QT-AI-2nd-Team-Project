import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../onboarding/providers/onboarding_providers.dart'
    show sharedPreferencesProvider;
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

/// SharedPreferences 저장 키.
const String kTtsVoicePrefsKey = 'tts_selected_voice';

/// 기본 목소리.
const String kDefaultTtsVoice = '선희 (여성)';

/// 현재 선택된 목소리 — SharedPreferences에 저장되어 앱 재시작 후에도 유지된다.
///
/// 마이페이지 > 설정에서 변경하며, 변경 즉시 QT 화면의 플레이어가
/// 새 목소리로 음성을 다시 준비한다.
class SelectedVoiceNotifier extends StateNotifier<String> {
  final SharedPreferences _prefs;

  SelectedVoiceNotifier(this._prefs)
      : super(_prefs.getString(kTtsVoicePrefsKey) ?? kDefaultTtsVoice);

  /// 목소리 선택 + 영구 저장.
  void select(String voice) {
    state = voice;
    _prefs.setString(kTtsVoicePrefsKey, voice);
  }
}

final selectedVoiceProvider =
    StateNotifierProvider<SelectedVoiceNotifier, String>((ref) {
  return SelectedVoiceNotifier(ref.watch(sharedPreferencesProvider));
});

/// 음성 생성 상태.
enum TtsState { idle, generating, playing, paused, error }

final ttsStateProvider = StateProvider<TtsState>((ref) => TtsState.idle);

/// 현재 재생 중인 파일 경로.
final currentAudioPathProvider = StateProvider<String?>((ref) => null);
