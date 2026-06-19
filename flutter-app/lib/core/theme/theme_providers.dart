import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../features/onboarding/providers/onboarding_providers.dart'
    show sharedPreferencesProvider;

/// 테마 모드 — **마이페이지 설정 화면에서** 라이트/다크/시스템 중 선택한다.
///
/// - light: 항상 라이트, dark: 항상 다크, system: 기기 설정을 따라간다.
/// - 값은 SharedPreferences(`theme_mode`)에 기기 로컬로 저장한다(서버 설정 아님).
/// - 구버전 bool(`dark_mode`) 저장값이 있으면 1회 마이그레이션한다.
/// - 저장값·구버전값이 모두 없으면(최초 실행) **라이트가 기본값**이다.
///   사용자가 직접 다크/시스템을 고른 적이 없으면 항상 라이트로 시작한다.
final themeModeProvider =
    StateNotifierProvider<ThemeModeNotifier, ThemeMode>((ref) {
  return ThemeModeNotifier(ref.watch(sharedPreferencesProvider));
});

class ThemeModeNotifier extends StateNotifier<ThemeMode> {
  ThemeModeNotifier(this._prefs) : super(_initial(_prefs));

  static const String _key = 'theme_mode';
  static const String _legacyKey = 'dark_mode'; // 구버전 on/off 토글
  final SharedPreferences _prefs;

  static ThemeMode _initial(SharedPreferences prefs) {
    final saved = prefs.getString(_key);
    if (saved != null) return _parse(saved);
    // 구버전 bool 토글 마이그레이션(true=다크, false=라이트). 없으면 라이트(앱 기본값).
    final legacy = prefs.getBool(_legacyKey);
    if (legacy != null) return legacy ? ThemeMode.dark : ThemeMode.light;
    return ThemeMode.light;
  }

  static ThemeMode _parse(String value) => switch (value) {
        'dark' => ThemeMode.dark,
        'light' => ThemeMode.light,
        'system' => ThemeMode.system,
        _ => ThemeMode.light, // 알 수 없는 값은 앱 기본값(라이트)로
      };

  bool get isDark => state == ThemeMode.dark;

  /// 테마 모드 선택 — 즉시 반영 후 로컬 저장.
  Future<void> setMode(ThemeMode mode) async {
    state = mode;
    await _prefs.setString(_key, mode.name);
  }
}
