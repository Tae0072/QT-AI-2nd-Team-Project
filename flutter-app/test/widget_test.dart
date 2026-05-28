import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:qtai_app/core/config/app_config.dart';
import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart';
import 'package:qtai_app/main.dart';

void main() {
  setUp(() {
    AppConfig.reset();
    AppConfig.initializeForTest();
    SharedPreferences.setMockInitialValues({});
  });

  tearDown(() {
    AppConfig.reset();
  });

  testWidgets('QTAIApp이 정상적으로 렌더링된다', (WidgetTester tester) async {
    final prefs = await SharedPreferences.getInstance();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
        child: const QTAIApp(),
      ),
    );
    await tester.pumpAndSettle();

    // MaterialApp이 렌더링되었는지 확인
    expect(find.byType(MaterialApp), findsOneWidget);
  });

  testWidgets('QTAIApp 타이틀이 QT AI이다', (WidgetTester tester) async {
    final prefs = await SharedPreferences.getInstance();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
        child: const QTAIApp(),
      ),
    );
    await tester.pumpAndSettle();

    final MaterialApp app = tester.widget(find.byType(MaterialApp));
    expect(app.title, 'QT AI');
  });

  testWidgets('온보딩 미완료 시 initialRoute가 /onboarding이다',
      (WidgetTester tester) async {
    // onboarding_complete 키 없음 → false
    final prefs = await SharedPreferences.getInstance();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
        child: const QTAIApp(),
      ),
    );
    await tester.pumpAndSettle();

    final MaterialApp app = tester.widget(find.byType(MaterialApp));
    expect(app.initialRoute, equals('/onboarding'));
  });

  testWidgets('온보딩 완료 시 initialRoute가 /login이다',
      (WidgetTester tester) async {
    SharedPreferences.setMockInitialValues({'onboarding_complete': true});
    final prefs = await SharedPreferences.getInstance();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
        child: const QTAIApp(),
      ),
    );
    await tester.pumpAndSettle();

    final MaterialApp app = tester.widget(find.byType(MaterialApp));
    expect(app.initialRoute, equals('/login'));
  });
}
