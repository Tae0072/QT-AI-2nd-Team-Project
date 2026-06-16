import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/core/config/app_config.dart';
import 'package:qtai_app/features/auth/providers/auth_providers.dart';
import 'package:qtai_app/features/note/models/note_markup_quill_codec.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
import 'package:qtai_app/features/bible/models/bible_models.dart';
import 'package:qtai_app/features/bible/models/bible_reference.dart';
import 'package:qtai_app/features/bible/providers/bible_providers.dart';
import 'package:qtai_app/features/bible/screens/today_qt_screen.dart';
import 'package:qtai_app/features/bible/services/bible_repository.dart';
import 'package:qtai_app/features/home/providers/home_providers.dart';
import 'package:qtai_app/features/mypage/models/dashboard_response.dart';
import 'package:qtai_app/features/mypage/providers/mypage_providers.dart';
import 'package:qtai_app/features/music/providers/music_providers.dart';
import 'package:qtai_app/features/note/models/note_models.dart';
import 'package:qtai_app/features/note/providers/note_providers.dart';
import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart';
import 'package:qtai_app/features/sharing/models/sharing_post_response.dart';
import 'package:qtai_app/features/sharing/providers/sharing_providers.dart';
import 'package:qtai_app/features/sharing/services/sharing_repository.dart';
import 'package:qtai_app/main.dart';
import 'package:qtai_app/features/tts/widgets/qt_tts_button.dart';
import 'package:qtai_app/routes/app_router.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  testWidgets('오늘 QT 화면은 기본 한글 본문만 보이고 영어는 선택 시 표시한다', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    const passage = TodayQtPassage(
      reference: BibleReference(
        koreanBookName: '고린도전서',
        englishBookName: '1 Corinthians',
        chapter: 1,
        verseFrom: 10,
        verseTo: 17,
      ),
      book: BibleVerseBook(
        code: '1CO',
        koreanName: '고린도전서',
        englishName: '1 Corinthians',
        chapter: 1,
      ),
      verses: [
        BibleVerse(
          id: 1010,
          bookCode: '1CO',
          chapterNo: 1,
          verseNo: 10,
          koreanText: '더미 한글 본문 10',
          englishText: 'Dummy English verse 10',
        ),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          todayQtPassageProvider.overrideWith((ref) async => passage),
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
        child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            home: const TodayQtScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('오늘 QT'), findsOneWidget);
    expect(find.text('고린도전서 1:10-17'), findsOneWidget);
    expect(find.text('더미 한글 본문 10'), findsOneWidget);
    expect(find.text('1 Corinthians'), findsNothing);
    expect(find.text('Dummy English verse 10'), findsNothing);

    await tester.tap(find.byKey(const Key('today-qt-english-toggle')));
    await tester.pumpAndSettle();

    expect(find.text('1 Corinthians'), findsOneWidget);
    expect(find.text('Dummy English verse 10'), findsOneWidget);
    expect(find.byType(QtTtsButton), findsOneWidget);
  });

  testWidgets('노트 버튼을 누르면 오늘 QT 본문을 가진 노트 작성 화면으로 이동한다', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    const passage = TodayQtPassage(
      qtPassageId: 7,
      passageDate: '2026-06-05',
      title: '성령으로 깨닫는 하나님의 지혜',
      hasExplanation: true,
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
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          todayQtPassageProvider.overrideWith((ref) async => passage),
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          onGenerateRoute: AppRouter.onGenerateRoute,
          home: const TodayQtScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('노트'));
    await tester.pumpAndSettle();

    expect(find.text('QT 노트'), findsOneWidget);
    expect(find.text('고린도전서 2:1-16'), findsOneWidget);
    expect(find.text('더미 한글 본문 1'), findsOneWidget);
    expect(find.byTooltip('굵게'), findsOneWidget);
    expect(find.byTooltip('텍스트 색상'), findsOneWidget);
    expect(find.byTooltip('배경 색상'), findsOneWidget);
    expect(find.byTooltip('구절 삽입'), findsOneWidget);
  });

  testWidgets('QT 노트 첫 입력 중 앱 route 상태가 unknown으로 갱신되어도 노트 화면과 본문 입력을 유지한다',
      (tester) async {
    AppConfig.reset();
    AppConfig.initializeForTest();
    addTearDown(AppConfig.reset);
    SharedPreferences.setMockInitialValues({'onboarding_complete': true});
    final prefs = await SharedPreferences.getInstance();
    final auth = _FakeAuthNotifier(AuthStatus.authenticated);

    const passage = TodayQtPassage(
      qtPassageId: 7,
      passageDate: '2026-06-05',
      title: '성령으로 깨닫는 하나님의 지혜',
      hasExplanation: true,
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
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
          authStatusProvider.overrideWith((_) => auth),
          musicControllerProvider.overrideWith(_FakeMusicController.new),
          todayQtPassageProvider.overrideWith((ref) async => passage),
          bibleRepositoryProvider.overrideWithValue(_FakeBibleRepository()),
          sharingRepositoryProvider.overrideWithValue(_FakeSharingRepository()),
          notesProvider.overrideWith((ref) async {
            return NoteListResponse(items: const [], hasNext: false);
          }),
          meditationCalendarProvider.overrideWith((ref, month) async {
            return MeditationCalendar(
              month: month,
              days: const [],
              summary: CalendarSummary(
                savedDays: 0,
                savedNoteCount: 0,
                meditationStreakDays: 0,
              ),
            );
          }),
          dashboardProvider.overrideWith((ref) async {
            return const DashboardResponse(
              profile: ProfileSummary(memberId: 1, nickname: '테스터'),
            );
          }),
          // 홈(랜딩) 최근 기록은 빈 목록(테스트에서 스피너가 무한 회전하지 않게).
          homeRecentNotesProvider.overrideWith((ref) async => const []),
        ],
        child: const QTAIApp(),
      ),
    );
    await tester.pumpAndSettle();

    // 앱은 홈(랜딩)에서 시작 → '묵상 시작하기'로 오늘 QT 본문 화면에 진입한다.
    await tester.ensureVisible(find.text('묵상 시작하기'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('묵상 시작하기'));
    await tester.pumpAndSettle();

    await tester
        .tap(find.widgetWithIcon(OutlinedButton, Icons.edit_note_outlined));
    await tester.pumpAndSettle();

    // 본문은 flutter_quill 에디터다(enterText 불가) → 컨트롤러로 직접 입력.
    final body = find.byKey(const ValueKey('qt-note-body-input'));
    final beforeQuill = tester.widget<QuillEditor>(body).controller;
    beforeQuill.replaceText(
      0,
      beforeQuill.document.length <= 1 ? 0 : beforeQuill.document.length - 1,
      '첫 글자',
      const TextSelection.collapsed(offset: 3),
    );
    await tester.pump();

    auth.setUnknown();
    await tester.pump();

    expect(find.text('QT 노트'), findsOneWidget);
    expect(find.text('오늘 QT'), findsNothing);
    expect(body, findsOneWidget);

    // route 상태가 바뀌어도 같은 컨트롤러·입력값이 유지된다.
    final afterQuill = tester.widget<QuillEditor>(body).controller;
    expect(afterQuill, same(beforeQuill));
    expect(
      NoteMarkupQuillCodec.deltaToMarkers(afterQuill.document.toDelta()).trim(),
      '첫 글자',
    );
  });

  testWidgets('해설 버튼을 누르면 준비된 해설이 없을 때 빈 상태를 표시한다', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    const passage = TodayQtPassage(
      qtPassageId: 7,
      passageDate: '2026-06-05',
      title: '성령으로 깨닫는 하나님의 지혜',
      hasExplanation: true,
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
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          todayQtPassageProvider.overrideWith((ref) async => passage),
          bibleRepositoryProvider.overrideWithValue(_FakeBibleRepository()),
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          onGenerateRoute: AppRouter.onGenerateRoute,
          home: const TodayQtScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('해설'));
    await tester.pumpAndSettle();

    expect(find.text('해설'), findsOneWidget);
    expect(find.text('아직 준비된 해설이 없습니다.'), findsOneWidget);
  });

  testWidgets('해설 화면은 절 번호(장:절)와 절 본문을 함께 보여주고 검수해설은 노출하지 않는다', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    const passage = TodayQtPassage(
      qtPassageId: 7,
      passageDate: '2026-06-05',
      title: '성령으로 깨닫는 하나님의 지혜',
      hasExplanation: true,
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
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          todayQtPassageProvider.overrideWith((ref) async => passage),
          bibleRepositoryProvider.overrideWithValue(
            _FakeBibleRepository(
              content: const QtStudyContent(
                summary: null,
                explanations: [
                  QtStudyExplanation(
                    verseId: 2001,
                    summary: '핵심 요약',
                    explanation: '해설 본문',
                    sourceLabel: '검수해설',
                    aiAssetId: 11,
                  ),
                ],
                glossaryTerms: [],
              ),
            ),
          ),
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          onGenerateRoute: AppRouter.onGenerateRoute,
          home: const TodayQtScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('해설'));
    await tester.pumpAndSettle();

    // 절 번호(장:절)와 그 절 본문을 함께 보여준다.
    expect(find.text('2:1'), findsOneWidget);
    expect(find.text('더미 한글 본문 1'), findsOneWidget);
    // 검증용 출처 라벨은 사용자에게 노출하지 않는다.
    expect(find.text('검수해설'), findsNothing);
    // 해설 본문은 보여준다.
    expect(find.text('해설 본문'), findsOneWidget);
  });

  testWidgets('QT 영상 버튼을 누르면 영상 섹션으로 스크롤한다', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    final passage = TodayQtPassage(
      qtPassageId: 7,
      passageDate: '2026-06-11',
      simulatorStatus: 'READY',
      reference: const BibleReference(
        koreanBookName: '창세기',
        englishBookName: 'Genesis',
        chapter: 1,
        verseFrom: 1,
        verseTo: 20,
      ),
      book: const BibleVerseBook(
        code: 'GEN',
        koreanName: '창세기',
        englishName: 'Genesis',
        chapter: 1,
      ),
      verses: [
        for (var i = 1; i <= 20; i++)
          BibleVerse(
            id: 3000 + i,
            bookCode: 'GEN',
            chapterNo: 1,
            verseNo: i,
            koreanText: '테스트 본문 $i',
            englishText: 'Test English verse $i',
          ),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          todayQtPassageProvider.overrideWith((ref) async => passage),
          qtVideoClipProvider(7).overrideWith((ref) async {
            return const QtVideoClip(
              clipId: null,
              qtPassageId: 7,
              status: 'DISABLED',
              title: null,
              videoUrl: null,
              sourceVideoId: null,
              startTimeSec: null,
              endTimeSec: null,
              compositionType: null,
              clipStatus: null,
            );
          }),
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: const TodayQtScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('QT 영상'), findsOneWidget);
    final qtVideoButtonFinder =
        find.widgetWithIcon(OutlinedButton, Icons.movie_outlined);
    final qtVideoButton = tester.widget<OutlinedButton>(qtVideoButtonFinder);
    expect(qtVideoButton.onPressed, isNotNull);

    final videoFinder = find.byKey(const Key('today-qt-video-section'));
    expect(videoFinder, findsNothing);

    await tester.tap(qtVideoButtonFinder);
    await tester.pumpAndSettle();

    expect(videoFinder, findsOneWidget);
  });

  testWidgets('QT 영상 버튼은 준비되지 않은 상태에서는 비활성화된다', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    final passage = TodayQtPassage(
      qtPassageId: 7,
      passageDate: '2026-06-11',
      simulatorStatus: 'MISSING',
      reference: const BibleReference(
        koreanBookName: '창세기',
        englishBookName: 'Genesis',
        chapter: 1,
        verseFrom: 1,
        verseTo: 20,
      ),
      book: const BibleVerseBook(
        code: 'GEN',
        koreanName: '창세기',
        englishName: 'Genesis',
        chapter: 1,
      ),
      verses: [
        for (var i = 1; i <= 20; i++)
          BibleVerse(
            id: 3000 + i,
            bookCode: 'GEN',
            chapterNo: 1,
            verseNo: i,
            koreanText: '테스트 본문 $i',
            englishText: 'Test English verse $i',
          ),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          todayQtPassageProvider.overrideWith((ref) async => passage),
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
          home: const TodayQtScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final qtVideoButtonFinder =
        find.widgetWithIcon(OutlinedButton, Icons.movie_outlined);
    final qtVideoButton = tester.widget<OutlinedButton>(qtVideoButtonFinder);

    expect(find.text('QT 영상'), findsOneWidget);
    expect(qtVideoButton.onPressed, isNull);
    expect(find.byKey(const Key('today-qt-video-section')), findsNothing);
  });
}

class _FakeBibleRepository extends BibleRepository {
  final QtStudyContent content;

  _FakeBibleRepository({
    this.content = const QtStudyContent(
      summary: null,
      explanations: [],
      glossaryTerms: [],
    ),
  }) : super(Dio());

  @override
  Future<QtStudyContent> getQtStudyContent(int qtPassageId) async {
    return content;
  }

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
    ];
  }

  @override
  Future<BibleVerseRange> getChapterVerses({
    required String bookCode,
    required int chapter,
  }) async {
    return BibleVerseRange(
      book: BibleVerseBook(
        code: bookCode,
        koreanName: '창세기',
        englishName: 'Genesis',
        chapter: chapter,
      ),
      verses: const [
        BibleVerse(
          id: 1,
          bookCode: 'GEN',
          chapterNo: 1,
          verseNo: 1,
          koreanText: '더미 한글 본문 1',
          englishText: 'Dummy English verse 1',
        ),
      ],
    );
  }
}

class _FakeSharingRepository extends SharingRepository {
  _FakeSharingRepository() : super(Dio());

  @override
  Future<SharingPostListResponse> getSharingPosts({
    String? category,
    String? query,
    int page = 0,
    int size = 10,
  }) async {
    return SharingPostListResponse(items: const [], hasNext: false);
  }
}

class _FakeAuthNotifier extends AuthStatusNotifier {
  _FakeAuthNotifier(super.status) : super.withInitial();

  void setUnknown() => state = AuthStatus.unknown;
}

class _FakeMusicController extends MusicController {
  _FakeMusicController(super.ref);

  @override
  Future<void> ensureInitialized() async {
    state = state.copyWith(initialized: true, enabled: false);
  }

  @override
  void notifyUserGesture() {}
}
