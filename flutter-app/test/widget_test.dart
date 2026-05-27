import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qtai_app/core/config/app_config.dart';
import 'package:qtai_app/main.dart';

void main() {
  setUp(() {
    AppConfig.reset();
    AppConfig.initializeForTest();
  });

  tearDown(() {
    AppConfig.reset();
  });

  testWidgets('QTAIApp이 정상적으로 렌더링된다', (WidgetTester tester) async {
    await tester.pumpWidget(const ProviderScope(child: QTAIApp()));
    await tester.pumpAndSettle();

    // MaterialApp이 렌더링되었는지 확인
    expect(find.byType(MaterialApp), findsOneWidget);
  });

  testWidgets('QTAIApp 타이틀이 QT AI이다', (WidgetTester tester) async {
    await tester.pumpWidget(const ProviderScope(child: QTAIApp()));
    await tester.pumpAndSettle();

    final MaterialApp app = tester.widget(find.byType(MaterialApp));
    expect(app.title, 'QT AI');
  });
}
