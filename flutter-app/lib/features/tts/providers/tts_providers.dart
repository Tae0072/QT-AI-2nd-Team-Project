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

/// TTS 읽기 범위 SharedPreferences 키.
const String kTtsReadBiblePrefsKey = 'tts_read_bible';
const String kTtsReadExplanationPrefsKey = 'tts_read_explanation';

/// TTS 읽기 범위 on/off — SharedPreferences에 저장된다.
///
/// - 본문 읽기(한글): 기본 켜짐
/// - 주석(해설) 읽기: 기본 꺼짐
/// 둘 다 켜면 본문을 먼저 읽고 이어서 주석을 읽는다.
class TtsReadScopeNotifier extends StateNotifier<bool> {
  final SharedPreferences _prefs;
  final String _key;

  TtsReadScopeNotifier(this._prefs, this._key, bool defaultValue)
      : super(_prefs.getBool(_key) ?? defaultValue);

  void set(bool value) {
    state = value;
    _prefs.setBool(_key, value);
  }
}

/// 본문(한글) 읽기 여부.
final ttsReadBibleProvider =
    StateNotifierProvider<TtsReadScopeNotifier, bool>((ref) {
  return TtsReadScopeNotifier(
      ref.watch(sharedPreferencesProvider), kTtsReadBiblePrefsKey, true);
});

/// 주석(해설) 읽기 여부.
final ttsReadExplanationProvider =
    StateNotifierProvider<TtsReadScopeNotifier, bool>((ref) {
  return TtsReadScopeNotifier(
      ref.watch(sharedPreferencesProvider), kTtsReadExplanationPrefsKey, false);
});

/// 음성 생성 상태.
enum TtsState { idle, generating, playing, paused, error }

final ttsStateProvider = StateProvider<TtsState>((ref) => TtsState.idle);

/// 현재 재생 중인 파일 경로.
final currentAudioPathProvider = StateProvider<String?>((ref) => null);
