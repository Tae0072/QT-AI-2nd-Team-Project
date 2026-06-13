import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/note/screens/note_list_screen.dart';
import 'package:qtai_app/features/note/services/note_repository.dart';
import 'package:qtai_app/features/note/widgets/note_card.dart';
import 'package:qtai_app/routes/app_router.dart';

NoteListItem _item(int id, String category) => NoteListItem(
      id: id,
      category: category,
      title: '노트 $id',
      status: 'SAVED',
      visibility: 'PRIVATE',
      shared: false,
    );

/// 노트 목록·달력에 필요한 API만 가짜로 응답하고, getNotes 인자·deleteMany 호출을 기록한다.
class _FakeNoteRepository extends NoteRepository {
  _FakeNoteRepository({this.items = const [], this.deleteFailures = const []})
      : super(Dio());

  final List<NoteListItem> items;
  final List<int> deleteFailures; // deleteMany가 실패로 반환할 id들
  String? lastCategory;
  String? lastStatus;
  String? lastQ;
  int getNotesCallCount = 0;
  List<int>? deletedIds;
  int deleteManyCallCount = 0;

  @override
  Future<NoteListResponse> getNotes(
      {String? category, String? status, String? q, int page = 0}) async {
    getNotesCallCount++;
    lastCategory = category;
    lastStatus = status;
    lastQ = q;
    return NoteListResponse(items: items, hasNext: false);
  }

  @override
  Future<List<int>> deleteMany(Iterable<int> noteIds) async {
    deleteManyCallCount++;
    deletedIds = noteIds.toList();
    return deleteFailures;
  }

  @override
  Future<MeditationCalendar> getMeditationCalendar(String month) async {
    return MeditationCalendar(
      month: month,
      days: const [],
      summary: CalendarSummary(
          savedDays: 0, savedNoteCount: 0, meditationStreakDays: 0),
    );
  }
}

void main() {
  group('notesProvider — 카테고리·상태 필터 재조회', () {
    test('상태 필터를 바꾸면 getNotes(status:)로 다시 조회한다', () async {
      final fake = _FakeNoteRepository();
      final container = ProviderContainer(
        overrides: [noteRepositoryProvider.overrideWithValue(fake)],
      );
      addTearDown(container.dispose);

      // 초기: 전체(필터 없음)
      await container.read(notesProvider.future);
      expect(fake.lastStatus, isNull);
      expect(fake.lastCategory, isNull);

      // 상태 = 임시저장(DRAFT)
      container.read(noteStatusFilterProvider.notifier).state = 'DRAFT';
      await container.read(notesProvider.future);
      expect(fake.lastStatus, 'DRAFT');
      expect(fake.lastCategory, isNull);

      // 카테고리도 함께 적용 → 둘 다 전달
      container.read(noteCategoryFilterProvider.notifier).state = 'PRAYER';
      await container.read(notesProvider.future);
      expect(fake.lastCategory, 'PRAYER');
      expect(fake.lastStatus, 'DRAFT');
    });

    test('나눔 필터는 status 없이 조회하고 공유한 노트만 남긴다', () async {
      final fake = _FakeNoteRepository(items: [
        NoteListItem(
          id: 1,
          category: 'PRAYER',
          title: '비공개',
          status: 'SAVED',
          visibility: 'PRIVATE',
          shared: false,
        ),
        NoteListItem(
          id: 2,
          category: 'GRATITUDE',
          title: '나눔공개',
          status: 'SAVED',
          visibility: 'PUBLIC',
          shared: true,
        ),
      ]);
      final container = ProviderContainer(
        overrides: [noteRepositoryProvider.overrideWithValue(fake)],
      );
      addTearDown(container.dispose);

      container.read(noteStatusFilterProvider.notifier).state =
          kNoteSharedFilter;
      final response = await container.read(notesProvider.future);

      // '나눔'은 서버 status로 안 보낸다(공유 여부로 거름).
      expect(fake.lastStatus, isNull);
      // 공유한 노트만 남는다.
      expect(response.items.map((e) => e.id), [2]);
    });

    test('검색어를 바꾸면 getNotes(q:)로 다시 조회한다', () async {
      final fake = _FakeNoteRepository();
      final container = ProviderContainer(
        overrides: [noteRepositoryProvider.overrideWithValue(fake)],
      );
      addTearDown(container.dispose);

      await container.read(notesProvider.future);
      expect(fake.lastQ, isNull);

      container.read(noteSearchQueryProvider.notifier).state = '은혜';
      await container.read(notesProvider.future);
      expect(fake.lastQ, '은혜');
    });
  });

  group('NoteListScreen — 맥락형 FAB', () {
    late String? pushedRoute;
    late Object? pushedArgs;

    Future<void> pump(WidgetTester tester) async {
      // 달력+칩+검색바가 모두 보이도록 실제 폰 크기 뷰포트로 키운다(기본 600px는 짧음).
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 3.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      pushedRoute = null;
      pushedArgs = null;
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            noteRepositoryProvider.overrideWithValue(_FakeNoteRepository()),
          ],
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            onGenerateRoute: (settings) {
              if (settings.name == '/') {
                return MaterialPageRoute(
                  builder: (_) => const NoteListScreen(),
                  settings: settings,
                );
              }
              // N-02/N-03 등 내비게이션은 실제 화면 대신 인자만 기록.
              pushedRoute = settings.name;
              pushedArgs = settings.arguments;
              return MaterialPageRoute(
                builder: (_) => const SizedBox.shrink(),
                settings: settings,
              );
            },
          ),
        ),
      );
      await tester.pumpAndSettle();
    }

    testWidgets('전체 선택 시 작은 + FAB이고, 누르면 N-02(카테고리 선택)로 간다', (tester) async {
      await pump(tester);

      // 작성 단축 알약 없음, 일반 FAB 존재
      expect(find.text('기도 작성'), findsNothing);
      expect(find.byType(FloatingActionButton), findsOneWidget);

      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();
      expect(pushedRoute, AppRouter.noteCategorySelect);
    });

    testWidgets('기도 칩 선택 시 "기도 작성" 알약이 뜨고, 누르면 N-03로 PRAYER 직행',
        (tester) async {
      await pump(tester);

      // 카테고리 칩 '기도' 선택 → FAB이 알약으로 변신.
      // 좁은 폭(360dp)에선 가로 칩 리스트에서 '기도'가 화면 밖일 수 있어 먼저 스크롤해 노출.
      await tester.dragUntilVisible(
        find.text('기도'),
        find.byKey(const ValueKey('note-category-chip-list')),
        const Offset(-80, 0),
      );
      await tester.tap(find.text('기도'));
      await tester.pumpAndSettle();

      expect(find.text('기도 작성'), findsOneWidget);
      // 변신 후에는 일반 FloatingActionButton이 아니라 커스텀 알약
      expect(find.byType(FloatingActionButton), findsNothing);

      await tester.tap(find.text('기도 작성'));
      await tester.pumpAndSettle();

      expect(pushedRoute, AppRouter.noteEdit);
      expect(pushedArgs, isA<NoteEditArgs>());
      expect((pushedArgs as NoteEditArgs).category, 'PRAYER');
    });
  });

  group('NoteListScreen — ② QT/설교 칩 FAB 숨김', () {
    Future<void> pump(WidgetTester tester) async {
      // 달력+칩+검색바가 모두 보이도록 실제 폰 크기 뷰포트로 키운다(기본 600px는 짧음).
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 3.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            noteRepositoryProvider.overrideWithValue(_FakeNoteRepository()),
          ],
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            home: const NoteListScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();
    }

    testWidgets('QT 칩 선택 시 작성 FAB이 사라진다(설교도 동일)', (tester) async {
      await pump(tester);
      // 전체: FAB 보임
      expect(find.byType(FloatingActionButton), findsOneWidget);

      await tester.tap(find.text('QT'));
      await tester.pumpAndSettle();
      expect(find.byType(FloatingActionButton), findsNothing);

      await tester.tap(find.text('설교'));
      await tester.pumpAndSettle();
      expect(find.byType(FloatingActionButton), findsNothing);
    });
  });

  group('NoteListScreen — ① 다중 선택 삭제', () {
    testWidgets('☰ 선택 → 체크 → 삭제 시 deleteMany가 호출된다', (tester) async {
      // 달력이 커서 기본 뷰포트(800x600)에선 카드가 화면 밖이라, 둘 다 보이게 키운다.
      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final repo = _FakeNoteRepository(
        items: [_item(1, 'PRAYER'), _item(2, 'GRATITUDE')],
      );
      await tester.pumpWidget(
        ProviderScope(
          overrides: [noteRepositoryProvider.overrideWithValue(repo)],
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            home: const NoteListScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // 선택 모드 진입(햄버거)
      await tester.tap(find.text('선택'));
      await tester.pumpAndSettle();
      expect(find.byIcon(Icons.radio_button_unchecked), findsNWidgets(2));

      // 첫 카드 선택
      await tester.tap(find.byType(NoteCard).first);
      await tester.pumpAndSettle();
      expect(find.byIcon(Icons.check_circle), findsOneWidget);

      // 하단 삭제바 → 확인 다이얼로그 → 삭제
      await tester.tap(find.text('삭제 (1)'));
      await tester.pumpAndSettle();
      expect(find.text('선택한 노트 삭제'), findsOneWidget);
      await tester.tap(find.text('삭제'));
      await tester.pumpAndSettle();

      expect(repo.deletedIds, [1]);
    });

    testWidgets('삭제 다이얼로그에서 취소하면 deleteMany를 호출하지 않는다', (tester) async {
      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final repo = _FakeNoteRepository(items: [_item(1, 'PRAYER')]);
      await tester.pumpWidget(
        ProviderScope(
          overrides: [noteRepositoryProvider.overrideWithValue(repo)],
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            home: const NoteListScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.text('선택'));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(NoteCard).first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('삭제 (1)'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('취소'));
      await tester.pumpAndSettle();

      expect(repo.deleteManyCallCount, 0);
      // 취소하면 선택 모드가 유지된다(삭제바 그대로).
      expect(find.text('삭제 (1)'), findsOneWidget);
    });

    testWidgets('부분 실패 시 실패 안내 스낵바를 보여준다', (tester) async {
      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final repo = _FakeNoteRepository(
        items: [_item(1, 'PRAYER')],
        deleteFailures: [1], // 1개 삭제 실패
      );
      await tester.pumpWidget(
        ProviderScope(
          overrides: [noteRepositoryProvider.overrideWithValue(repo)],
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            home: const NoteListScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.text('선택'));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(NoteCard).first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('삭제 (1)'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('삭제')); // 다이얼로그 확정
      await tester.pump(); // deleteMany 처리 + 다이얼로그 닫힘
      await tester.pump(const Duration(milliseconds: 300)); // 스낵바 렌더

      // 부분 성공도 정확히: 성공 0 + 실패 1.
      expect(find.text('0개 삭제, 1개 실패'), findsOneWidget);
    });

    testWidgets('전체선택 토글: 누르면 전부 선택, 다시 누르면 해제', (tester) async {
      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final container = ProviderContainer(
        overrides: [
          noteRepositoryProvider.overrideWithValue(
            _FakeNoteRepository(
                items: [_item(1, 'PRAYER'), _item(2, 'GRATITUDE')]),
          ),
        ],
      );
      addTearDown(container.dispose);
      await container.read(notesProvider.future); // 목록 채움

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            home: const NoteListScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.text('선택'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('전체선택'));
      await tester.pumpAndSettle();
      expect(container.read(noteSelectedIdsProvider), {1, 2});

      await tester.tap(find.text('전체선택'));
      await tester.pumpAndSettle();
      expect(container.read(noteSelectedIdsProvider), isEmpty);
    });

    testWidgets('선택 모드 중 카테고리 필터를 바꾸면 선택이 초기화된다(누수 방지)', (tester) async {
      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final container = ProviderContainer(
        overrides: [
          noteRepositoryProvider.overrideWithValue(
            _FakeNoteRepository(items: [_item(1, 'PRAYER')]),
          ),
        ],
      );
      addTearDown(container.dispose);

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            home: const NoteListScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.text('선택'));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(NoteCard).first);
      await tester.pumpAndSettle();
      expect(container.read(noteSelectionModeProvider), isTrue);
      expect(container.read(noteSelectedIdsProvider), isNotEmpty);

      // 카테고리 칩을 바꾸면 선택 모드·선택이 초기화된다.
      // (카드 배지에도 '기도'가 있어 칩을 콕 집는다.)
      await tester.tap(find.widgetWithText(FilterChip, '기도'));
      await tester.pumpAndSettle();
      expect(container.read(noteSelectionModeProvider), isFalse);
      expect(container.read(noteSelectedIdsProvider), isEmpty);
    });
  });

  group('NoteListScreen — ② 빈 상태 안내', () {
    testWidgets('QT 칩 + 빈 목록이면 작성 안내가 뜬다', (tester) async {
      // 달력+칩+검색바가 모두 보이도록 실제 폰 크기 뷰포트로 키운다(기본 600px는 짧음).
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 3.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            noteRepositoryProvider.overrideWithValue(_FakeNoteRepository()),
          ],
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            home: const NoteListScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.text('QT'));
      await tester.pumpAndSettle();
      expect(find.text('QT 노트는 오늘의 QT 화면에서 작성해요'), findsOneWidget);
    });
  });
}
