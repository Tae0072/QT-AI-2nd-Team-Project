import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../features/auth/screens/login_screen.dart';
import '../features/auth/screens/nickname_setup_screen.dart';
import '../features/home/screens/home_screen.dart';
import '../features/mypage/screens/mypage_screen.dart';
import '../features/mypage/screens/notification_list_screen.dart';
import '../features/mypage/screens/praise_screen.dart';
import '../features/mypage/screens/profile_edit_screen.dart';
import '../features/mypage/screens/settings_screen.dart';
import '../features/mypage/screens/tts_settings_screen.dart';
import '../features/music/screens/music_settings_screen.dart';
import '../features/note/screens/note_category_select_screen.dart';
import '../features/note/screens/note_detail_screen.dart';
import '../features/note/screens/note_edit_screen.dart';
import '../features/note/screens/note_list_screen.dart';
import '../features/note/screens/qt_note_editor_screen.dart';
import '../features/onboarding/providers/onboarding_providers.dart';
import '../features/onboarding/screens/onboarding_screen.dart';
import '../features/dev/dev_mode_screen.dart'; // [DEV_MODE]
import '../features/sharing/screens/my_sharing_screen.dart';
import '../features/sharing/screens/sharing_bookmarks_screen.dart';
import '../features/sharing/screens/sharing_detail_screen.dart';
import '../features/sharing/screens/sharing_feed_screen.dart';
import '../features/sharing/screens/sharing_mentions_screen.dart';
import '../features/study/screens/qt_study_content_screen.dart';

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
  static const String musicSettings = '/settings/music';
  static const String praise = '/praise';
  static const String sharing = '/sharing';
  static const String sharingDetail = '/sharing/detail';
  static const String sharingBookmarks = '/sharing/bookmarks';
  static const String sharingMentions = '/sharing/mentions';
  static const String mySharing = '/my-sharing';
  static const String noteList = '/notes';
  static const String noteCategorySelect = '/notes/category-select';
  static const String noteEdit = '/notes/edit';
  static const String noteDetail = '/notes/detail';
  static const String qtNoteEditor = '/notes/qt-editor';
  static const String qtStudyContent = '/qt/study-content';
  static const String devMode = '/dev-mode'; // [DEV_MODE]

  static Route<dynamic> onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case onboarding:
        return MaterialPageRoute(
          builder: (context) => Consumer(
            builder: (context, ref, _) => OnboardingScreen(
              onComplete: () async {
                await ref.read(onboardingCompleteProvider.notifier).complete();
                if (context.mounted) {
                  unawaited(Navigator.of(context).pushReplacementNamed(login));
                }
              },
            ),
          ),
        );
      case login:
        return MaterialPageRoute(
          builder: (_) => const LoginScreen(),
        );
      // [DEV_MODE] 개발자 모드 화면
      case devMode:
        return MaterialPageRoute(
          builder: (_) => const DevModeScreen(),
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
      case musicSettings:
        return MaterialPageRoute(
          builder: (_) => const MusicSettingsScreen(),
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
      case sharingBookmarks:
        return MaterialPageRoute(
          builder: (_) => const SharingBookmarksScreen(),
        );
      case sharingMentions:
        return MaterialPageRoute(
          builder: (_) => const SharingMentionsScreen(),
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
        return MaterialPageRoute(
          settings: settings,
          builder: (_) => const NoteEditScreen(),
        );
      case noteDetail:
        final noteId = settings.arguments as int;
        return MaterialPageRoute(
          builder: (_) => NoteDetailScreen(noteId: noteId),
        );
      case qtNoteEditor:
        final args = settings.arguments as QtNoteEditorArgs;
        return MaterialPageRoute(
          builder: (_) => QtNoteEditorScreen(args: args),
        );
      case qtStudyContent:
        final args = settings.arguments as QtStudyContentArgs;
        return MaterialPageRoute(
          builder: (_) => QtStudyContentScreen(args: args),
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
