import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/models/bible_reference.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/screens/today_qt_screen.dart';
import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart';
import 'package:qtai_app/features/tts/widgets/qt_tts_button.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  testWidgets('오늘 QT 화면은 파싱된 본문 범위와 절 목록을 표시한다', (tester) async {
    // TTS 버튼(QtTtsButton)이 목소리/읽기 범위 설정을 읽으므로
    // sharedPreferencesProvider를 테스트용 mock 인스턴스로 override한다.
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    const passage = TodayQtPassage(
      reference: BibleReference(
        koreanBookName: '고린도전서',
        englishBookName: '1 Corinthians',
        chapter: 1,
        verseFrom: 10,
        verseTo: 17,
      ),
      book: BibleVerseBook(
        code: '1CO',
        koreanName: '고린도전서',
        englishName: '1 Corinthians',
        chapter: 1,
      ),
      verses: [
        BibleVerse(
          id: 1010,
          bookCode: '1CO',
          chapterNo: 1,
          verseNo: 10,
          koreanText: '더미 한글 본문 10',
          englishText: 'Dummy English verse 10',
        ),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          todayQtPassageProvider.overrideWith((ref) async => passage),
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
        child: MaterialApp(localizationsDelegates: AppLocalizations.localizationsDelegates, supportedLocales: AppLocalizations.supportedLocales, locale: const Locale('ko'), home: const TodayQtScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('오늘 QT'), findsOneWidget);
    expect(find.text('고린도전서 1:10-17'), findsOneWidget);
    expect(find.text('1 Corinthians'), findsOneWidget);
    expect(find.text('더미 한글 본문 10'), findsOneWidget);
    expect(find.text('Dummy English verse 10'), findsOneWidget);
    // TTS 읽기 버튼이 앱바에 표시된다 (토큰 미설정이라 준비는 건너뜀)
    expect(find.byType(QtTtsButton), findsOneWidget);
  });
}
