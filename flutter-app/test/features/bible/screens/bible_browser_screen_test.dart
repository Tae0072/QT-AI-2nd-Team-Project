import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/screens/bible_browser_screen.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/note/screens/note_list_screen.dart';
import 'package:qtai_app/features/note/services/note_repository.dart';
import 'package:qtai_app/routes/app_router.dart';

void main() {
  testWidgets('BibleBrowserScreen renders selected passage after search',
      (tester) async {
    final repository = _FakeBibleRepository();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider.overrideWithValue(repository),
        ],
        child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            home: const BibleBrowserScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('성경본문'), findsOneWidget);
    expect(find.text('창세기'), findsWidgets);
    expect(find.byKey(const Key('bible-book-picker')), findsOneWidget);
    expect(find.byKey(const Key('bible-chapter-picker')), findsOneWidget);
    expect(find.byKey(const Key('bible-verse-from-picker')), findsOneWidget);
    expect(find.byKey(const Key('bible-verse-to-picker')), findsOneWidget);
    expect(find.byKey(const Key('bible-chapter-input')), findsNothing);

    await tester.tap(find.text('조회'));
    await tester.pumpAndSettle();

    expect(repository.requestedBookCode, 'GEN');
    expect(repository.requestedChapter, 1);
    expect(repository.requestedVerseFrom, 1);
    expect(repository.requestedVerseTo, 1);
    expect(find.text('1:1'), findsOneWidget);
    expect(find.text('테스트 본문 1'), findsOneWidget);
    expect(find.text('Test English body 1'), findsNothing);

    await tester.tap(find.byKey(const Key('bible-browser-english-toggle')));
    await tester.pumpAndSettle();

    expect(find.text('Genesis'), findsOneWidget);
    expect(find.text('Test English body 1'), findsOneWidget);
  });

  testWidgets('sermon note button opens the note sermon entry point',
      (tester) async {
    final bibleRepository = _FakeBibleRepository();
    final noteRepository = _FakeNoteRepository();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bibleRepositoryProvider.overrideWithValue(bibleRepository),
          noteRepositoryProvider.overrideWithValue(noteRepository),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          onGenerateRoute: AppRouter.onGenerateRoute,
          home: const BibleBrowserScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byType(FilledButton).first);
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('bible-browser-sermon-note-button')));
    await tester.pumpAndSettle();

    expect(find.byType(NoteListScreen), findsOneWidget);
    expect(noteRepository.requestedCategory, 'SERMON');
  });

  testWidgets('sermon note button delegates to the home note tab callback',
      (tester) async {
    final bibleRepository = _FakeBibleRepository();
    final container = ProviderContainer(
      overrides: [
        bibleRepositoryProvider.overrideWithValue(bibleRepository),
      ],
    );
    addTearDown(container.dispose);

    var openedNoteTab = false;

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: BibleBrowserScreen(
            onOpenSermonNotes: () => openedNoteTab = true,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byType(FilledButton).first);
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('bible-browser-sermon-note-button')));
    await tester.pumpAndSettle();

    expect(openedNoteTab, isTrue);
    expect(container.read(noteCategoryFilterProvider), 'SERMON');
  });
}

class _FakeBibleRepository extends BibleRepository {
  String? requestedBookCode;
  int? requestedChapter;
  int? requestedVerseFrom;
  int? requestedVerseTo;

  _FakeBibleRepository() : super(Dio());

  @override
  Future<List<BibleBook>> getBooks() async {
    return const [
      BibleBook(
        id: 46,
        testament: 'OLD',
        code: 'GEN',
        koreanName: '창세기',
        englishName: 'Genesis',
        displayOrder: 1,
      ),
    ];
  }

  @override
  Future<BibleVerseRange> getChapterVerses({
    required String bookCode,
    required int chapter,
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
          id: 1001,
          bookCode: 'GEN',
          chapterNo: 1,
          verseNo: 1,
          koreanText: '테스트 본문 1',
          englishText: 'Test English body 1',
        ),
        BibleVerse(
          id: 1002,
          bookCode: 'GEN',
          chapterNo: 1,
          verseNo: 2,
          koreanText: '테스트 본문 2',
          englishText: 'Test English body 2',
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
    requestedBookCode = bookCode;
    requestedChapter = chapter;
    requestedVerseFrom = verseFrom;
    requestedVerseTo = verseTo;
    return const BibleVerseRange(
      book: BibleVerseBook(
        code: 'GEN',
        koreanName: '창세기',
        englishName: 'Genesis',
        chapter: 1,
      ),
      verses: [
        BibleVerse(
          id: 1001,
          bookCode: 'GEN',
          chapterNo: 1,
          verseNo: 1,
          koreanText: '테스트 본문 1',
          englishText: 'Test English body 1',
        ),
      ],
    );
  }
}

class _FakeNoteRepository extends NoteRepository {
  String? requestedCategory;

  _FakeNoteRepository() : super(Dio());

  @override
  Future<NoteListResponse> getNotes({
    String? category,
    int page = 0,
  }) async {
    requestedCategory = category;
    return NoteListResponse(items: const [], hasNext: false);
  }
}
