import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/models/bible_reference.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';
import 'package:qtai_app/features/note/models/note_markup_quill_codec.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/note/screens/qt_note_editor_screen.dart';
import 'package:qtai_app/features/note/services/note_repository.dart';

/// QuillEditor 본문 컨트롤러(테스트용).
QuillController _bodyQuill(WidgetTester tester, {Key? key}) =>
    tester
        .widget<QuillEditor>(
          find.byKey(key ?? const ValueKey('qt-note-body-input')),
        )
        .controller;

/// QuillEditor 본문을 [text]로 "전체 교체"한다(`enterText`와 같은 의미).
/// QuillEditor는 일반 TextField가 아니라 `enterText`가 안 통하므로 컨트롤러를 직접 조작한다.
/// 교체 후 커서를 텍스트 끝으로 옮겨 @멘션 감지 등이 실제 입력과 같게 동작하게 한다.
void _enterBody(WidgetTester tester, String text, {Key? key}) {
  final controller = _bodyQuill(tester, key: key);
  // 문서는 항상 끝에 '\n'을 가지므로, 그 앞까지(길이-1)를 교체해 본문만 바꾼다.
  final length = controller.document.length;
  controller.replaceText(
    0,
    length <= 1 ? 0 : length - 1,
    text,
    TextSelection.collapsed(offset: text.length),
  );
}

/// 본문을 저장 형식(마커 평문)으로 읽는다.
String _bodyMarkers(WidgetTester tester, {Key? key}) =>
    NoteMarkupQuillCodec.deltaToMarkers(
      _bodyQuill(tester, key: key).document.toDelta(),
    );

/// 본문에서 [base]~[extent] 범위를 선택한다(서식 적용 대상 지정).
void _selectBody(WidgetTester tester, int base, int extent, {Key? key}) {
  _bodyQuill(tester, key: key).updateSelection(
    TextSelection(baseOffset: base, extentOffset: extent),
    ChangeSource.local,
  );
}

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

    _enterBody(tester, '오늘 적용 🙂');
    await tester.pump();
    expect(_bodyMarkers(tester).trim(), '오늘 적용 🙂');
  });

  testWidgets('본문 입력 중에는 QT 본문 영역으로 튀지 않고 작성 영역과 입력값을 유지한다', (tester) async {
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

    final body = find.byKey(const ValueKey('qt-note-body-input'));
    _enterBody(tester, '첫 입력');
    await tester.pump();

    expect(find.text('QT 노트'), findsOneWidget);
    expect(_bodyMarkers(tester).trim(), '첫 입력');
    expect(
      find.byKey(const ValueKey('qt-note-passage-scroll')),
      findsOneWidget,
    );
    expect(
      find.byKey(const ValueKey('qt-note-editor-scroll')),
      findsOneWidget,
    );
    expect(body.hitTestable(), findsOneWidget);
  });

  testWidgets('키보드 inset 변화 후에도 본문 입력값과 포커스를 유지한다', (tester) async {
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

    final body = find.byKey(const ValueKey('qt-note-body-input'));
    _enterBody(tester, '첫 글자');
    await tester.pump();

    final beforeController = _bodyQuill(tester);

    tester.view.viewInsets = const FakeViewPadding(bottom: 320);
    addTearDown(tester.view.resetViewInsets);
    await tester.pump();

    // 키보드 inset이 바뀌어도 같은 컨트롤러·입력값·에디터가 유지된다.
    expect(find.text('QT 노트'), findsOneWidget);
    expect(_bodyQuill(tester), same(beforeController));
    expect(_bodyMarkers(tester).trim(), '첫 글자');
    expect(body, findsOneWidget);
    expect(
        find.byKey(const ValueKey('qt-note-passage-scroll')), findsOneWidget);
    expect(find.byKey(const ValueKey('qt-note-editor-scroll')), findsOneWidget);
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

  testWidgets('서식 수정바는 제목 입력 박스와 본문 입력창 사이에 배치된다',
      (tester) async {
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

    final titleBottom = tester.getBottomLeft(find.bySemanticsLabel('제목')).dy;
    final boldTop = tester.getTopLeft(find.byTooltip('굵게')).dy;
    final boldBottom = tester.getBottomLeft(find.byTooltip('굵게')).dy;
    final bodyTop = tester
        .getTopLeft(find.byKey(const ValueKey('qt-note-body-input')))
        .dy;

    expect(titleBottom, lessThanOrEqualTo(boldTop));
    expect(boldBottom, lessThanOrEqualTo(bodyTop));

    tester.view.viewInsets = const FakeViewPadding(bottom: 320);
    addTearDown(tester.view.resetViewInsets);
    await tester.pump();

    expect(find.byTooltip('굵게').hitTestable(), findsOneWidget);
    expect(find.byTooltip('텍스트 색상').hitTestable(), findsOneWidget);
    expect(find.byTooltip('배경 색상').hitTestable(), findsOneWidget);
    expect(
        find.byKey(const ValueKey('qt-note-passage-scroll')), findsOneWidget);
    expect(find.byKey(const ValueKey('qt-note-editor-scroll')), findsOneWidget);
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

    _enterBody(tester, '@Ge');
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

    expect(_bodyMarkers(tester), '@');
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

    _enterBody(tester, '@창');
    await tester.pumpAndSettle();

    expect(find.text('창세기'), findsOneWidget);
    expect(find.text('출애굽기'), findsNothing);

    _enterBody(tester, '@창 1:1');
    await tester.pumpAndSettle();

    expect(find.text('창세기 1:1 삽입'), findsOneWidget);

    await tester.tap(find.text('창세기 1:1 삽입'));
    await tester.pumpAndSettle();

    // 에디터 본문은 Quill이라 find.text로 못 잡으므로 저장 형식(마커)으로 확인한다.
    expect(_bodyMarkers(tester), contains('창세기 1:1 더미 구절 본문 1'));
    expect(_bodyMarkers(tester), isNot(contains('@')));
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

    _enterBody(tester, '@창');
    await tester.pumpAndSettle();

    await tester.tap(find.text('창세기'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('qt-note-mention-chapter-picker')),
        findsOneWidget);
    expect(find.byKey(const Key('qt-note-mention-verse-from-picker')),
        findsOneWidget);
    expect(find.byKey(const Key('qt-note-mention-verse-to-picker')),
        findsOneWidget);
    expect(find.text('문구 삽입'), findsOneWidget);

    await tester.tap(find.text('문구 삽입'));
    await tester.pumpAndSettle();

    expect(_bodyMarkers(tester), contains('창세기 1:1 더미 구절 본문 1'));
    expect(_bodyMarkers(tester), isNot(contains('@')));
  });

  testWidgets('글씨 크기를 선택 텍스트에 적용하면 [fs] 마커로 저장된다', (tester) async {
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

    _enterBody(tester, '본문 🙂');
    _selectBody(tester, 0, 2); // '본문' 선택
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

    // 선택한 '본문'에 크기가 적용되어 저장 형식(마커)에 [fs=24]가 남는다.
    expect(_bodyMarkers(tester), contains('[fs=24]'));
    expect(_bodyMarkers(tester), contains('본문'));
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

    _enterBody(tester, '강조');
    _selectBody(tester, 0, 2); // '강조' 선택

    await tester.tap(find.byTooltip('배경 색상'));
    await tester.pumpAndSettle();
    await tester.tap(
      find.byKey(
          ValueKey('note-color-swatch-${const Color(0xFFFEF08A).toARGB32()}')),
    );
    await tester.pumpAndSettle();

    // 선택한 '강조'에 배경색이 적용되어 저장 형식에 [bg=...] 마커로 감싸진다.
    final markers = _bodyMarkers(tester);
    expect(markers, contains('[bg=#FEF08A]'));
    expect(markers, contains('강조[bg=]'));
  });

  testWidgets('굵게 버튼은 최근 선택 범위를 잃지 않고 선택 텍스트에 적용된다', (tester) async {
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

    _enterBody(tester, '강조');
    _selectBody(tester, 0, 2); // '강조' 선택
    await tester.pump();

    await tester.tap(find.byTooltip('굵게'));
    await tester.pump();

    // 선택한 '강조'가 ** 마커로 감싸져 저장된다.
    expect(_bodyMarkers(tester), '**강조**');
  });

  testWidgets('본문 입력창은 flutter_quill 에디터로 placeholder를 노출한다', (tester) async {
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

    // 새 본문 입력은 flutter_quill 에디터이며 placeholder('노트 작성')를 노출한다.
    final editor = tester.widget<QuillEditor>(
      find.byKey(const ValueKey('qt-note-body-input')),
    );
    expect(editor.config.placeholder, '노트 작성');
  });
  testWidgets('목록 버튼은 현재 줄 앞에 접두어를 넣는다', (tester) async {
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

    _enterBody(tester, '내용');
    await tester.pump();

    await tester.tap(find.byTooltip('(1) 목록'));
    await tester.pump();
    // 번호 접두어가 현재 줄 맨 앞에 들어간다.
    expect(_bodyMarkers(tester).trim(), '(1) 내용');

    await tester.tap(find.byTooltip('동그라미 목록'));
    await tester.pump();
    // 글머리표 접두어도 줄 앞에 추가된다.
    expect(_bodyMarkers(tester), contains('• '));
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

    _enterBody(tester, '@Ge');
    await tester.pumpAndSettle();
    await tester.tap(find.text('Genesis').first);
    await tester.pumpAndSettle();

    final insertButtonFinder = find.widgetWithText(FilledButton, '문구 삽입');
    final insertButton = tester.widget<FilledButton>(insertButtonFinder);

    expect(find.byKey(const Key('qt-note-mention-chapter-picker')),
        findsOneWidget);
    expect(insertButton.onPressed, isNull);
  });

  testWidgets('draft button sends DRAFT QT note payload', (tester) async {
    final repository = _FakeNoteRepository();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          noteRepositoryProvider.overrideWithValue(repository),
        ],
        child: const MaterialApp(home: QtNoteEditorScreen(args: _saveArgs)),
      ),
    );

    await tester.enterText(find.byType(TextField).first, 'draft title');
    _enterBody(tester, 'draft body');
    await tester.pump();
    await tester.tap(find.widgetWithText(OutlinedButton, '임시저장'));
    await tester.pump();

    expect(repository.createQtNoteCalls, hasLength(1));
    final call = repository.createQtNoteCalls.single;
    expect(call.qtPassageId, 77);
    expect(call.title, 'draft title');
    expect(call.body, 'draft body');
    expect(call.status, 'DRAFT');
    expect(call.verseIds, [7001, 7002]);
  });

  testWidgets('save button sends SAVED QT note payload', (tester) async {
    final repository = _FakeNoteRepository();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          noteRepositoryProvider.overrideWithValue(repository),
        ],
        child: const MaterialApp(home: QtNoteEditorScreen(args: _saveArgs)),
      ),
    );

    await tester.enterText(find.byType(TextField).first, 'saved title');
    _enterBody(tester, 'saved body');
    await tester.pump();
    await tester.tap(find.widgetWithText(FilledButton, '저장'));
    await tester.pump();

    expect(repository.createQtNoteCalls, hasLength(1));
    final call = repository.createQtNoteCalls.single;
    expect(call.qtPassageId, 77);
    expect(call.title, 'saved title');
    expect(call.body, 'saved body');
    expect(call.status, 'SAVED');
    expect(call.verseIds, [7001, 7002]);
  });
}

const _saveArgs = QtNoteEditorArgs(
  passage: TodayQtPassage(
    qtPassageId: 77,
    passageDate: '2026-06-12',
    title: 'save test passage',
    reference: BibleReference(
      koreanBookName: 'Genesis',
      englishBookName: 'Genesis',
      chapter: 1,
      verseFrom: 1,
      verseTo: 2,
    ),
    book: BibleVerseBook(
      code: 'GEN',
      koreanName: 'Genesis',
      englishName: 'Genesis',
      chapter: 1,
    ),
    verses: [
      BibleVerse(
        id: 7001,
        bookCode: 'GEN',
        chapterNo: 1,
        verseNo: 1,
        koreanText: 'body 1',
        englishText: 'body 1',
      ),
      BibleVerse(
        id: 7002,
        bookCode: 'GEN',
        chapterNo: 1,
        verseNo: 2,
        koreanText: 'body 2',
        englishText: 'body 2',
      ),
    ],
  ),
);

class _CapturedCreateQtNote {
  final int? qtPassageId;
  final String title;
  final String body;
  final List<int> verseIds;
  final String status;

  const _CapturedCreateQtNote({
    required this.qtPassageId,
    required this.title,
    required this.body,
    required this.verseIds,
    required this.status,
  });
}

class _FakeNoteRepository extends NoteRepository {
  final createQtNoteCalls = <_CapturedCreateQtNote>[];

  _FakeNoteRepository() : super(Dio());

  @override
  Future<void> createQtNote({
    required int? qtPassageId,
    required String title,
    required String body,
    required List<int> verseIds,
    String status = 'SAVED',
  }) async {
    createQtNoteCalls.add(
      _CapturedCreateQtNote(
        qtPassageId: qtPassageId,
        title: title,
        body: body,
        verseIds: List<int>.of(verseIds),
        status: status,
      ),
    );
  }
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
