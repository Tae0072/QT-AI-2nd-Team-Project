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
    expect(find.byTooltip('하이라이트'), findsOneWidget);
    expect(find.byTooltip('들여쓰기'), findsOneWidget);
    expect(find.byTooltip('동그라미 목록'), findsOneWidget);
    expect(find.byTooltip('(1) 목록'), findsOneWidget);
    expect(find.byTooltip('1) 목록'), findsOneWidget);

    await tester.enterText(find.bySemanticsLabel('노트 작성'), '오늘 적용 🙂');
    expect(find.text('오늘 적용 🙂'), findsOneWidget);
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

  testWidgets('글씨 크기를 숫자로 지정하고 이모티콘 크기는 기본값으로 유지한다', (tester) async {
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

    await tester.enterText(
        find.byKey(const ValueKey('note-font-size-input')), '24');
    await tester.tap(find.text('적용'));
    await tester.pumpAndSettle();

    final textField = tester.widget<TextField>(
      find.byKey(const ValueKey('qt-note-body-input')),
    );
    expect(textField.style?.fontSize, 24);

    final span = textField.controller!.buildTextSpan(
      context: tester.element(find.byKey(const ValueKey('qt-note-body-input'))),
      style: textField.style!,
      withComposing: false,
    );
    final emojiSpan = span.children!
        .whereType<TextSpan>()
        .firstWhere((child) => child.text == '🙂');
    expect(emojiSpan.style?.fontSize, 16);
  });

  testWidgets('하이라이트는 마커만 보이지 않고 선택한 텍스트를 강조 표시한다', (tester) async {
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

    await tester.tap(find.byTooltip('하이라이트'));
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
        .where((child) => child.text == '==')
        .toList();

    expect(textField.controller!.text, '==강조==');
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
}

class _FakeBibleRepository extends BibleRepository {
  _FakeBibleRepository() : super(Dio());

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
