import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/screens/bible_passage_screen.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/study/screens/qt_study_content_screen.dart';
import 'package:qtai_app/routes/app_router.dart';
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

// 탭-탭 범위/노트 동봉 검증용 3절 장.
const _chapter3 = BibleVerseRange(
  book: BibleVerseBook(
    code: 'GEN',
    koreanName: '창세기',
    englishName: 'Genesis',
    chapter: 1,
  ),
  verses: [
    BibleVerse(
        id: 2001,
        bookCode: 'GEN',
        chapterNo: 1,
        verseNo: 1,
        koreanText: '테스트 본문 1',
        englishText: 'Body 1'),
    BibleVerse(
        id: 2002,
        bookCode: 'GEN',
        chapterNo: 1,
        verseNo: 2,
        koreanText: '테스트 본문 2',
        englishText: 'Body 2'),
    BibleVerse(
        id: 2003,
        bookCode: 'GEN',
        chapterNo: 1,
        verseNo: 3,
        koreanText: '테스트 본문 3',
        englishText: 'Body 3'),
  ],
);

const _emptyChapter = BibleVerseRange(
  book: BibleVerseBook(
    code: 'GEN',
    koreanName: '창세기',
    englishName: 'Genesis',
    chapter: 1,
  ),
  verses: [],
);

Future<void> _pump(
  WidgetTester tester,
  BibleRepository repository, {
  BibleVerseRange chapter = _range,
  int focusVerseNo = 1,
}) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: [bibleRepositoryProvider.overrideWithValue(repository)],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        locale: const Locale('ko'),
        home: BiblePassageScreen(chapter: chapter, focusVerseNo: focusVerseNo),
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

  testWidgets('빈 장은 해설 가용성을 조회하지 않는다', (tester) async {
    final repository = _FakeRepo(BiblePassageStudy.none);

    await _pump(tester, repository, chapter: _emptyChapter);

    expect(repository.studyRequests, isEmpty);
    final button = tester.widget<FilledButton>(
        find.byKey(const Key('bible-explanation-button')));
    expect(button.onPressed, isNull);
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

  testWidgets('절을 탭-탭하면 선택 범위 라벨이 갱신된다', (tester) async {
    await _pump(tester, _FakeRepo(BiblePassageStudy.none),
        chapter: _chapter3, focusVerseNo: 1);

    // 진입 시 포커스 1절 단일 선택(하단 액션바 라벨).
    expect(find.text('창세기 1:1'), findsOneWidget);

    // 2절 탭 → 단일 2, 3절 탭 → 범위 2-3.
    await tester.tap(find.text('테스트 본문 2'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('테스트 본문 3'));
    await tester.pumpAndSettle();

    expect(find.text('창세기 1:2-3'), findsOneWidget);
  });

  testWidgets('해설 조회와 진입 인자는 현재 선택 범위를 따른다', (tester) async {
    QtStudyContentArgs? captured;
    final repository = _FakeRepo.byRange((range) {
      if (range.verseFrom == 2 && range.verseTo == 3) {
        return const BiblePassageStudy(
          qtPassageId: 42,
          hasExplanation: true,
        );
      }
      return BiblePassageStudy.none;
    });

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider.overrideWithValue(repository),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: const BiblePassageScreen(chapter: _chapter3, focusVerseNo: 1),
          onGenerateRoute: (settings) {
            if (settings.name == AppRouter.qtStudyContent) {
              captured = settings.arguments as QtStudyContentArgs?;
            }
            return MaterialPageRoute(
                builder: (_) => const Scaffold(body: SizedBox()));
          },
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('테스트 본문 2'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('테스트 본문 3'));
    await tester.pumpAndSettle();

    expect(
      repository.studyRequests.last,
      (
        bookCode: 'GEN',
        chapter: 1,
        verseFrom: 2,
        verseTo: 3,
      ),
    );
    final button = tester.widget<FilledButton>(
        find.byKey(const Key('bible-explanation-button')));
    expect(button.onPressed, isNotNull);

    await tester.tap(find.byKey(const Key('bible-explanation-button')));
    await tester.pumpAndSettle();

    expect(captured, isNotNull);
    expect(captured!.qtPassageId, 42);
    expect(captured!.referenceText, '창세기 1:2-3');
    expect(captured!.verseLabels, {2002: '2', 2003: '3'});
  });

  testWidgets('노트 작성하기는 선택 절을 동봉해 설교 노트로 이동한다', (tester) async {
    NoteEditArgs? captured;
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider
              .overrideWithValue(_FakeRepo(BiblePassageStudy.none)),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: const BiblePassageScreen(chapter: _chapter3, focusVerseNo: 2),
          onGenerateRoute: (settings) {
            if (settings.name == AppRouter.noteEdit) {
              captured = settings.arguments as NoteEditArgs?;
            }
            return MaterialPageRoute(
                builder: (_) => const Scaffold(body: SizedBox()));
          },
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('노트 작성하기'));
    await tester.pumpAndSettle();

    expect(captured, isNotNull);
    expect(captured!.category, 'SERMON');
    // 진입 포커스 2절(id 2002)이 단일 선택으로 동봉된다.
    expect(captured!.verseIds, [2002]);
    expect(captured!.referenceText, '창세기 1:2');
  });
}

class _FakeRepo extends BibleRepository {
  final BiblePassageStudy Function(BiblePassageRef range) _studyFor;
  final List<BiblePassageRef> studyRequests = [];

  _FakeRepo(BiblePassageStudy study)
      : _studyFor = ((_) => study),
        super(Dio());

  _FakeRepo.byRange(this._studyFor) : super(Dio());

  @override
  Future<BiblePassageStudy> getBiblePassageStudy({
    required String bookCode,
    required int chapter,
    required int verseFrom,
    required int verseTo,
  }) async {
    final range = (
      bookCode: bookCode,
      chapter: chapter,
      verseFrom: verseFrom,
      verseTo: verseTo,
    );
    studyRequests.add(range);
    return _studyFor(range);
  }
}
