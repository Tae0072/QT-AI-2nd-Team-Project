import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/l10n/app_localizations.dart';

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
          child: MaterialApp(localizationsDelegates: AppLocalizations.localizationsDelegates, supportedLocales: AppLocalizations.supportedLocales, locale: Locale('ko'), home: LoginScreen()),
        ),
      );
      await tester.pump();

      expect(find.text('카카오로 시작하기'), findsOneWidget);
    });

    testWidgets('헤드라인이 표시된다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(localizationsDelegates: AppLocalizations.localizationsDelegates, supportedLocales: AppLocalizations.supportedLocales, locale: Locale('ko'), home: LoginScreen()),
        ),
      );
      await tester.pump();

      expect(find.textContaining('매일의 묵상을'), findsOneWidget);
    });

    testWidgets('카카오 로그인 버튼은 ElevatedButton이다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(localizationsDelegates: AppLocalizations.localizationsDelegates, supportedLocales: AppLocalizations.supportedLocales, locale: Locale('ko'), home: LoginScreen()),
        ),
      );
      await tester.pump();

      expect(find.byType(ElevatedButton), findsOneWidget);
    });

    testWidgets('초기 상태에서 에러 메시지가 없다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(localizationsDelegates: AppLocalizations.localizationsDelegates, supportedLocales: AppLocalizations.supportedLocales, locale: Locale('ko'), home: LoginScreen()),
        ),
      );
      await tester.pump();

      expect(find.text('로그인에 실패했습니다. 다시 시도해주세요.'), findsNothing);
    });
  });
}
