import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../features/onboarding/providers/onboarding_providers.dart'
    show sharedPreferencesProvider;

/// 본문/UI 글자 크기 — 설정 화면 "폰트 크기"(SMALL/MEDIUM/LARGE)가 단일 진실이다.
///
/// 렌더링은 즉시 반영돼야 하므로 값을 기기 로컬(SharedPreferences `font_size`)에
/// 저장하고, 이 provider를 `MaterialApp.builder`에서 읽어 `MediaQuery.textScaler`로
/// 적용한다(다크 모드 [themeModeProvider]와 같은 로컬 저장 패턴).
/// 서버 설정값(settingsProvider.fontSize)과는 설정 화면에서 양방향으로 맞춘다.
final fontSizeProvider =
    StateNotifierProvider<FontSizeNotifier, String>((ref) {
  return FontSizeNotifier(ref.watch(sharedPreferencesProvider));
});

class FontSizeNotifier extends StateNotifier<String> {
  FontSizeNotifier(this._prefs) : super(_normalize(_prefs.getString(_key)));

  static const String _key = 'font_size';
  final SharedPreferences _prefs;

  /// 허용값(SMALL/MEDIUM/LARGE) 외에는 MEDIUM으로 보정한다.
  static String _normalize(String? raw) {
    switch (raw) {
      case 'SMALL':
      case 'MEDIUM':
      case 'LARGE':
        return raw!;
      default:
        return 'MEDIUM';
    }
  }

  /// 폰트 크기 변경 — 즉시 반영 후 로컬 저장. 같은 값이면 무시한다.
  Future<void> set(String value) async {
    final v = _normalize(value);
    if (v == state) return;
    state = v;
    await _prefs.setString(_key, v);
  }
}

/// 폰트 크기 코드 → 텍스트 배율. 레이아웃 깨짐을 막기 위해 보수적인 범위만 쓴다.
double fontScaleOf(String fontSize) {
  switch (fontSize) {
    case 'SMALL':
      return 0.9;
    case 'LARGE':
      return 1.2;
    case 'MEDIUM':
    default:
      return 1.0;
  }
}
