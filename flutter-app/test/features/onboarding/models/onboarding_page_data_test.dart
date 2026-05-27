import 'package:flutter_test/flutter_test.dart';

import 'package:qtai_app/features/onboarding/models/onboarding_page_data.dart';

void main() {
  group('OnboardingPageData', () {
    test('defaults는 4페이지이다', () {
      expect(OnboardingPageData.defaults.length, 4);
    });

    test('모든 페이지에 title과 description이 있다', () {
      for (final page in OnboardingPageData.defaults) {
        expect(page.title.isNotEmpty, isTrue);
        expect(page.description.isNotEmpty, isTrue);
      }
    });

    test('마지막 페이지 제목은 시작 관련 문구이다', () {
      final lastPage = OnboardingPageData.defaults.last;
      expect(lastPage.title, contains('시작'));
    });

    test('모든 페이지의 배경색이 서로 다르다', () {
      final colors = OnboardingPageData.defaults
          .map((p) => p.backgroundColor.toARGB32())
          .toSet();
      expect(colors.length, OnboardingPageData.defaults.length);
    });
  });
}
