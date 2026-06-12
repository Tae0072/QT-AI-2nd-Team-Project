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

/// 자유 노트 N-03이 QT와 공유하는 리치텍스트 에디터(서식·@멘션·편집 로드)를 쓰는지 검증한다(QA ③⑨).
void main() {
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
          // N-03은 ModalRoute.arguments(NoteEditArgs)로 모드를 받는다.
          onGenerateRoute: (settings) => MaterialPageRoute(
            builder: (_) => const NoteEditScreen(),
            settings: RouteSettings(name: '/', arguments: args),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  Finder bodyField() => find.byWidgetPredicate(
        (w) => w is TextField && w.decoration?.labelText == '본문',
      );

  testWidgets('리치 서식 툴바를 쓰고, 굵게가 선택을 ** 마커로 감싼다', (tester) async {
    await pump(tester);

    // 옛 마크다운 툴바엔 없던 리치 기능(글씨 크기·구절 삽입)이 보인다 → 공유 에디터로 교체됨.
    expect(find.byTooltip('글씨 크기'), findsOneWidget);
    expect(find.byTooltip('굵게'), findsOneWidget);
    expect(find.byTooltip('텍스트 색상'), findsOneWidget);
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
      noteRepository: _FakeNoteRepository(
        detail: NoteDetail(
          id: 7,
          category: 'PRAYER',
          title: '감사 노트',
          body: '**굵게** [fs=24]큰글',
          status: 'SAVED',
          visibility: 'PRIVATE',
          shared: false,
          verses: const [],
        ),
      ),
    );

    final controller = tester.widget<TextField>(bodyField()).controller!;
    // 마커가 보존된 채로 로드되고, 리치 컨트롤러(라이브 프리뷰)로 채워진다.
    expect(controller, isA<NoteRichBodyController>());
    expect(controller.text, '**굵게** [fs=24]큰글');
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
}

class _FakeNoteRepository extends NoteRepository {
  final NoteDetail detail;

  _FakeNoteRepository({required this.detail}) : super(Dio());

  @override
  Future<NoteDetail> getDetail(int noteId) async => detail;
}
