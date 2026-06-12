import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:qtai_app/core/theme/font_scale_provider.dart';
import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart'
    show sharedPreferencesProvider;

Future<ProviderContainer> _container() async {
  SharedPreferences.setMockInitialValues({});
  final prefs = await SharedPreferences.getInstance();
  return ProviderContainer(
    overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
  );
}

void main() {
  group('fontScaleOf', () {
    test('코드별 배율 매핑', () {
      expect(fontScaleOf('SMALL'), 0.9);
      expect(fontScaleOf('MEDIUM'), 1.0);
      expect(fontScaleOf('LARGE'), 1.2);
    });

    test('알 수 없는 값은 MEDIUM(1.0)으로 처리', () {
      expect(fontScaleOf('HUGE'), 1.0);
      expect(fontScaleOf(''), 1.0);
    });
  });

  group('FontSizeNotifier', () {
    test('초기값은 MEDIUM(저장된 값 없음)', () async {
      final c = await _container();
      addTearDown(c.dispose);
      expect(c.read(fontSizeProvider), 'MEDIUM');
    });

    test('set으로 변경하면 상태가 갱신된다', () async {
      final c = await _container();
      addTearDown(c.dispose);

      await c.read(fontSizeProvider.notifier).set('LARGE');
      expect(c.read(fontSizeProvider), 'LARGE');
    });

    test('허용되지 않은 값은 MEDIUM으로 보정', () async {
      final c = await _container();
      addTearDown(c.dispose);

      await c.read(fontSizeProvider.notifier).set('WRONG');
      expect(c.read(fontSizeProvider), 'MEDIUM');
    });

    test('저장된 값이 있으면 그 값으로 초기화', () async {
      SharedPreferences.setMockInitialValues({'font_size': 'SMALL'});
      final prefs = await SharedPreferences.getInstance();
      final c = ProviderContainer(
        overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
      );
      addTearDown(c.dispose);

      expect(c.read(fontSizeProvider), 'SMALL');
    });
  });
}
