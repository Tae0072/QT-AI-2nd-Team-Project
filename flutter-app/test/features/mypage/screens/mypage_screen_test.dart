import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/mypage/models/dashboard_response.dart';
import 'package:qtai_app/features/mypage/providers/mypage_providers.dart';
import 'package:qtai_app/features/mypage/screens/mypage_screen.dart';
import 'package:qtai_app/l10n/app_localizations.dart';

/// 마이페이지: 미션 블록 분리 + '나의 찬양' 제거 검증.
///
/// - 연속/이번 주/이번 달 묵상 일수를 닉네임 부제가 아니라 별도 '미션' 블록으로 노출한다.
/// - '나의 찬양'(F-09 사용자 노출)은 제거되어 더 이상 표시되지 않는다.
void main() {
  Widget buildScreen(DashboardResponse response) {
    return ProviderScope(
      overrides: [
        dashboardProvider.overrideWith((ref) async => response),
      ],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: const Locale('ko'),
        home: const MyPageScreen(),
      ),
    );
  }

  DashboardResponse dashboard() => const DashboardResponse(
        profile: ProfileSummary(memberId: 1, nickname: '테스트닉'),
        stats: StatsWidget(
          week: WeekMonth(meditationDays: 3),
          month: WeekMonth(meditationDays: 12),
          meditationStreakDays: 5,
        ),
        unreadNotificationCount: 0,
      );

  testWidgets('미션 블록이 연속/이번 주/이번 달을 별도 블록으로 표시한다', (tester) async {
    await tester.pumpWidget(buildScreen(dashboard()));
    await tester.pumpAndSettle();

    // 미션 블록 제목 + 3개 지표 라벨
    expect(find.text('미션'), findsOneWidget);
    expect(find.text('연속'), findsOneWidget);
    expect(find.text('이번 주'), findsOneWidget);
    expect(find.text('이번 달'), findsOneWidget);

    // 지표 값(연속 5일 / 이번 주 3일 / 이번 달 12일)
    expect(find.text('5일'), findsOneWidget);
    expect(find.text('3일'), findsOneWidget);
    expect(find.text('12일'), findsOneWidget);

    // 닉네임은 그대로 노출
    expect(find.text('테스트닉'), findsOneWidget);
  });

  testWidgets("'나의 찬양' 항목이 더 이상 노출되지 않는다", (tester) async {
    await tester.pumpWidget(buildScreen(dashboard()));
    await tester.pumpAndSettle();

    expect(find.text('나의 찬양'), findsNothing);
    // 알림 행은 유지된다(찬양만 제거).
    expect(find.text('알림'), findsOneWidget);
  });
}
