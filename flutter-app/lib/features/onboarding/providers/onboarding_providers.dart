import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// SharedPreferences 온보딩 완료 키.
const String _kOnboardingCompleteKey = 'onboarding_complete';

/// SharedPreferences 인스턴스 Provider.
///
/// main.dart에서 앱 시작 전에 override해야 한다:
/// ```dart
/// final prefs = await SharedPreferences.getInstance();
/// runApp(ProviderScope(
///   overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
///   child: const App(),
/// ));
/// ```
final sharedPreferencesProvider = Provider<SharedPreferences>(
  (ref) => throw UnimplementedError(
    'sharedPreferencesProvider must be overridden at app startup',
  ),
);

/// 온보딩 완료 여부 Provider.
///
/// SharedPreferences에서 플래그를 읽어 반환한다.
final onboardingCompleteProvider = StateNotifierProvider<OnboardingNotifier, bool>(
  (ref) {
    final prefs = ref.watch(sharedPreferencesProvider);
    return OnboardingNotifier(prefs);
  },
);

/// 온보딩 완료 상태 관리 Notifier.
class OnboardingNotifier extends StateNotifier<bool> {
  final SharedPreferences _prefs;

  OnboardingNotifier(this._prefs)
      : super(_prefs.getBool(_kOnboardingCompleteKey) ?? false);

  /// 온보딩 완료 처리 — SharedPreferences에 플래그 저장.
  Future<void> complete() async {
    await _prefs.setBool(_kOnboardingCompleteKey, true);
    state = true;
  }

  /// 온보딩 초기화 (테스트/디버그용).
  Future<void> reset() async {
    await _prefs.remove(_kOnboardingCompleteKey);
    state = false;
  }
}
