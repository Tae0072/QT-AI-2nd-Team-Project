import 'package:flutter/material.dart';

import '../features/mypage/screens/mypage_screen.dart';
import '../features/mypage/screens/profile_edit_screen.dart';

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
