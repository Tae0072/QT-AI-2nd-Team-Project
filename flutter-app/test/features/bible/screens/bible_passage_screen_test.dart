import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/screens/bible_passage_screen.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';
import 'package:qtai_app/core/widgets/calm_paper.dart';
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
      koreanText: '테스트 본문 1절 내용',
      englishText: 'Test verse body 1',
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
        home: const BiblePassageScreen(chapter: _range, focusVerseNo: 1),
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
    expect(find.text('테스트 본문 1절 내용'), findsOneWidget);
  });

  testWidgets('해설 버튼은 가용성이 없으면 비활성', (tester) async {
    await _pump(tester, _FakeRepo(BiblePassageStudy.none));

    final button = tester.widget<FilledButton>(
        find.byKey(const Key('bible-explanation-button')));
    expect(button.onPressed, isNull);
  });

  testWidgets('해설 버튼은 승인 해설이 있으면 활성', (tester) async {
    await _pump(
      tester,
      _FakeRepo(const BiblePassageStudy(qtPassageId: 42, hasExplanation: true)),
    );

    final button = tester.widget<FilledButton>(
        find.byKey(const Key('bible-explanation-button')));
    expect(button.onPressed, isNotNull);
  });

  testWidgets('영어 토글 전에는 영어 본문이 안 보인다', (tester) async {
    await _pump(tester, _FakeRepo(BiblePassageStudy.none));

    expect(find.text('Test verse body 1'), findsNothing);
    expect(find.byType(CpSubBox), findsNothing);
  });

  testWidgets('영어 토글 시 영어 본문이 QT처럼 sub-box로 보인다', (tester) async {
    await _pump(tester, _FakeRepo(BiblePassageStudy.none));

    await tester.tap(find.byKey(const Key('bible-browser-english-toggle')));
    await tester.pumpAndSettle();

    // 영어 본문이 노출되고, 오늘의 QT와 동일한 sunken sub-box 안에 들어간다.
    expect(find.text('Test verse body 1'), findsOneWidget);
    expect(
      find.ancestor(
        of: find.text('Test verse body 1'),
        matching: find.byType(CpSubBox),
      ),
      findsOneWidget,
    );
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
