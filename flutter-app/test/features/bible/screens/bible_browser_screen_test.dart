import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/screens/bible_browser_screen.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';
import 'package:qtai_app/l10n/app_localizations.dart';

void main() {
  testWidgets(
      'BibleBrowserScreen renders TOC picker and opens selected passage',
      (tester) async {
    final repository = _FakeBibleRepository();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider.overrideWithValue(repository),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: const BibleBrowserScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('목차검색 :: 성경본문'), findsOneWidget);
    expect(find.text('창세기'), findsWidgets);
    expect(find.text('Genesis'), findsOneWidget);
    expect(find.text('율법서'), findsOneWidget);
    expect(find.byKey(const Key('bible-book-list')), findsOneWidget);
    expect(find.byKey(const Key('bible-chapter-list')), findsOneWidget);
    expect(find.byKey(const Key('bible-verse-list')), findsOneWidget);
    expect(find.byKey(const Key('bible-book-picker')), findsNothing);
    expect(find.byKey(const Key('bible-verse-from-picker')), findsNothing);
    expect(find.byKey(const Key('bible-verse-to-picker')), findsNothing);

    await tester.tap(find.text('2절'));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('bible-selection-bar')));
    await tester.pumpAndSettle();

    expect(repository.requestedBookCode, 'GEN');
    expect(repository.requestedChapter, 1);
    expect(repository.requestedVerseFrom, 2);
    expect(repository.requestedVerseTo, 2);
    expect(find.text('1:2'), findsOneWidget);
    expect(find.text('테스트 본문 2'), findsOneWidget);
    expect(find.text('Test English body 2'), findsNothing);

    await tester.tap(find.byKey(const Key('bible-browser-english-toggle')));
    await tester.pumpAndSettle();

    expect(find.text('Genesis'), findsWidgets);
    expect(find.text('Test English body 2'), findsOneWidget);
  });
}

class _FakeBibleRepository extends BibleRepository {
  String? requestedBookCode;
  int? requestedChapter;
  int? requestedVerseFrom;
  int? requestedVerseTo;

  _FakeBibleRepository() : super(Dio());

  @override
  Future<List<BibleBook>> getBooks() async {
    return const [
      BibleBook(
        id: 46,
        testament: 'OLD',
        code: 'GEN',
        koreanName: '창세기',
        englishName: 'Genesis',
        displayOrder: 1,
      ),
    ];
  }

  @override
  Future<BibleVerseRange> getChapterVerses({
    required String bookCode,
    required int chapter,
  }) async {
    return BibleVerseRange(
      book: BibleVerseBook(
        code: bookCode,
        koreanName: '창세기',
        englishName: 'Genesis',
        chapter: chapter,
      ),
      verses: [
        BibleVerse(
          id: 1001,
          bookCode: bookCode,
          chapterNo: chapter,
          verseNo: 1,
          koreanText: '테스트 본문 1',
          englishText: 'Test English body 1',
        ),
        BibleVerse(
          id: 1002,
          bookCode: bookCode,
          chapterNo: chapter,
          verseNo: 2,
          koreanText: '테스트 본문 2',
          englishText: 'Test English body 2',
        ),
      ],
    );
  }

  @override
  Future<BibleVerseRange> getVerses({
    required String bookCode,
    required int chapter,
    required int verseFrom,
    required int verseTo,
  }) async {
    requestedBookCode = bookCode;
    requestedChapter = chapter;
    requestedVerseFrom = verseFrom;
    requestedVerseTo = verseTo;
    return BibleVerseRange(
      book: BibleVerseBook(
        code: bookCode,
        koreanName: '창세기',
        englishName: 'Genesis',
        chapter: chapter,
      ),
      verses: [
        BibleVerse(
          id: 1000 + verseFrom,
          bookCode: bookCode,
          chapterNo: chapter,
          verseNo: verseFrom,
          koreanText: '테스트 본문 $verseFrom',
          englishText: 'Test English body $verseFrom',
        ),
      ],
    );
  }
}
