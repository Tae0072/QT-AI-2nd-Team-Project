import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/routes/app_router.dart';

void main() {
  group('AppRouter', () {
    test('route constants are correctly defined', () {
      expect(AppRouter.splash, equals('/'));
      expect(AppRouter.login, equals('/login'));
      expect(AppRouter.home, equals('/home'));
      expect(AppRouter.onboarding, equals('/onboarding'));
      expect(AppRouter.myPage, equals('/my-page'));
    });

    test('home route returns MaterialPageRoute', () {
      final route = AppRouter.onGenerateRoute(
        const RouteSettings(name: '/home'),
      );
      expect(route, isA<MaterialPageRoute>());
    });

    test('unknown route returns fallback MaterialPageRoute', () {
      final route = AppRouter.onGenerateRoute(
        const RouteSettings(name: '/unknown'),
      );
      expect(route, isA<MaterialPageRoute>());
    });

    testWidgets('home route renders Scaffold', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          onGenerateRoute: AppRouter.onGenerateRoute,
          initialRoute: '/home',
        ),
      );
      await tester.pumpAndSettle();
      expect(find.byType(Scaffold), findsOneWidget);
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
  });
}
