import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/sharing/widgets/page_navigator.dart';

void main() {
  Widget wrap(Widget child) =>
      MaterialApp(home: Scaffold(body: Center(child: child)));

  testWidgets('1페이지 이하면 아무것도 그리지 않는다', (tester) async {
    await tester.pumpWidget(wrap(
      PageNavigator(currentPage: 0, totalPages: 1, onSelect: (_) {}),
    ));
    expect(find.text('1'), findsNothing);
  });

  testWidgets('첫 블록은 1~5를 보여주고 번호 탭 시 0부터의 페이지를 전달한다',
      (tester) async {
    int? picked;
    await tester.pumpWidget(wrap(
      PageNavigator(currentPage: 0, totalPages: 12, onSelect: (p) => picked = p),
    ));

    for (final n in ['1', '2', '3', '4', '5']) {
      expect(find.text(n), findsOneWidget);
    }
    expect(find.text('6'), findsNothing);

    await tester.tap(find.text('3'));
    expect(picked, 2); // 화면 3 = 0부터 2
  });

  testWidgets('첫 페이지에서는 이전 화살표가 비활성(탭해도 콜백 없음)', (tester) async {
    int calls = 0;
    await tester.pumpWidget(wrap(
      PageNavigator(currentPage: 0, totalPages: 12, onSelect: (_) => calls++),
    ));
    await tester.tap(find.byIcon(Icons.chevron_left));
    expect(calls, 0);
  });

  testWidgets('다음 블록(6페이지=index5)은 6~10을 보여준다', (tester) async {
    await tester.pumpWidget(wrap(
      PageNavigator(currentPage: 5, totalPages: 12, onSelect: (_) {}),
    ));
    for (final n in ['6', '7', '8', '9', '10']) {
      expect(find.text(n), findsOneWidget);
    }
    expect(find.text('5'), findsNothing);
    expect(find.text('11'), findsNothing);
  });

  testWidgets('마지막 블록은 남은 개수만 보여준다(12페이지면 11,12)', (tester) async {
    await tester.pumpWidget(wrap(
      PageNavigator(currentPage: 11, totalPages: 12, onSelect: (_) {}),
    ));
    expect(find.text('11'), findsOneWidget);
    expect(find.text('12'), findsOneWidget);
    expect(find.text('13'), findsNothing);
  });
}
