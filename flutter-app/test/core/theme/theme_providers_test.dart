import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/core/theme/theme_providers.dart';
import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// 다크 모드 토글 상태 테스트 — 설정 화면 토글이 단일 진실(시스템 비추종)임을 고정한다.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<ProviderContainer> container() async {
    final prefs = await SharedPreferences.getInstance();
    final c = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(c.dispose);
    return c;
  }

  test('기본값은 라이트 모드다(저장값 없음)', () async {
    SharedPreferences.setMockInitialValues({});
    final c = await container();

    expect(c.read(themeModeProvider), ThemeMode.light);
  });

  test('토글 켜면 다크로 바뀌고 로컬에 저장된다', () async {
    SharedPreferences.setMockInitialValues({});
    final c = await container();

    await c.read(themeModeProvider.notifier).setDark(true);

    expect(c.read(themeModeProvider), ThemeMode.dark);
    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getBool('dark_mode'), isTrue);
  });

  test('저장된 다크 설정은 재시작(새 컨테이너) 후에도 복원된다', () async {
    SharedPreferences.setMockInitialValues({'dark_mode': true});
    final c = await container();

    expect(c.read(themeModeProvider), ThemeMode.dark);

    await c.read(themeModeProvider.notifier).setDark(false);
    expect(c.read(themeModeProvider), ThemeMode.light);
  });
}
