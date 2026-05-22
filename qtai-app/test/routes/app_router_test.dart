import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/routes/app_router.dart';

void main() {
  group('AppRouter', () {
    test('라우트 상수가 올바르게 정의되어 있다', () {
      expect(AppRouter.splash, equals('/'));
      expect(AppRouter.login, equals('/login'));
      expect(AppRouter.home, equals('/home'));
      expect(AppRouter.onboarding, equals('/onboarding'));
      expect(AppRouter.myPage, equals('/my-page'));
    });

    test('home 라우트가 MaterialPageRoute를 반환한다', () {
      final route = AppRouter.onGenerateRoute(
        const RouteSettings(name: '/home'),
      );
      expect(route, isA<MaterialPageRoute>());
    });

    test('알 수 없는 라우트가 fallback 화면을 반환한다', () {
      final route = AppRouter.onGenerateRoute(
        const RouteSettings(name: '/unknown'),
      );
      expect(route, isA<MaterialPageRoute>());
    });

    testWidgets('home 라우트가 홈 텍스트를 렌더링한다', (tester) async {
      final route = AppRouter.onGenerateRoute(
        const RouteSettings(name: '/home'),
      );
      final page = (route as MaterialPageRoute).builder(
        tester.element(find.byType(Container).first),
      );

      await tester.pumpWidget(MaterialApp(home: page));
      await tester.pumpAndSettle();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('알 수 없는 라우트가 에러 메시지를 렌더링한다', (tester) async {
      final route = AppRouter.onGenerateRoute(
        const RouteSettings(name: '/nonexistent'),
      );
      final page = (route as MaterialPageRoute).builder(
        tester.element(find.byType(Container).first),
      );

      await tester.pumpWidget(MaterialApp(home: page));
      await tester.pumpAndSettle();
      expect(find.text('Route not found: /nonexistent'), findsOneWidget);
    });
  });
}
