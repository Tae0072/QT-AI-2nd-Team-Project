import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/onboarding/models/onboarding_page_data.dart';
import 'package:qtai_app/features/onboarding/screens/onboarding_screen.dart';

void main() {
  final testPages = [
    const OnboardingPageData(
      title: '페이지 1',
      description: '설명 1',
      icon: Icons.star,
      backgroundColor: Color(0xFF1A237E),
      iconColor: Color(0xFFB388FF),
    ),
    const OnboardingPageData(
      title: '페이지 2',
      description: '설명 2',
      icon: Icons.favorite,
      backgroundColor: Color(0xFF283593),
      iconColor: Color(0xFFEA80FC),
    ),
    const OnboardingPageData(
      title: '마지막 페이지',
      description: '마지막 설명',
      icon: Icons.rocket_launch,
      backgroundColor: Color(0xFF3949AB),
      iconColor: Color(0xFFFFD180),
    ),
  ];

  Widget buildWidget({VoidCallback? onComplete}) {
    return MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      locale: const Locale('ko'),
      home: OnboardingScreen(
        onComplete: onComplete ?? () {},
        pages: testPages,
      ),
    );
  }

  group('OnboardingScreen', () {
    testWidgets('첫 페이지가 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      expect(find.text('페이지 1'), findsOneWidget);
      expect(find.text('설명 1'), findsOneWidget);
    });

    testWidgets('스와이프하면 다음 페이지로 이동한다', (tester) async {
      await tester.pumpWidget(buildWidget());

      // 왼쪽으로 스와이프 → 2페이지
      await tester.drag(find.byType(PageView), const Offset(-400, 0));
      await tester.pumpAndSettle();

      expect(find.text('페이지 2'), findsOneWidget);
    });

    testWidgets('다음 버튼으로 페이지가 전환된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      expect(find.text('다음'), findsOneWidget);

      await tester.tap(find.text('다음'));
      await tester.pumpAndSettle();

      expect(find.text('페이지 2'), findsOneWidget);
    });

    testWidgets('마지막 페이지에서 시작하기 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      // 2번 스와이프 → 마지막 페이지
      await tester.drag(find.byType(PageView), const Offset(-400, 0));
      await tester.pumpAndSettle();
      await tester.drag(find.byType(PageView), const Offset(-400, 0));
      await tester.pumpAndSettle();

      expect(find.text('시작하기'), findsOneWidget);
      expect(find.text('마지막 페이지'), findsOneWidget);
    });

    testWidgets('시작하기 버튼을 누르면 onComplete가 호출된다', (tester) async {
      var completed = false;
      await tester.pumpWidget(buildWidget(onComplete: () => completed = true));

      // 마지막 페이지로 이동
      await tester.drag(find.byType(PageView), const Offset(-400, 0));
      await tester.pumpAndSettle();
      await tester.drag(find.byType(PageView), const Offset(-400, 0));
      await tester.pumpAndSettle();

      await tester.tap(find.text('시작하기'));
      await tester.pumpAndSettle();

      expect(completed, isTrue);
    });

    testWidgets('건너뛰기 버튼을 누르면 onComplete가 호출된다', (tester) async {
      var completed = false;
      await tester.pumpWidget(buildWidget(onComplete: () => completed = true));

      await tester.tap(find.text('건너뛰기'));
      await tester.pumpAndSettle();

      expect(completed, isTrue);
    });

    testWidgets('마지막 페이지에서 건너뛰기 버튼은 숨겨진다', (tester) async {
      await tester.pumpWidget(buildWidget());

      // 마지막 페이지로 이동
      await tester.drag(find.byType(PageView), const Offset(-400, 0));
      await tester.pumpAndSettle();
      await tester.drag(find.byType(PageView), const Offset(-400, 0));
      await tester.pumpAndSettle();

      // 건너뛰기 버튼이 opacity 0으로 숨겨짐
      final opacity = tester.widget<AnimatedOpacity>(
        find.ancestor(
          of: find.text('건너뛰기'),
          matching: find.byType(AnimatedOpacity),
        ),
      );
      expect(opacity.opacity, 0.0);
    });

    testWidgets('dot indicator가 페이지 수만큼 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget());

      // AnimatedContainer가 3개(페이지 수) 있어야 함
      final dots = find.byType(AnimatedContainer);
      expect(dots, findsNWidgets(testPages.length));
    });
  });
}
