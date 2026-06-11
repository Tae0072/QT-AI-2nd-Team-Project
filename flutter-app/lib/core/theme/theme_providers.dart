import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../features/onboarding/providers/onboarding_providers.dart'
    show sharedPreferencesProvider;

/// 다크 모드 상태 — **마이페이지 설정 화면에서만** 변경한다(2026-06-11 T 결정).
///
/// 시스템 다크 모드 설정은 추종하지 않는다(`ThemeMode.system` 미사용) —
/// 기기 설정과 무관하게 앱 안의 토글이 단일 진실이다.
/// 값은 SharedPreferences(`dark_mode`)에 기기 로컬로 저장한다(서버 설정 아님).
final themeModeProvider =
    StateNotifierProvider<ThemeModeNotifier, ThemeMode>((ref) {
  return ThemeModeNotifier(ref.watch(sharedPreferencesProvider));
});

class ThemeModeNotifier extends StateNotifier<ThemeMode> {
  ThemeModeNotifier(this._prefs)
      : super((_prefs.getBool(_key) ?? false) ? ThemeMode.dark : ThemeMode.light);

  static const String _key = 'dark_mode';
  final SharedPreferences _prefs;

  bool get isDark => state == ThemeMode.dark;

  /// 다크 모드 켜기/끄기 — 즉시 반영 후 로컬 저장.
  Future<void> setDark(bool value) async {
    state = value ? ThemeMode.dark : ThemeMode.light;
    await _prefs.setBool(_key, value);
  }
}
