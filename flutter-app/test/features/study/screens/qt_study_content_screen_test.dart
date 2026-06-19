import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/core/theme/app_theme.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/study/screens/qt_study_content_screen.dart';
import 'package:qtai_app/l10n/app_localizations.dart';

/// F-08 — 절별 해설 텍스트 스타일 회귀 테스트.
///
/// 절 본문은 굵게+본문 강조색, 요약은 회색(text2) 유지, 해설은 파란색(explanationBlue)으로
/// 라이트/다크 테마 토큰을 사용한다. 색을 하드코딩하지 않고 AppColors 토큰으로 검증한다.
void main() {
  const passageId = 7;
  const verseText = '태초에 하나님이 천지를 창조하시니라';
  const summaryText = '한 줄 요약입니다';
  const explanationText = '이 절은 창조의 시작을 선언한다';

  Widget buildScreen(ThemeData theme) {
    const content = QtStudyContent(
      summary: null,
      explanations: [
        QtStudyExplanation(
          verseId: 1,
          summary: summaryText,
          explanation: explanationText,
          sourceLabel: null,
          aiAssetId: null,
        ),
      ],
      glossaryTerms: [],
    );

    return ProviderScope(
      overrides: [
        qtStudyContentProvider(passageId).overrideWith((ref) => content),
      ],
      child: MaterialApp(
        theme: theme,
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: const Locale('ko'),
        home: const QtStudyContentScreen(
          args: QtStudyContentArgs(
            qtPassageId: passageId,
            referenceText: '창세기 1장',
            verseLabels: {1: '1:1'},
            verseTexts: {1: verseText},
          ),
        ),
      ),
    );
  }

  TextStyle styleOf(WidgetTester tester, String text) =>
      tester.widget<Text>(find.text(text)).style!;

  testWidgets('라이트: 절 본문은 굵게+본문색, 요약은 회색, 해설은 파란색', (tester) async {
    final colors = AppTheme.lightColors;
    await tester.pumpWidget(buildScreen(AppTheme.theme));
    await tester.pumpAndSettle();

    final verseStyle = styleOf(tester, verseText);
    expect(verseStyle.fontWeight, FontWeight.w700);
    expect(verseStyle.color, colors.text);

    // 요약은 지금처럼 회색(text2) 유지.
    expect(styleOf(tester, summaryText).color, colors.text2);

    // 해설은 파란색 토큰.
    expect(styleOf(tester, explanationText).color, colors.explanationBlue);
  });

  testWidgets('다크: 해설 파란색이 다크 토큰으로 바뀐다', (tester) async {
    final colors = AppTheme.darkColors;
    await tester.pumpWidget(buildScreen(AppTheme.darkTheme));
    await tester.pumpAndSettle();

    expect(styleOf(tester, explanationText).color, colors.explanationBlue);
    expect(styleOf(tester, verseText).color, colors.text);
    // 라이트와 다른 값이어야 테마 분기가 동작하는 것.
    expect(colors.explanationBlue, isNot(AppTheme.lightColors.explanationBlue));
  });
}
