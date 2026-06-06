import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/screens/bible_browser_screen.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';

void main() {
  testWidgets('BibleBrowserScreen renders selected passage after search',
      (tester) async {
    final repository = _FakeBibleRepository();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider.overrideWithValue(repository),
        ],
        child: MaterialApp(localizationsDelegates: AppLocalizations.localizationsDelegates, supportedLocales: AppLocalizations.supportedLocales, locale: const Locale('ko'), home: const BibleBrowserScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('성경본문'), findsOneWidget);
    expect(find.text('고린도전서'), findsOneWidget);

    await tester.enterText(find.byKey(const Key('bible-chapter-input')), '2');
    await tester.tap(find.text('조회'));
    await tester.pumpAndSettle();

    expect(repository.requestedBookCode, '1CO');
    expect(repository.requestedChapter, 2);
    expect(find.text('2:1'), findsOneWidget);
    expect(find.text('테스트 본문 1'), findsOneWidget);
  });
}

class _FakeBibleRepository extends BibleRepository {
  String? requestedBookCode;
  int? requestedChapter;

  _FakeBibleRepository() : super(Dio());

  @override
  Future<List<BibleBook>> getBooks() async {
    return const [
      BibleBook(
        id: 46,
        testament: 'NEW',
        code: '1CO',
        koreanName: '고린도전서',
        englishName: '1 Corinthians',
        displayOrder: 46,
      ),
    ];
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
    return const BibleVerseRange(
      book: BibleVerseBook(
        code: '1CO',
        koreanName: '고린도전서',
        englishName: '1 Corinthians',
        chapter: 2,
      ),
      verses: [
        BibleVerse(
          id: 46002001,
          bookCode: '1CO',
          chapterNo: 2,
          verseNo: 1,
          koreanText: '테스트 본문 1',
          englishText: null,
        ),
      ],
    );
  }
}
