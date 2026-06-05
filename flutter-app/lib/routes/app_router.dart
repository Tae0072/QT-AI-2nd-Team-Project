import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../features/auth/screens/login_screen.dart';
import '../features/home/screens/home_screen.dart';
import '../features/auth/screens/nickname_setup_screen.dart';
import '../features/mypage/screens/mypage_screen.dart';
import '../features/mypage/screens/notification_list_screen.dart';
import '../features/mypage/screens/praise_screen.dart';
import '../features/mypage/screens/profile_edit_screen.dart';
import '../features/mypage/screens/settings_screen.dart';
import '../features/mypage/screens/tts_settings_screen.dart';
import '../features/note/screens/note_category_select_screen.dart';
import '../features/note/screens/note_detail_screen.dart';
import '../features/note/screens/note_edit_screen.dart';
import '../features/note/screens/note_list_screen.dart';
import '../features/sharing/screens/my_sharing_screen.dart';
import '../features/sharing/screens/sharing_detail_screen.dart';
import '../features/sharing/screens/sharing_feed_screen.dart';
import '../features/onboarding/providers/onboarding_providers.dart';
import '../features/onboarding/screens/onboarding_screen.dart';

/// 앱 라우트 설정.
class AppRouter {
  static const String splash = '/';
  static const String login = '/login';
  static const String home = '/home';
  static const String onboarding = '/onboarding';
  static const String nicknameSetup = '/nickname-setup';
  static const String myPage = '/my-page';
  static const String profileEdit = '/my-page/profile';
  static const String notifications = '/notifications';
  static const String appSettings = '/settings';
  static const String ttsSettings = '/settings/tts';
  static const String praise = '/praise';
  static const String sharing = '/sharing';
  static const String sharingDetail = '/sharing/detail';
  static const String mySharing = '/my-sharing';
  static const String noteList = '/notes';
  static const String noteCategorySelect = '/notes/category-select';
  static const String noteEdit = '/notes/edit';
  static const String noteDetail = '/notes/detail';

  static Route<dynamic> onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case onboarding:
        return MaterialPageRoute(
          builder: (context) => Consumer(
            builder: (context, ref, _) => OnboardingScreen(
              onComplete: () async {
                await ref.read(onboardingCompleteProvider.notifier).complete();
                if (context.mounted) {
                  Navigator.of(context).pushReplacementNamed(login);
                }
              },
            ),
          ),
        );
      case login:
        return MaterialPageRoute(
          builder: (_) => const LoginScreen(),
        );
      case nicknameSetup:
        return MaterialPageRoute(
          builder: (_) => const NicknameSetupScreen(),
        );
      case home:
        return MaterialPageRoute(
          builder: (_) => const HomeScreen(),
        );
      case myPage:
        return MaterialPageRoute(
          builder: (_) => const MyPageScreen(),
        );
      case profileEdit:
        return MaterialPageRoute(
          builder: (_) => const ProfileEditScreen(),
        );
      case notifications:
        return MaterialPageRoute(
          builder: (_) => const NotificationListScreen(),
        );
      case appSettings:
        return MaterialPageRoute(
          builder: (_) => const SettingsScreen(),
        );
      case ttsSettings:
        return MaterialPageRoute(
          builder: (_) => const TtsSettingsScreen(),
        );
      case praise:
        return MaterialPageRoute(
          builder: (_) => const PraiseScreen(),
        );
      case sharing:
        return MaterialPageRoute(
          builder: (_) => const SharingFeedScreen(),
        );
      case mySharing:
        return MaterialPageRoute(
          builder: (_) => const MySharingScreen(),
        );
      case sharingDetail:
        final postId = settings.arguments as int;
        return MaterialPageRoute(
          builder: (_) => SharingDetailScreen(postId: postId),
        );
      case noteList:
        return MaterialPageRoute(
          builder: (_) => const NoteListScreen(),
        );
      case noteCategorySelect:
        return MaterialPageRoute(
          builder: (_) => const NoteCategorySelectScreen(),
        );
      case noteEdit:
        // N-02(작성)·N-04(수정)에서 넘긴 NoteEditArgs를 N-03이 직접 꺼내 쓴다.
        return MaterialPageRoute(
          settings: settings,
          builder: (_) => const NoteEditScreen(),
        );
      case noteDetail:
        // N-01 목록에서 넘긴 noteId(int)로 상세 화면을 연다.
        final noteId = settings.arguments as int;
        return MaterialPageRoute(
          builder: (_) => NoteDetailScreen(noteId: noteId),
        );
      default:
        return MaterialPageRoute(
          builder: (_) => Scaffold(
            body: Center(child: Text('Route not found: ${settings.name}')),
          ),
        );
    }
  }
}
