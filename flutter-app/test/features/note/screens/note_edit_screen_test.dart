import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/note/screens/note_edit_screen.dart';
import 'package:qtai_app/features/note/services/note_repository.dart';
import 'package:qtai_app/features/note/widgets/note_rich_text_editor.dart';

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

  Finder bodyField() => find.byWidgetPredicate(
        (w) => w is TextField && w.decoration?.labelText == '본문',
      );
  // 저장 버튼(FilledButton). 임시저장은 OutlinedButton이라 구분된다.
  Finder saveButton() => find.byType(FilledButton);

  testWidgets('리치 서식 툴바를 쓰고, 굵게가 선택을 ** 마커로 감싼다', (tester) async {
    await pump(tester);

    expect(find.byTooltip('글씨 크기'), findsOneWidget);
    expect(find.byTooltip('굵게'), findsOneWidget);
    expect(find.byTooltip('구절 삽입'), findsOneWidget);

    await tester.enterText(bodyField(), '강조');
    final controller = tester.widget<TextField>(bodyField()).controller!;
    controller.selection =
        const TextSelection(baseOffset: 0, extentOffset: 2);

    await tester.tap(find.byTooltip('굵게'));
    await tester.pump();

    expect(controller.text, '**강조**');
  });

  testWidgets('@Ge 입력으로 창세기 구절 멘션 추천이 뜬다', (tester) async {
    await pump(tester);

    await tester.enterText(bodyField(), '@Ge');
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

    final controller = tester.widget<TextField>(bodyField()).controller!;
    expect(controller, isA<NoteRichBodyController>());
    expect(controller.text, '**굵게** 설교 본문');
  });

  testWidgets('작성 진입 인자(verseIds)를 저장 시 그대로 보낸다 (설교 ②)',
      (tester) async {
    final repo = _FakeNoteRepository();
    await pump(
      tester,
      args: const NoteEditArgs(category: 'SERMON', verseIds: [101, 102]),
      noteRepository: repo,
    );

    await tester.enterText(bodyField(), '설교 메모');
    await tester.tap(saveButton());
    await tester.pumpAndSettle();

    expect(repo.createdCategory, 'SERMON');
    expect(repo.createdVerseIds, [101, 102]);
  });

  testWidgets('@멘션으로 삽입한 절의 verseId가 저장된다 (note_verses)',
      (tester) async {
    final repo = _FakeNoteRepository();
    await pump(tester, noteRepository: repo);

    await tester.enterText(bodyField(), '@창 1:1');
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
  });
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

  @override
  Future<void> update(
    int noteId, {
    required String title,
    required String body,
    List<int>? verseIds,
    String status = 'SAVED',
    String visibility = 'PRIVATE',
  }) async {
    updatedNoteId = noteId;
    updatedVerseIds = verseIds;
  }
}
