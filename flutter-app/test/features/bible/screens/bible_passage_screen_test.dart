import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/screens/bible_passage_screen.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';
import 'package:qtai_app/l10n/app_localizations.dart';

const _range = BibleVerseRange(
  book: BibleVerseBook(
    code: 'GEN',
    koreanName: '창세기',
    englishName: 'Genesis',
    chapter: 1,
  ),
  verses: [
    BibleVerse(
      id: 1,
      bookCode: 'GEN',
      chapterNo: 1,
      verseNo: 1,
      koreanText: '태초에 하나님이 천지를 창조하시니라',
      englishText: 'In the beginning God created the heavens and the earth.',
    ),
  ],
);

Future<void> _pump(WidgetTester tester, BibleRepository repository) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: [bibleRepositoryProvider.overrideWithValue(repository)],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: const Locale('ko'),
        home: const BiblePassageScreen(range: _range),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('renders passage text as a full page', (tester) async {
    await _pump(tester, _FakeRepo(BiblePassageStudy.none));

    expect(find.text('창세기 1장'), findsWidgets);
    expect(find.text('1:1'), findsOneWidget);
    expect(find.text('태초에 하나님이 천지를 창조하시니라'), findsOneWidget);
  });

  testWidgets('해설 버튼은 가용성이 없으면 비활성', (tester) async {
    await _pump(tester, _FakeRepo(BiblePassageStudy.none));

    final button = tester.widget<FilledButton>(find.byType(FilledButton));
    expect(button.onPressed, isNull);
  });

  testWidgets('해설 버튼은 승인 해설이 있으면 활성', (tester) async {
    await _pump(
      tester,
      _FakeRepo(const BiblePassageStudy(qtPassageId: 42, hasExplanation: true)),
    );

    final button = tester.widget<FilledButton>(find.byType(FilledButton));
    expect(button.onPressed, isNotNull);
  });
}

class _FakeRepo extends BibleRepository {
  final BiblePassageStudy study;

  _FakeRepo(this.study) : super(Dio());

  @override
  Future<BiblePassageStudy> getBiblePassageStudy({
    required String bookCode,
    required int chapter,
    required int verseFrom,
    required int verseTo,
  }) async {
    return study;
  }
}
