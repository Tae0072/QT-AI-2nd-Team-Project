import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/models/bible_reference.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';
import 'package:qtai_app/features/note/screens/qt_note_editor_screen.dart';

void main() {
  testWidgets('QT 노트 작성 화면은 본문과 작성 영역을 분리하고 이모티콘 입력을 유지한다', (tester) async {
    const args = QtNoteEditorArgs(
      passage: TodayQtPassage(
        qtPassageId: 3,
        passageDate: '2026-06-05',
        title: '성령으로 깨닫는 하나님의 지혜',
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
      ),
    );

    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(home: QtNoteEditorScreen(args: args)),
      ),
    );

    expect(find.text('QT 노트'), findsOneWidget);
    expect(
        find.byKey(const ValueKey('qt-note-passage-scroll')), findsOneWidget);
    expect(find.byKey(const ValueKey('qt-note-editor-scroll')), findsOneWidget);
    expect(find.byTooltip('글씨 크기'), findsOneWidget);
    expect(find.byTooltip('굵게'), findsOneWidget);
    expect(find.byTooltip('텍스트 색상'), findsOneWidget);
    expect(find.byTooltip('배경 색상'), findsOneWidget);
    expect(find.byTooltip('하이라이트'), findsNothing);
    expect(find.byTooltip('들여쓰기'), findsOneWidget);
    expect(find.byTooltip('동그라미 목록'), findsOneWidget);
    expect(find.byTooltip('(1) 목록'), findsOneWidget);
    expect(find.byTooltip('1) 목록'), findsOneWidget);

    await tester.enterText(find.bySemanticsLabel('노트 작성'), '오늘 적용 🙂');
    expect(find.text('오늘 적용 🙂'), findsOneWidget);
  });

  testWidgets('작은 화면에서도 구절 삽입 버튼을 바로 누를 수 있다', (tester) async {
    tester.view.physicalSize = const Size(393, 852);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    const args = QtNoteEditorArgs(
      passage: TodayQtPassage(
        qtPassageId: 3,
        passageDate: '2026-06-05',
        title: '성령으로 깨닫는 하나님의 지혜',
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
      ),
    );

    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(home: QtNoteEditorScreen(args: args)),
      ),
    );

    expect(find.byTooltip('구절 삽입').hitTestable(), findsOneWidget);
  });

  testWidgets('@Ge 영어 입력으로 창세기 추천을 표시한다', (tester) async {
    const args = QtNoteEditorArgs(
      passage: TodayQtPassage(
        qtPassageId: 3,
        passageDate: '2026-06-05',
        title: '성령으로 깨닫는 하나님의 지혜',
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
      ),
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider.overrideWithValue(_FakeBibleRepository()),
        ],
        child: const MaterialApp(home: QtNoteEditorScreen(args: args)),
      ),
    );

    await tester.enterText(find.bySemanticsLabel('노트 작성'), '@Ge');
    await tester.pumpAndSettle();

    expect(find.text('창세기'), findsOneWidget);
    expect(find.text('출애굽기'), findsNothing);
  });

  testWidgets('구절 삽입 버튼을 누르면 @ 입력과 성경 권 추천을 바로 표시한다', (tester) async {
    const args = QtNoteEditorArgs(
      passage: TodayQtPassage(
        qtPassageId: 3,
        passageDate: '2026-06-05',
        title: '성령으로 깨닫는 하나님의 지혜',
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
      ),
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider.overrideWithValue(_FakeBibleRepository()),
        ],
        child: const MaterialApp(home: QtNoteEditorScreen(args: args)),
      ),
    );

    await tester.tap(find.byTooltip('구절 삽입'));
    await tester.pumpAndSettle();

    final textField = tester.widget<TextField>(
      find.byKey(const ValueKey('qt-note-body-input')),
    );
    expect(textField.controller!.text, '@');
    expect(find.text('창세기'), findsOneWidget);
    expect(find.text('출애굽기'), findsOneWidget);
  });

  testWidgets('@창 1:1 입력 중 스크롤 추천에서 구절을 바로 삽입한다', (tester) async {
    const args = QtNoteEditorArgs(
      passage: TodayQtPassage(
        qtPassageId: 3,
        passageDate: '2026-06-05',
        title: '성령으로 깨닫는 하나님의 지혜',
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
      ),
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider.overrideWithValue(_FakeBibleRepository()),
        ],
        child: const MaterialApp(home: QtNoteEditorScreen(args: args)),
      ),
    );

    await tester.enterText(find.bySemanticsLabel('노트 작성'), '@창');
    await tester.pumpAndSettle();

    expect(find.text('창세기'), findsOneWidget);
    expect(find.text('출애굽기'), findsNothing);

    await tester.enterText(find.bySemanticsLabel('노트 작성'), '@창 1:1');
    await tester.pumpAndSettle();

    expect(find.text('창세기 1:1 삽입'), findsOneWidget);

    await tester.tap(find.text('창세기 1:1 삽입'));
    await tester.pumpAndSettle();

    expect(find.textContaining('창세기 1:1 더미 구절 본문 1'), findsOneWidget);
    expect(find.text('@창 1:1'), findsNothing);
  });

  testWidgets('@창 권 선택 후 장과 절 피커로 구절을 삽입한다', (tester) async {
    const args = QtNoteEditorArgs(
      passage: TodayQtPassage(
        qtPassageId: 3,
        passageDate: '2026-06-05',
        title: '성령으로 깨닫는 하나님의 지혜',
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
      ),
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider.overrideWithValue(_FakeBibleRepository()),
        ],
        child: const MaterialApp(home: QtNoteEditorScreen(args: args)),
      ),
    );

    await tester.enterText(find.bySemanticsLabel('노트 작성'), '@창');
    await tester.pumpAndSettle();

    await tester.tap(find.text('창세기'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('qt-note-mention-chapter-picker')),
        findsOneWidget);
    expect(find.byKey(const Key('qt-note-mention-verse-from-picker')),
        findsOneWidget);
    expect(find.byKey(const Key('qt-note-mention-verse-to-picker')),
        findsOneWidget);
    expect(find.text('창세기 1:1 삽입'), findsOneWidget);

    await tester.tap(find.text('창세기 1:1 삽입'));
    await tester.pumpAndSettle();

    expect(find.textContaining('창세기 1:1 더미 구절 본문 1'), findsOneWidget);
    expect(find.text('@창'), findsNothing);
  });

  testWidgets('글씨 크기를 슬라이더로 지정하고 이후 입력부터 적용한다', (tester) async {
    const args = QtNoteEditorArgs(
      passage: TodayQtPassage(
        qtPassageId: 3,
        passageDate: '2026-06-05',
        title: '성령으로 깨닫는 하나님의 지혜',
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
      ),
    );

    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(home: QtNoteEditorScreen(args: args)),
      ),
    );

    await tester.enterText(find.bySemanticsLabel('노트 작성'), '본문 🙂');
    await tester.tap(find.byTooltip('글씨 크기'));
    await tester.pumpAndSettle();

    expect(find.text('글씨 크기'), findsOneWidget);
    expect(find.byKey(const ValueKey('note-font-size-slider')), findsOneWidget);

    final slider = tester
        .widget<Slider>(find.byKey(const ValueKey('note-font-size-slider')));
    slider.onChanged!(24);
    await tester.pump();
    await tester.tap(find.text('적용'));
    await tester.pumpAndSettle();

    final textField = tester.widget<TextField>(
      find.byKey(const ValueKey('qt-note-body-input')),
    );
    final controller = textField.controller!;
    expect(textField.style?.fontSize, 16);
    expect(controller.text, '본문 🙂[fs=24]');

    await tester.enterText(
      find.byKey(const ValueKey('qt-note-body-input')),
      '${controller.text}큰글',
    );

    final span = textField.controller!.buildTextSpan(
      context: tester.element(find.byKey(const ValueKey('qt-note-body-input'))),
      style: textField.style!,
      withComposing: false,
    );
    final emojiSpan = span.children!
        .whereType<TextSpan>()
        .firstWhere((child) => child.text == '🙂');
    expect(emojiSpan.style?.fontSize, 16);

    final styledSpan = span.children!
        .whereType<TextSpan>()
        .firstWhere((child) => child.text == '큰');
    expect(styledSpan.style?.fontSize, 24);
  });

  testWidgets('배경 색상은 하이라이트 대신 선택 텍스트에 적용된다', (tester) async {
    const args = QtNoteEditorArgs(
      passage: TodayQtPassage(
        qtPassageId: 3,
        passageDate: '2026-06-05',
        title: '성령으로 깨닫는 하나님의 지혜',
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
      ),
    );

    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(home: QtNoteEditorScreen(args: args)),
      ),
    );

    await tester.enterText(
        find.byKey(const ValueKey('qt-note-body-input')), '강조');

    final textField = tester.widget<TextField>(
      find.byKey(const ValueKey('qt-note-body-input')),
    );
    textField.controller!.selection = const TextSelection(
      baseOffset: 0,
      extentOffset: 2,
    );

    await tester.tap(find.byTooltip('배경 색상'));
    await tester.pumpAndSettle();
    await tester.tap(
      find.byKey(
          ValueKey('note-color-swatch-${const Color(0xFFFEE2E2).toARGB32()}')),
    );
    await tester.pumpAndSettle();

    final span = textField.controller!.buildTextSpan(
      context: tester.element(find.byKey(const ValueKey('qt-note-body-input'))),
      style: textField.style!,
      withComposing: false,
    );
    final highlighted = span.children!
        .whereType<TextSpan>()
        .where((child) => child.text == '강' || child.text == '조')
        .toList();
    final markers = span.children!
        .whereType<TextSpan>()
        .where((child) => child.text?.startsWith('[bg=') ?? false)
        .toList();

    expect(textField.controller!.text, startsWith('[bg='));
    expect(textField.controller!.text, contains('강조[bg=]'));
    expect(highlighted, hasLength(2));
    expect(
      highlighted.every((child) => child.style?.backgroundColor != null),
      isTrue,
    );
    expect(markers, hasLength(2));
    expect(
      markers.every((marker) => marker.style?.color == Colors.transparent),
      isTrue,
    );
  });

  testWidgets('본문 입력창은 한국어 입력 힌트를 기본으로 쓰고 입력기 보조 UI를 최소화한다', (tester) async {
    const args = QtNoteEditorArgs(
      passage: TodayQtPassage(
        qtPassageId: 3,
        passageDate: '2026-06-05',
        title: '성령으로 깨닫는 하나님의 지혜',
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
      ),
    );

    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(home: QtNoteEditorScreen(args: args)),
      ),
    );

    final textField = tester.widget<TextField>(
      find.byKey(const ValueKey('qt-note-body-input')),
    );

    expect(textField.hintLocales, const [Locale('ko', 'KR')]);
    expect(textField.enableSuggestions, isFalse);
    expect(textField.autocorrect, isFalse);
  });
  testWidgets('empty numbered list prefix is removed on trailing space',
      (tester) async {
    const args = QtNoteEditorArgs(
      passage: TodayQtPassage(
        qtPassageId: 3,
        passageDate: '2026-06-05',
        title: 'title',
        reference: BibleReference(
          koreanBookName: 'Genesis',
          englishBookName: 'Genesis',
          chapter: 1,
          verseFrom: 1,
          verseTo: 1,
        ),
        book: BibleVerseBook(
          code: 'GEN',
          koreanName: 'Genesis',
          englishName: 'Genesis',
          chapter: 1,
        ),
        verses: [
          BibleVerse(
            id: 1,
            bookCode: 'GEN',
            chapterNo: 1,
            verseNo: 1,
            koreanText: 'body',
            englishText: 'body',
          ),
        ],
      ),
    );

    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(home: QtNoteEditorScreen(args: args)),
      ),
    );

    final body = find.byKey(const ValueKey('qt-note-body-input'));
    await tester.enterText(body, '(1) ');
    await tester.enterText(body, '(1)  ');

    var textField = tester.widget<TextField>(body);
    expect(textField.controller!.text, isEmpty);

    await tester.enterText(body, '  1) ');
    await tester.enterText(body, '  1)  ');

    textField = tester.widget<TextField>(body);
    expect(textField.controller!.text, '  ');

    await tester.enterText(body, '(1) body ');

    textField = tester.widget<TextField>(body);
    expect(textField.controller!.text, '(1) body ');
  });

  testWidgets('mention picker disables insert when chapter verses fail',
      (tester) async {
    const args = QtNoteEditorArgs(
      passage: TodayQtPassage(
        qtPassageId: 3,
        passageDate: '2026-06-05',
        title: 'title',
        reference: BibleReference(
          koreanBookName: 'Genesis',
          englishBookName: 'Genesis',
          chapter: 1,
          verseFrom: 1,
          verseTo: 1,
        ),
        book: BibleVerseBook(
          code: 'GEN',
          koreanName: 'Genesis',
          englishName: 'Genesis',
          chapter: 1,
        ),
        verses: [
          BibleVerse(
            id: 1,
            bookCode: 'GEN',
            chapterNo: 1,
            verseNo: 1,
            koreanText: 'body',
            englishText: 'body',
          ),
        ],
      ),
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider.overrideWithValue(
            _FakeBibleRepository(failChapterVerses: true),
          ),
        ],
        child: const MaterialApp(home: QtNoteEditorScreen(args: args)),
      ),
    );

    await tester.enterText(
      find.byKey(const ValueKey('qt-note-body-input')),
      '@Ge',
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Genesis').first);
    await tester.pumpAndSettle();

    final insertButtonFinder = find.ancestor(
      of: find.textContaining('1:1'),
      matching: find.byType(FilledButton),
    );
    final insertButton = tester.widget<FilledButton>(insertButtonFinder);

    expect(find.byKey(const Key('qt-note-mention-chapter-picker')),
        findsOneWidget);
    expect(insertButton.onPressed, isNull);
  });
}

class _FakeBibleRepository extends BibleRepository {
  final bool failChapterVerses;

  _FakeBibleRepository({this.failChapterVerses = false}) : super(Dio());

  @override
  Future<List<BibleBook>> getBooks() async {
    return const [
      BibleBook(
        id: 1,
        testament: 'OLD',
        code: 'GEN',
        koreanName: '창세기',
        englishName: 'Genesis',
        displayOrder: 1,
      ),
      BibleBook(
        id: 2,
        testament: 'OLD',
        code: 'EXO',
        koreanName: '출애굽기',
        englishName: 'Exodus',
        displayOrder: 2,
      ),
    ];
  }

  @override
  Future<BibleVerseRange> getChapterVerses({
    required String bookCode,
    required int chapter,
  }) async {
    if (failChapterVerses) {
      throw StateError('chapter boom');
    }
    return const BibleVerseRange(
      book: BibleVerseBook(
        code: 'GEN',
        koreanName: '창세기',
        englishName: 'Genesis',
        chapter: 1,
      ),
      verses: [
        BibleVerse(
          id: 1001001,
          bookCode: 'GEN',
          chapterNo: 1,
          verseNo: 1,
          koreanText: '더미 구절 본문 1',
          englishText: 'Dummy English verse 1',
        ),
        BibleVerse(
          id: 1001002,
          bookCode: 'GEN',
          chapterNo: 1,
          verseNo: 2,
          koreanText: '더미 구절 본문 2',
          englishText: 'Dummy English verse 2',
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
    return const BibleVerseRange(
      book: BibleVerseBook(
        code: 'GEN',
        koreanName: '창세기',
        englishName: 'Genesis',
        chapter: 1,
      ),
      verses: [
        BibleVerse(
          id: 1001001,
          bookCode: 'GEN',
          chapterNo: 1,
          verseNo: 1,
          koreanText: '더미 구절 본문 1',
          englishText: 'Dummy English verse 1',
        ),
      ],
    );
  }
}
