import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/core/theme/theme_providers.dart';
import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// 테마 모드(라이트/다크/시스템) 상태 테스트 — 설정 화면 선택이 단일 진실이고
/// 기기 로컬에 저장되며, 구버전 bool 설정이 마이그레이션됨을 고정한다.
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

  test('기본값은 라이트다(저장값·구버전값 없음·최초 실행)', () async {
    SharedPreferences.setMockInitialValues({});
    final c = await container();

    expect(c.read(themeModeProvider), ThemeMode.light);
  });

  test('모드를 고르면 바뀌고 theme_mode에 저장된다', () async {
    SharedPreferences.setMockInitialValues({});
    final c = await container();

    await c.read(themeModeProvider.notifier).setMode(ThemeMode.dark);
    expect(c.read(themeModeProvider), ThemeMode.dark);
    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getString('theme_mode'), 'dark');

    await c.read(themeModeProvider.notifier).setMode(ThemeMode.system);
    expect(c.read(themeModeProvider), ThemeMode.system);
    expect((await SharedPreferences.getInstance()).getString('theme_mode'),
        'system');
  });

  test('저장된 theme_mode는 재시작(새 컨테이너) 후에도 복원된다', () async {
    SharedPreferences.setMockInitialValues({'theme_mode': 'light'});
    final c = await container();

    expect(c.read(themeModeProvider), ThemeMode.light);
  });

  test('구버전 dark_mode=true는 다크로 마이그레이션된다', () async {
    SharedPreferences.setMockInitialValues({'dark_mode': true});
    final c = await container();

    expect(c.read(themeModeProvider), ThemeMode.dark);
  });

  test('구버전 dark_mode=false는 라이트로 마이그레이션된다', () async {
    SharedPreferences.setMockInitialValues({'dark_mode': false});
    final c = await container();

    expect(c.read(themeModeProvider), ThemeMode.light);
  });

  test('직접 고른 theme_mode=system은 재시작 후에도 시스템으로 복원된다', () async {
    SharedPreferences.setMockInitialValues({'theme_mode': 'system'});
    final c = await container();

    expect(c.read(themeModeProvider), ThemeMode.system);
  });

  test('알 수 없는 theme_mode 값은 라이트(앱 기본값)로 처리된다', () async {
    SharedPreferences.setMockInitialValues({'theme_mode': 'oops'});
    final c = await container();

    expect(c.read(themeModeProvider), ThemeMode.light);
  });
}
