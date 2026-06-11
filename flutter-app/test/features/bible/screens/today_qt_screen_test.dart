import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/models/bible_reference.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/screens/today_qt_screen.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';
import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart';
import 'package:qtai_app/features/tts/widgets/qt_tts_button.dart';
import 'package:qtai_app/routes/app_router.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  testWidgets('오늘 QT 화면은 기본 한글 본문만 보이고 영어는 선택 시 표시한다', (tester) async {
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
        child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            home: const TodayQtScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('오늘 QT'), findsOneWidget);
    expect(find.text('고린도전서 1:10-17'), findsOneWidget);
    expect(find.text('더미 한글 본문 10'), findsOneWidget);
    expect(find.text('1 Corinthians'), findsNothing);
    expect(find.text('Dummy English verse 10'), findsNothing);

    await tester.tap(find.byKey(const Key('today-qt-english-toggle')));
    await tester.pumpAndSettle();

    expect(find.text('1 Corinthians'), findsOneWidget);
    expect(find.text('Dummy English verse 10'), findsOneWidget);
    expect(find.byType(QtTtsButton), findsOneWidget);
  });

  testWidgets('노트 버튼을 누르면 오늘 QT 본문을 가진 노트 작성 화면으로 이동한다', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    const passage = TodayQtPassage(
      qtPassageId: 7,
      passageDate: '2026-06-05',
      title: '성령으로 깨닫는 하나님의 지혜',
      hasExplanation: true,
      reference: BibleReference(
        koreanBookName: '고린도전서',
        englishBookName: '1 Corinthians',
        chapter: 2,
        verseFrom: 1,
        verseTo: 16,
      ),
      book: BibleVerseBook(
        code: '1CO',
        koreanName: '고린도전서',
        englishName: '1 Corinthians',
        chapter: 2,
      ),
      verses: [
        BibleVerse(
          id: 2001,
          bookCode: '1CO',
          chapterNo: 2,
          verseNo: 1,
          koreanText: '더미 한글 본문 1',
          englishText: 'Dummy English verse 1',
        ),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          todayQtPassageProvider.overrideWith((ref) async => passage),
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          onGenerateRoute: AppRouter.onGenerateRoute,
          home: const TodayQtScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('노트'));
    await tester.pumpAndSettle();

    expect(find.text('QT 노트'), findsOneWidget);
    expect(find.text('고린도전서 2:1-16'), findsOneWidget);
    expect(find.text('더미 한글 본문 1'), findsOneWidget);
    expect(find.byTooltip('굵게'), findsOneWidget);
    expect(find.byTooltip('텍스트 색상'), findsOneWidget);
    expect(find.byTooltip('배경 색상'), findsOneWidget);
    expect(find.byTooltip('구절 삽입'), findsOneWidget);
  });

  testWidgets('해설 버튼을 누르면 준비된 해설이 없을 때 빈 상태를 표시한다', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    const passage = TodayQtPassage(
      qtPassageId: 7,
      passageDate: '2026-06-05',
      title: '성령으로 깨닫는 하나님의 지혜',
      hasExplanation: true,
      reference: BibleReference(
        koreanBookName: '고린도전서',
        englishBookName: '1 Corinthians',
        chapter: 2,
        verseFrom: 1,
        verseTo: 16,
      ),
      book: BibleVerseBook(
        code: '1CO',
        koreanName: '고린도전서',
        englishName: '1 Corinthians',
        chapter: 2,
      ),
      verses: [
        BibleVerse(
          id: 2001,
          bookCode: '1CO',
          chapterNo: 2,
          verseNo: 1,
          koreanText: '더미 한글 본문 1',
          englishText: 'Dummy English verse 1',
        ),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          todayQtPassageProvider.overrideWith((ref) async => passage),
          bibleRepositoryProvider.overrideWithValue(_FakeBibleRepository()),
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          onGenerateRoute: AppRouter.onGenerateRoute,
          home: const TodayQtScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('해설'));
    await tester.pumpAndSettle();

    expect(find.text('해설'), findsOneWidget);
    expect(find.text('아직 준비된 해설이 없습니다.'), findsOneWidget);
  });
}

class _FakeBibleRepository extends BibleRepository {
  _FakeBibleRepository() : super(Dio());

  @override
  Future<QtStudyContent> getQtStudyContent(int qtPassageId) async {
    return const QtStudyContent(
      summary: null,
      explanations: [],
      glossaryTerms: [],
    );
  }
}
