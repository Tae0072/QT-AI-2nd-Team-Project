import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/bible/models/passage_view_logic.dart';

void main() {
  group('resolvePassageFocusVerse', () {
    test('요청 절이 장 절 목록에 있으면 그대로 사용', () {
      expect(resolvePassageFocusVerse([1, 2, 3, 4, 5], 3), 3);
    });

    test('요청 절이 목록에 없으면 첫 절로 폴백(먼 절 진입 안전)', () {
      expect(resolvePassageFocusVerse([25, 26, 27], 1), 25);
      expect(resolvePassageFocusVerse([1, 2, 3], 999), 1);
    });

    test('요청이 null이면 첫 절', () {
      expect(resolvePassageFocusVerse([10, 11, 12], null), 10);
    });

    test('절 목록이 비면 요청값(없으면 1)', () {
      expect(resolvePassageFocusVerse([], 5), 5);
      expect(resolvePassageFocusVerse([], null), 1);
    });
  });

  group('passageVerseLabel', () {
    test('단일 절', () => expect(passageVerseLabel(25, 25), '25'));
    test('범위 절', () => expect(passageVerseLabel(25, 30), '25-30'));
  });
}
