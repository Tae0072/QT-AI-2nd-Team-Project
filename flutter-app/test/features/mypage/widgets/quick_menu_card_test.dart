import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/mypage/widgets/quick_menu_card.dart';

void main() {
  group('QuickMenuCard', () {
    testWidgets('알림·찬양·설정 메뉴 항목 표시', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: Scaffold(
            body: QuickMenuCard(),
          ),
        ),
      );

      expect(find.text('알림'), findsOneWidget);
      expect(find.text('나의 찬양'), findsOneWidget);
      expect(find.text('설정'), findsOneWidget);
    });

    testWidgets('알림 뱃지 표시 (카운트 > 0)', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: Scaffold(
            body: QuickMenuCard(unreadNotificationCount: 5),
          ),
        ),
      );

      expect(find.text('5'), findsOneWidget);
    });

    testWidgets('알림 뱃지 미표시 (카운트 0)', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: Scaffold(
            body: QuickMenuCard(unreadNotificationCount: 0),
          ),
        ),
      );

      // 뱃지 컨테이너가 없어야 함
      expect(find.text('0'), findsNothing);
    });

    testWidgets('99 초과 시 99+ 표시', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: Scaffold(
            body: QuickMenuCard(unreadNotificationCount: 150),
          ),
        ),
      );

      expect(find.text('99+'), findsOneWidget);
    });

    testWidgets('찬양 곡 수 표시', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: Scaffold(
            body: QuickMenuCard(savedSongCount: 12),
          ),
        ),
      );

      expect(find.text('12곡'), findsOneWidget);
    });
  });
}
