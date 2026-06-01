import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart';

void main() {
  group('OnboardingNotifier', () {
    late SharedPreferences prefs;

    setUp(() {
      SharedPreferences.setMockInitialValues({});
    });

    test('초기값 — onboarding_complete 키 없으면 false', () async {
      prefs = await SharedPreferences.getInstance();
      final notifier = OnboardingNotifier(prefs);

      expect(notifier.state, isFalse);
    });

    test('초기값 — onboarding_complete=true면 true', () async {
      SharedPreferences.setMockInitialValues({'onboarding_complete': true});
      prefs = await SharedPreferences.getInstance();
      final notifier = OnboardingNotifier(prefs);

      expect(notifier.state, isTrue);
    });

    test('complete() — state가 true로 바뀌고 SharedPreferences에 저장된다',
        () async {
      prefs = await SharedPreferences.getInstance();
      final notifier = OnboardingNotifier(prefs);

      expect(notifier.state, isFalse);

      await notifier.complete();

      expect(notifier.state, isTrue);
      expect(prefs.getBool('onboarding_complete'), isTrue);
    });

    test('reset() — state가 false로 바뀌고 SharedPreferences에서 제거된다',
        () async {
      SharedPreferences.setMockInitialValues({'onboarding_complete': true});
      prefs = await SharedPreferences.getInstance();
      final notifier = OnboardingNotifier(prefs);

      expect(notifier.state, isTrue);

      await notifier.reset();

      expect(notifier.state, isFalse);
      expect(prefs.getBool('onboarding_complete'), isNull);
    });

    test('complete() 후 reset() — 왕복 동작이 정상이다', () async {
      prefs = await SharedPreferences.getInstance();
      final notifier = OnboardingNotifier(prefs);

      await notifier.complete();
      expect(notifier.state, isTrue);

      await notifier.reset();
      expect(notifier.state, isFalse);
      expect(prefs.getBool('onboarding_complete'), isNull);
    });
  });

  group('onboardingCompleteProvider (Riverpod 통합)', () {
    test('Provider가 SharedPreferences 값을 읽는다', () async {
      SharedPreferences.setMockInitialValues({'onboarding_complete': true});
      final prefs = await SharedPreferences.getInstance();

      final container = ProviderContainer(
        overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
      );
      addTearDown(container.dispose);

      expect(container.read(onboardingCompleteProvider), isTrue);
    });

    test('Provider — 초기값 false일 때 complete() 호출하면 true', () async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();

      final container = ProviderContainer(
        overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
      );
      addTearDown(container.dispose);

      expect(container.read(onboardingCompleteProvider), isFalse);

      await container
          .read(onboardingCompleteProvider.notifier)
          .complete();

      expect(container.read(onboardingCompleteProvider), isTrue);
    });
  });
}
