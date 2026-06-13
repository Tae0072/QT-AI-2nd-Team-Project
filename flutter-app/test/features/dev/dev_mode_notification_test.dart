import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/dev/dev_mode_screen.dart';

void main() {
  testWidgets('개발자 모드에 알림 테스트 버튼들이 표시된다', (tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: DevModeScreen()),
    );
    await tester.pumpAndSettle();

    expect(find.text('알림 보내기 (테스트)'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, '좋아요 알림'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, '댓글 알림'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, '100개 돌파'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, '1000개 돌파'), findsOneWidget);
    expect(
      find.widgetWithText(FilledButton, '테스트 알림 보내기'),
      findsOneWidget,
    );
  });
}
