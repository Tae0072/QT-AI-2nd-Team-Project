import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';
import 'package:qtai_app/features/note/models/note_markup_quill_codec.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/note/screens/note_edit_screen.dart';
import 'package:qtai_app/features/note/services/note_repository.dart';

/// N-03이 QT와 공유하는 리치텍스트 에디터(서식·@멘션) + 인용 절(verseIds) 저장(QA ③⑨·⑪)을 검증한다.
void main() {
  // 저장 후 pop이 안전하도록 런처(home) 위에 NoteEditScreen을 push한다.
  Future<void> pump(
    WidgetTester tester, {
    NoteEditArgs args = const NoteEditArgs(category: 'PRAYER'),
    NoteRepository? noteRepository,
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider.overrideWithValue(_FakeBibleRepository()),
          if (noteRepository != null)
            noteRepositoryProvider.overrideWithValue(noteRepository),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: Builder(
            builder: (context) => Scaffold(
              body: ElevatedButton(
                onPressed: () => Navigator.of(context).push(
                  MaterialPageRoute(
                    settings: RouteSettings(arguments: args),
                    builder: (_) => const NoteEditScreen(),
                  ),
                ),
                child: const Text('open'),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
  }

  // 저장 버튼(FilledButton). 임시저장은 OutlinedButton이라 구분된다.
  Finder saveButton() => find.byType(FilledButton);

  testWidgets('리치 서식 툴바를 쓰고, 굵게가 선택을 ** 마커로 감싼다', (tester) async {
    await pump(tester);

    expect(find.byTooltip('글씨 크기'), findsOneWidget);
    expect(find.byTooltip('굵게'), findsOneWidget);
    expect(find.byTooltip('구절 삽입'), findsOneWidget);

    _enterBody(tester, '강조');
    _selectBody(tester, 0, 2); // '강조' 선택
    await tester.tap(find.byTooltip('굵게'));
    await tester.pump();

    expect(_bodyMarkers(tester), '**강조**');
  });

  testWidgets('@Ge 입력으로 창세기 구절 멘션 추천이 뜬다', (tester) async {
    await pump(tester);

    _enterBody(tester, '@Ge');
    await tester.pumpAndSettle();

    expect(find.text('창세기'), findsOneWidget);
    expect(find.text('출애굽기'), findsNothing);
  });

  testWidgets('편집 모드는 기존 리치 본문(마커 포함)을 그대로 불러와 프리뷰한다',
      (tester) async {
    await pump(
      tester,
      args: const NoteEditArgs(noteId: 7),
      noteRepository: _FakeNoteRepository(detail: _sermonDetail()),
    );

    // 기존 마커 본문이 저장 형식 그대로 에디터에 불러와진다.
    expect(_bodyMarkers(tester), '**굵게** 설교 본문');
  });

  testWidgets('작성 진입 인자(verseIds)를 저장 시 그대로 보낸다 (설교 ②)',
      (tester) async {
    final repo = _FakeNoteRepository();
    await pump(
      tester,
      args: const NoteEditArgs(category: 'SERMON', verseIds: [101, 102]),
      noteRepository: repo,
    );

    _enterBody(tester, '설교 메모');
    await tester.tap(saveButton());
    await tester.pumpAndSettle();

    expect(repo.createdCategory, 'SERMON');
    expect(repo.createdVerseIds, [101, 102]);
  });

  testWidgets('@멘션으로 삽입한 절의 verseId가 저장된다 (note_verses)',
      (tester) async {
    final repo = _FakeNoteRepository();
    await pump(tester, noteRepository: repo);

    _enterBody(tester, '@창 1:1');
    await tester.pumpAndSettle();
    await tester.tap(find.text('창세기 1:1 삽입'));
    await tester.pumpAndSettle();

    await tester.tap(saveButton());
    await tester.pumpAndSettle();

    expect(repo.createdVerseIds, contains(1001001));
  });

  testWidgets('편집 모드는 기존 인용 절을 seed해 수정 저장 시 보존한다',
      (tester) async {
    final repo = _FakeNoteRepository(detail: _sermonDetail());
    await pump(
      tester,
      args: const NoteEditArgs(noteId: 7),
      noteRepository: repo,
    );

    await tester.tap(saveButton());
    await tester.pumpAndSettle();

    expect(repo.updatedNoteId, 7);
    expect(repo.updatedVerseIds, [500]);
    // 수정 PATCH는 불러온 노트의 category를 그대로 다시 보낸다(서버 필수값).
    expect(repo.updatedCategory, 'SERMON');
  });

  testWidgets('성경 본문 진입 시 인용 미리보기(참조·본문)를 읽기 전용으로 보여준다',
      (tester) async {
    await pump(
      tester,
      args: const NoteEditArgs(
        category: 'SERMON',
        verseIds: [101],
        referenceText: '창세기 1:1-2',
        versePreview: '태초에 하나님이 천지를 창조하시니라',
      ),
    );

    expect(find.text('창세기 1:1-2'), findsOneWidget);
    expect(find.text('태초에 하나님이 천지를 창조하시니라'), findsOneWidget);
  });

  testWidgets('인용 정보가 없으면 미리보기 박스를 그리지 않는다', (tester) async {
    await pump(tester, args: const NoteEditArgs(category: 'PRAYER'));

    expect(find.byIcon(Icons.menu_book_outlined), findsNothing);
  });
}

// 본문은 flutter_quill 에디터(키 없음, 화면에 하나)다. enterText가 안 통하므로 컨트롤러를 직접 조작.
QuillController _bodyQuill(WidgetTester tester) =>
    tester.widget<QuillEditor>(find.byType(QuillEditor)).controller;

void _enterBody(WidgetTester tester, String text) {
  final controller = _bodyQuill(tester);
  final length = controller.document.length;
  controller.replaceText(
    0,
    length <= 1 ? 0 : length - 1,
    text,
    TextSelection.collapsed(offset: text.length),
  );
}

String _bodyMarkers(WidgetTester tester) =>
    NoteMarkupQuillCodec.deltaToMarkers(_bodyQuill(tester).document.toDelta());

void _selectBody(WidgetTester tester, int base, int extent) {
  _bodyQuill(tester).updateSelection(
    TextSelection(baseOffset: base, extentOffset: extent),
    ChangeSource.local,
  );
}

NoteDetail _sermonDetail() => NoteDetail(
      id: 7,
      category: 'SERMON',
      title: '주일 설교',
      body: '**굵게** 설교 본문',
      status: 'SAVED',
      visibility: 'PRIVATE',
      shared: false,
      verses: [
        NoteVerseRef(bibleVerseId: 500, bookCode: 'GEN', chapterNo: 1, verseNo: 1),
      ],
    );

class _FakeBibleRepository extends BibleRepository {
  _FakeBibleRepository() : super(Dio());

  @override
  Future<List<BibleBook>> getBooks() async => const [
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

  @override
  Future<BibleVerseRange> getVerses({
    required String bookCode,
    required int chapter,
    required int verseFrom,
    required int verseTo,
  }) async =>
      const BibleVerseRange(
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
            englishText: 'Dummy verse 1',
          ),
        ],
      );
}

/// getDetail/create/update를 기록·반환하는 가짜 노트 레포.
class _FakeNoteRepository extends NoteRepository {
  final NoteDetail? detail;

  _FakeNoteRepository({this.detail}) : super(Dio());

  String? createdCategory;
  List<int>? createdVerseIds;
  int? updatedNoteId;
  List<int>? updatedVerseIds;

  @override
  Future<NoteDetail> getDetail(int noteId) async => detail!;

  @override
  Future<NoteCreateResponse> create({
    required String category,
    required String title,
    required String body,
    List<int>? verseIds,
    String status = 'SAVED',
    String visibility = 'PRIVATE',
  }) async {
    createdCategory = category;
    createdVerseIds = verseIds;
    return NoteCreateResponse(
      id: 1,
      category: category,
      status: status,
      visibility: visibility,
    );
  }

  String? updatedCategory;
  int? updatedQtPassageId;

  @override
  Future<void> update(
    int noteId, {
    required String category,
    int? qtPassageId,
    required String title,
    required String body,
    List<int>? verseIds,
    String status = 'SAVED',
    String visibility = 'PRIVATE',
  }) async {
    updatedNoteId = noteId;
    updatedCategory = category;
    updatedQtPassageId = qtPassageId;
    updatedVerseIds = verseIds;
  }
}
