import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/mypage/screens/tts_settings_screen.dart';
import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart';
import 'package:qtai_app/features/tts/providers/tts_providers.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  Future<SharedPreferences> mockPrefs() async {
    SharedPreferences.setMockInitialValues({});
    return SharedPreferences.getInstance();
  }

  Widget buildScreen(SharedPreferences prefs) {
    return ProviderScope(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
      child: MaterialApp(localizationsDelegates: AppLocalizations.localizationsDelegates, supportedLocales: AppLocalizations.supportedLocales, locale: const Locale('ko'), home: const TtsSettingsScreen()),
    );
  }

  testWidgets('TTS 읽기 설정 화면은 목소리/본문/해설 항목을 표시한다', (tester) async {
    final prefs = await mockPrefs();
    await tester.pumpWidget(buildScreen(prefs));

    expect(find.text('TTS 읽기 설정'), findsOneWidget);
    expect(find.text('읽기 목소리'), findsOneWidget);
    expect(find.text('본문 읽기 (한글)'), findsOneWidget);
    expect(find.text('해설 읽기'), findsOneWidget);
    // 기본값: 본문 켜짐 + 해설 꺼짐, 기본 목소리 표시
    expect(find.text(kDefaultTtsVoice), findsOneWidget);
  });

  testWidgets('해설 읽기 토글을 켜면 SharedPreferences에 저장된다', (tester) async {
    final prefs = await mockPrefs();
    await tester.pumpWidget(buildScreen(prefs));

    final explanationSwitch = find.widgetWithText(SwitchListTile, '해설 읽기');
    await tester.tap(explanationSwitch);
    await tester.pump();

    expect(prefs.getBool(kTtsReadExplanationPrefsKey), isTrue);
  });

  testWidgets('본문과 해설을 둘 다 끄려고 하면 차단하고 안내한다', (tester) async {
    final prefs = await mockPrefs();
    await tester.pumpWidget(buildScreen(prefs));

    // 기본 상태: 본문 켜짐, 해설 꺼짐 → 본문을 끄면 둘 다 꺼지므로 차단
    final bibleSwitch = find.widgetWithText(SwitchListTile, '본문 읽기 (한글)');
    await tester.tap(bibleSwitch);
    await tester.pump();

    expect(find.text('본문과 해설 중 최소 한 가지는 켜져 있어야 합니다'), findsOneWidget);
    // 값은 바뀌지 않아야 한다 (저장 안 됨 → 기본값 유지)
    expect(prefs.getBool(kTtsReadBiblePrefsKey), isNot(false));
  });
}
