import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/core/utils/date_format_utils.dart';

void main() {
  group('monthDayLabel', () {
    test('월/일을 한국어로 표시한다', () {
      expect(monthDayLabel(DateTime(2026, 6, 7)), '6월 7일');
      expect(monthDayLabel(DateTime(2026, 12, 25)), '12월 25일');
    });
  });

  group('monthDayFromIso', () {
    test('yyyy-MM-dd를 "M월 d일"로 변환한다', () {
      expect(monthDayFromIso('2026-06-07'), '6월 7일');
      expect(monthDayFromIso('2026-11-30'), '11월 30일');
    });

    test('형식이 맞지 않으면 원문을 그대로 반환한다', () {
      expect(monthDayFromIso('2026/06/07'), '2026/06/07');
      expect(monthDayFromIso('bad'), 'bad');
      expect(monthDayFromIso('2026-XX-07'), '2026-XX-07');
    });
  });

  group('relativeTimeLabel', () {
    final now = DateTime(2026, 6, 12, 12, 0, 0);

    test('null이면 빈 문자열', () {
      expect(relativeTimeLabel(null, now: now), '');
    });

    test('1분 미만은 "방금"', () {
      expect(
          relativeTimeLabel(now.subtract(const Duration(seconds: 30)), now: now),
          '방금');
    });

    test('분 단위', () {
      expect(
          relativeTimeLabel(now.subtract(const Duration(minutes: 5)), now: now),
          '5분 전');
    });

    test('시간 단위', () {
      expect(
          relativeTimeLabel(now.subtract(const Duration(hours: 2)), now: now),
          '2시간 전');
    });

    test('일 단위(7일 미만)', () {
      expect(
          relativeTimeLabel(now.subtract(const Duration(days: 3)), now: now),
          '3일 전');
    });

    test('7일 이상은 yyyy.MM.dd', () {
      final old = DateTime(2026, 6, 1, 9, 0, 0);
      expect(relativeTimeLabel(old, now: now), '2026.06.01');
    });
  });
}
