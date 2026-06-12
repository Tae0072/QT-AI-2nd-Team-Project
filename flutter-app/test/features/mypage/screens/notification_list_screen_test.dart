import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/mypage/models/notification_response.dart';
import 'package:qtai_app/features/mypage/providers/mypage_providers.dart';
import 'package:qtai_app/features/mypage/screens/notification_list_screen.dart';
import 'package:qtai_app/l10n/app_localizations.dart';

void main() {
  Widget buildScreen(
    NotificationListResponse response, {
    DateTime Function()? now,
  }) {
    return ProviderScope(
      overrides: [
        notificationsProvider.overrideWith((ref) async => response),
      ],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: Locale('ko'),
        home: NotificationListScreen(now: now),
      ),
    );
  }

  testWidgets('알림 상대 시간은 화면을 열어둔 상태에서도 갱신된다', (tester) async {
    var now = DateTime(2026, 6, 12, 10);
    final createdAt = now.subtract(const Duration(seconds: 50));
    final response = NotificationListResponse(
      items: [
        NotificationItem(
          id: 1,
          type: 'NOTICE',
          title: '공지 알림',
          body: '본문',
          read: false,
          createdAt: createdAt,
        ),
      ],
      totalElements: 1,
      hasNext: false,
    );

    await tester.pumpWidget(buildScreen(response, now: () => now));
    await tester.pump();

    expect(find.text('방금 전 · 09:59'), findsOneWidget);
    expect(find.text('1분 전 · 09:59'), findsNothing);

    await tester.pump(const Duration(seconds: 29));
    now = now.add(const Duration(seconds: 31));
    await tester.pump(const Duration(seconds: 2));

    expect(find.text('방금 전 · 09:59'), findsNothing);
    expect(find.text('1분 전 · 09:59'), findsOneWidget);
  });

  testWidgets('미래 timestamp는 방금 전으로 숨기지 않고 절대 시각을 표시한다', (tester) async {
    final response = NotificationListResponse(
      items: [
        NotificationItem(
          id: 1,
          type: 'NOTICE',
          title: '공지 알림',
          body: '본문',
          read: false,
          createdAt: DateTime(2026, 6, 13, 4, 52),
        ),
      ],
      totalElements: 1,
      hasNext: false,
    );

    await tester.pumpWidget(
      buildScreen(response, now: () => DateTime(2026, 6, 12, 10, 54)),
    );
    await tester.pump();

    expect(find.text('방금 전 · 04:52'), findsNothing);
    expect(find.text('6/13 04:52'), findsOneWidget);
  });
}
