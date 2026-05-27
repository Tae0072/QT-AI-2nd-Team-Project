import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:qtai_app/features/onboarding/widgets/page_indicator.dart';

void main() {
  Widget buildWidget({int currentPage = 0, int pageCount = 4}) {
    return MaterialApp(
      home: Scaffold(
        body: Center(
          child: PageIndicator(
            currentPage: currentPage,
            pageCount: pageCount,
          ),
        ),
      ),
    );
  }

  group('PageIndicator', () {
    testWidgets('페이지 수만큼 dot이 표시된다', (tester) async {
      await tester.pumpWidget(buildWidget(pageCount: 4));

      final dots = find.byType(AnimatedContainer);
      expect(dots, findsNWidgets(4));
    });

    testWidgets('활성 dot은 더 넓다', (tester) async {
      await tester.pumpWidget(buildWidget(currentPage: 0, pageCount: 3));

      // 첫 번째(활성) dot의 width가 24, 나머지는 8
      final firstBox = tester.getSize(find.byType(AnimatedContainer).first);
      final secondBox = tester.getSize(find.byType(AnimatedContainer).at(1));

      expect(firstBox.width, greaterThan(secondBox.width));
    });

    testWidgets('페이지 변경 시 활성 dot이 이동한다', (tester) async {
      await tester.pumpWidget(buildWidget(currentPage: 2, pageCount: 4));

      // 세 번째 dot이 활성이므로 넓어야 함
      final sizes = List.generate(4, (i) {
        return tester.getSize(find.byType(AnimatedContainer).at(i));
      });

      expect(sizes[2].width, greaterThan(sizes[0].width));
      expect(sizes[2].width, greaterThan(sizes[1].width));
      expect(sizes[2].width, greaterThan(sizes[3].width));
    });
  });
}
