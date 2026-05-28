import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../features/mypage/screens/mypage_screen.dart';
import '../features/mypage/screens/profile_edit_screen.dart';
import '../features/onboarding/providers/onboarding_providers.dart';
import '../features/onboarding/screens/onboarding_screen.dart';

/// 앱 라우트 설정.
class AppRouter {
  static const String splash = '/';
  static const String login = '/login';
  static const String home = '/home';
  static const String onboarding = '/onboarding';
  static const String myPage = '/my-page';
  static const String profileEdit = '/my-page/profile';

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
        // TODO: 카카오 로그인 화면 연동 시 교체
        return MaterialPageRoute(
          builder: (_) => const Scaffold(body: Center(child: Text('로그인'))),
        );
      case home:
        return MaterialPageRoute(
          builder: (_) => const Scaffold(body: Center(child: Text('홈'))),
        );
      case myPage:
        return MaterialPageRoute(
          builder: (_) => const MyPageScreen(),
        );
      case profileEdit:
        return MaterialPageRoute(
          builder: (_) => const ProfileEditScreen(),
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
