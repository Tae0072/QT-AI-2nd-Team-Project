import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:qtai_app/core/config/app_config.dart';
import 'package:qtai_app/features/auth/screens/nickname_setup_screen.dart';

void main() {
  setUp(() {
    AppConfig.reset();
    AppConfig.initializeForTest();
  });

  tearDown(() {
    AppConfig.reset();
  });

  group('NicknameSetupScreen', () {
    testWidgets('환영 메시지가 표시된다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: NicknameSetupScreen()),
        ),
      );
      await tester.pump();

      expect(find.text('반갑습니다!'), findsOneWidget);
      expect(find.text('사용할 닉네임을 설정해주세요'), findsOneWidget);
    });

    testWidgets('닉네임 입력 필드가 존재한다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: NicknameSetupScreen()),
        ),
      );
      await tester.pump();

      expect(find.byType(TextFormField), findsOneWidget);
    });

    testWidgets('시작하기 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: NicknameSetupScreen()),
        ),
      );
      await tester.pump();

      expect(find.text('시작하기'), findsOneWidget);
    });

    testWidgets('안내 텍스트가 표시된다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: NicknameSetupScreen()),
        ),
      );
      await tester.pump();

      expect(find.text('한글, 영문, 숫자 조합 가능 (2~10자)'), findsOneWidget);
    });

    testWidgets('빈 닉네임 제출 시 유효성 검증 에러가 표시된다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: NicknameSetupScreen()),
        ),
      );
      await tester.pump();

      // 빈 상태에서 시작하기 탭
      await tester.tap(find.text('시작하기'));
      await tester.pumpAndSettle();

      expect(find.text('닉네임을 입력해주세요'), findsOneWidget);
    });

    testWidgets('1자 닉네임 제출 시 최소 길이 에러가 표시된다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: NicknameSetupScreen()),
        ),
      );
      await tester.pump();

      await tester.enterText(find.byType(TextFormField), 'A');
      await tester.tap(find.text('시작하기'));
      await tester.pumpAndSettle();

      expect(find.text('닉네임은 2자 이상이어야 합니다'), findsOneWidget);
    });

    testWidgets('11자 닉네임 입력 시 maxLength에 의해 10자로 제한된다',
        (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: NicknameSetupScreen()),
        ),
      );
      await tester.pump();

      await tester.enterText(find.byType(TextFormField), '가나다라마바사아자차카');
      await tester.pump();

      // maxLength: 10이므로 11자 입력 시 10자로 잘림
      final field = tester.widget<TextFormField>(find.byType(TextFormField));
      expect(field.controller!.text.length, lessThanOrEqualTo(10));
    });

    testWidgets('특수문자 닉네임 제출 시 유효성 검증 에러가 표시된다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: NicknameSetupScreen()),
        ),
      );
      await tester.pump();

      await tester.enterText(find.byType(TextFormField), 'test@!');
      await tester.tap(find.text('시작하기'));
      await tester.pumpAndSettle();

      expect(find.text('한글, 영문, 숫자만 사용할 수 있습니다'), findsOneWidget);
    });

    testWidgets('초기 상태에서 에러 메시지가 없다', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(home: NicknameSetupScreen()),
        ),
      );
      await tester.pump();

      expect(find.text('닉네임 설정에 실패했습니다. 다시 시도해주세요.'), findsNothing);
    });
  });
}
