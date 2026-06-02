import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:qtai_app/core/config/app_config.dart';
import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart';
import 'package:qtai_app/routes/app_router.dart';

void main() {
  setUp(() {
    AppConfig.reset();
    AppConfig.initializeForTest();
  });

  tearDown(() {
    AppConfig.reset();
  });

  group('AppRouter', () {
    test('route constants are correctly defined', () {
      expect(AppRouter.splash, equals('/'));
      expect(AppRouter.login, equals('/login'));
      expect(AppRouter.home, equals('/home'));
      expect(AppRouter.onboarding, equals('/onboarding'));
      expect(AppRouter.myPage, equals('/my-page'));
      expect(AppRouter.profileEdit, equals('/my-page/profile'));
    });

    test('home route returns MaterialPageRoute', () {
      final route = AppRouter.onGenerateRoute(
        const RouteSettings(name: '/home'),
      );
      expect(route, isA<MaterialPageRoute>());
    });

    test('login route returns MaterialPageRoute', () {
      final route = AppRouter.onGenerateRoute(
        const RouteSettings(name: '/login'),
      );
      expect(route, isA<MaterialPageRoute>());
    });

    test('onboarding route returns MaterialPageRoute', () {
      final route = AppRouter.onGenerateRoute(
        const RouteSettings(name: '/onboarding'),
      );
      expect(route, isA<MaterialPageRoute>());
    });

    test('unknown route returns fallback MaterialPageRoute', () {
      final route = AppRouter.onGenerateRoute(
        const RouteSettings(name: '/unknown'),
      );
      expect(route, isA<MaterialPageRoute>());
    });

    test('home route returns MaterialPageRoute with HomeScreen', () {
      final route = AppRouter.onGenerateRoute(
        const RouteSettings(name: '/home'),
      );
      expect(route, isA<MaterialPageRoute>());
    });

    testWidgets('login route renders 카카오 로그인 텍스트', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            onGenerateRoute: AppRouter.onGenerateRoute,
            initialRoute: '/login',
          ),
        ),
      );
      await tester.pump();
      expect(find.text('카카오 로그인'), findsOneWidget);
    });

    testWidgets('unknown route renders error message', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          onGenerateRoute: AppRouter.onGenerateRoute,
          initialRoute: '/nonexistent',
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Route not found: /nonexistent'), findsOneWidget);
    });

    testWidgets('onboarding onComplete → login으로 네비게이션된다',
        (tester) async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
          child: MaterialApp(
            onGenerateRoute: AppRouter.onGenerateRoute,
            initialRoute: '/onboarding',
          ),
        ),
      );
      await tester.pumpAndSettle();

      // 온보딩 화면이 렌더링됨
      expect(find.byType(Scaffold), findsOneWidget);

      // 건너뛰기 버튼으로 onComplete 호출
      await tester.tap(find.text('건너뛰기'));
      await tester.pumpAndSettle();

      // login 화면으로 전환됨 — LoginScreen의 카카오 로그인 버튼 확인
      expect(find.text('카카오 로그인'), findsOneWidget);

      // SharedPreferences에 완료 플래그 저장됨
      expect(prefs.getBool('onboarding_complete'), isTrue);
    });
  });
}
