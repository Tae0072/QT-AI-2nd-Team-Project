import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/bible/models/bible_reference.dart';

void main() {
  group('BibleReferenceParser', () {
    test('성서유니온 오늘 본문 표기에서 권, 장, 절 범위를 파싱한다', () {
      final result = BibleReferenceParser.parse(
        '고린도전서(1 Corinthians)1:10 - 1:17',
      );

      expect(result.koreanBookName, '고린도전서');
      expect(result.englishBookName, '1 Corinthians');
      expect(result.chapter, 1);
      expect(result.verseFrom, 10);
      expect(result.verseTo, 17);
      expect(result.displayText, '고린도전서 1:10-17');
    });
  });
}
