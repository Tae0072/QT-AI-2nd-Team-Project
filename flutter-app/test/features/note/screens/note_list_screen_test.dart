import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/note/screens/note_edit_screen.dart';
import 'package:qtai_app/features/note/screens/note_list_screen.dart';
import 'package:qtai_app/features/note/services/note_repository.dart';
import 'package:qtai_app/routes/app_router.dart';

/// 노트 목록·달력에 필요한 두 API만 가짜로 응답하고, getNotes 인자를 기록한다.
class _FakeNoteRepository extends NoteRepository {
  _FakeNoteRepository() : super(Dio());

  String? lastCategory;
  String? lastStatus;
  String? lastQ;
  int getNotesCallCount = 0;

  @override
  Future<NoteListResponse> getNotes(
      {String? category, String? status, String? q, int page = 0}) async {
    getNotesCallCount++;
    lastCategory = category;
    lastStatus = status;
    lastQ = q;
    return NoteListResponse(items: const [], hasNext: false);
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

    testWidgets('전체 선택 시 작은 + FAB이고, 누르면 N-02(카테고리 선택)로 간다',
        (tester) async {
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

      // 카테고리 칩 '기도' 선택 → FAB이 알약으로 변신
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
}
