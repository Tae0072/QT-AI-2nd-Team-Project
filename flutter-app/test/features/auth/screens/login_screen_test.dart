import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/core/config/app_config.dart';
import 'package:qtai_app/features/auth/screens/login_screen.dart';

void main() {
  setUp(() {
    AppConfig.reset();
    AppConfig.initializeForTest();
  });

  tearDown(() {
    AppConfig.reset();
  });

  group('LoginScreen', () {
    testWidgets('카카오 로그인 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: LoginScreen()),
        ),
      );
      await tester.pump();

      expect(find.text('카카오 로그인'), findsOneWidget);
    });

    testWidgets('앱 타이틀 QT AI가 표시된다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: LoginScreen()),
        ),
      );
      await tester.pump();

      expect(find.text('QT AI'), findsOneWidget);
    });

    testWidgets('앱 설명 텍스트가 표시된다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: LoginScreen()),
        ),
      );
      await tester.pump();

      expect(find.text('매일의 큐티를 더 깊이있게'), findsOneWidget);
    });

    testWidgets('카카오 로그인 버튼은 ElevatedButton이다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: LoginScreen()),
        ),
      );
      await tester.pump();

      expect(find.byType(ElevatedButton), findsOneWidget);
    });

    testWidgets('초기 상태에서 에러 메시지가 없다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: LoginScreen()),
        ),
      );
      await tester.pump();

      expect(find.text('로그인에 실패했습니다. 다시 시도해주세요.'), findsNothing);
    });

    testWidgets('초기 상태에서 로딩 인디케이터가 없다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: LoginScreen()),
        ),
      );
      await tester.pump();

      expect(find.byType(CircularProgressIndicator), findsNothing);
    });
  });
}
