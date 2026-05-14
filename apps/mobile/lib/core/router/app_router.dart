import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/ai_session/ai_chat_screen.dart';
import '../../features/auth/login_screen.dart';
import '../../features/bible_reader/bible_reader_screen.dart';
import '../../features/journal/journal_editor_screen.dart';
import '../../features/journal/journal_list_screen.dart';
import '../../features/today_qt/today_qt_screen.dart';

/// 5 화면 라우팅 골격 (08_프론트엔드_Flutter_가이드 § 10).
///
/// - `/today`        — 오늘 QT (첫 화면)
/// - `/bible/:b/:c/:v` — 일반 성경 보기 (읽기 전용)
/// - `/journal`      — 묵상 노트 목록
/// - `/journal/:id`  — 묵상 노트 편집 (4필드 자동 저장)
/// - `/ai/:sessionId` — AI 대화 화면
/// - `/login`        — Google OAuth (선택)
final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/today',
    routes: [
      GoRoute(path: '/today', builder: (_, __) => const TodayQtScreen()),
      GoRoute(
        path: '/bible/:bookCode/:chapter/:verse',
        builder: (_, state) => BibleReaderScreen(
          bookCode: state.pathParameters['bookCode']!,
          chapter: int.parse(state.pathParameters['chapter']!),
          verse: int.parse(state.pathParameters['verse']!),
        ),
      ),
      GoRoute(path: '/journal', builder: (_, __) => const JournalListScreen()),
      GoRoute(
        path: '/journal/:id',
        builder: (_, state) =>
            JournalEditorScreen(journalId: int.parse(state.pathParameters['id']!)),
      ),
      GoRoute(
        path: '/ai/:sessionId',
        builder: (_, state) =>
            AiChatScreen(sessionId: int.parse(state.pathParameters['sessionId']!)),
      ),
      GoRoute(path: '/login', builder: (_, __) => const LoginScreen()),
    ],
    // TODO(김지민): redirect 가드 — 인증 필요 라우트(/journal, /ai)는 토큰 없으면 /login.
  );
});
