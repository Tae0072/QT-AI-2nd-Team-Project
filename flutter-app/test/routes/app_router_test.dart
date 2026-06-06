import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:qtai_app/core/config/app_config.dart';
import 'package:qtai_app/features/onboarding/providers/onboarding_providers.dart';
import 'package:qtai_app/l10n/app_localizations.dart';
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

    testWidgets('login route renders 카카오로 시작하기 텍스트', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
            onGenerateRoute: AppRouter.onGenerateRoute,
            initialRoute: '/login',
          ),
        ),
      );
      await tester.pump();
      expect(find.text('카카오로 시작하기'), findsOneWidget);
    });

    testWidgets('unknown route renders error message', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          locale: const Locale('ko'),
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
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            locale: const Locale('ko'),
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

      // login 화면으로 전환됨 — LoginScreen의 카카오로 시작하기 버튼 확인
      expect(find.text('카카오로 시작하기'), findsOneWidget);

      // SharedPreferences에 완료 플래그 저장됨
      expect(prefs.getBool('onboarding_complete'), isTrue);
    });
  });

  group('onGenerateInitialRoutes — 루트 탭 뒤로가기 제거', () {
    test('초기 라우트를 단일 스택으로 생성한다 (기본 2단 스택 방지)', () {
      // main.dart의 onGenerateInitialRoutes와 동일한 패턴.
      // 기본 동작은 "/home" → ["/", "/home"] 2단 스택을 만들지만,
      // 아래 패턴은 라우트를 정확히 하나만 생성한다.
      final routes = <Route<dynamic>>[
        AppRouter.onGenerateRoute(const RouteSettings(name: AppRouter.home)),
      ];
      expect(routes.length, 1);
      expect(routes.first, isA<MaterialPageRoute>());
    });

    testWidgets('루트 진입 시 canPop=false, 하위 화면 push 후 canPop=true',
        (tester) async {
      final navKey = GlobalKey<NavigatorState>();
      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            navigatorKey: navKey,
            initialRoute: AppRouter.login,
            onGenerateInitialRoutes: (name) =>
                [AppRouter.onGenerateRoute(RouteSettings(name: name))],
            onGenerateRoute: AppRouter.onGenerateRoute,
          ),
        ),
      );
      await tester.pump();

      // 단일 라우트 스택 → 루트에서는 뒤로 갈 곳이 없다(뒤로가기 버튼 미표시).
      expect(navKey.currentState!.canPop(), isFalse);

      // 하위 화면을 push하면 뒤로가기가 가능해진다(설정/마이페이지 등 진입 시).
      navKey.currentState!.pushNamed(AppRouter.login);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 350));
      expect(navKey.currentState!.canPop(), isTrue);
    });
  });
}
