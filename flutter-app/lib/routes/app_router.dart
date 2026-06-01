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
  static const String praise = '/praise';
  static const String sharing = '/sharing';
  static const String sharingDetail = '/sharing/detail';

  static Route<dynamic> onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case onboarding:
        return MaterialPageRoute(
          builder: (context) => Consumer(
            builder: (context, ref, _) => OnboardingScreen(
              onComplete: () async {
                await ref
                    .read(onboardingCompleteProvider.notifier)
                    .complete();
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
      case praise:
        return MaterialPageRoute(
          builder: (_) => const PraiseScreen(),
        );
      case sharing:
        return MaterialPageRoute(
          builder: (_) => const SharingFeedScreen(),
        );
      case sharingDetail:
        final postId = settings.arguments as int;
        return MaterialPageRoute(
          builder: (_) => SharingDetailScreen(postId: postId),
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
