import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/bible/models/verse_range_selection.dart';

void main() {
  // 시작 상태: 단일 1절, 앵커 없음(첫 탭 대기).
  const start = VerseRangeSelection(from: 1, to: 1);

  group('VerseRangeSelection.tap — 분기/경계', () {
    test('앵커 전 첫 탭 → 그 절로 단일 선택 + 앵커', () {
      final r = start.tap(3);
      expect(r.from, 3);
      expect(r.to, 3);
      expect(r.anchored, isTrue);
    });

    test('둘째 탭 전진(끝 절 ≥ 시작) → 범위 [시작, 끝], 앵커 해제', () {
      final r = start.tap(3).tap(7);
      expect(r.from, 3);
      expect(r.to, 7);
      expect(r.anchored, isFalse);
    });

    test('둘째 탭 후진(끝 절 < 시작) → 앞/뒤 자동 정렬(스왑)', () {
      final r = start.tap(7).tap(3);
      expect(r.from, 3);
      expect(r.to, 7);
      expect(r.anchored, isFalse);
    });

    test('둘째 탭이 시작과 같은 절(경계) → 단일 유지, 앵커 해제', () {
      final r = start.tap(5).tap(5);
      expect(r.from, 5);
      expect(r.to, 5);
      expect(r.anchored, isFalse);
    });

    test('셋째 탭 → 새 단일 선택으로 재시작(재앵커)', () {
      final r = start.tap(3).tap(7).tap(2);
      expect(r.from, 2);
      expect(r.to, 2);
      expect(r.anchored, isTrue);
    });

    test('탭-탭-탭 시나리오: 단일 → 범위 → 재시작 단일', () {
      var s = start.tap(2); // 단일 2
      expect([s.from, s.to, s.anchored], [2, 2, true]);
      s = s.tap(4); // 범위 2-4
      expect([s.from, s.to, s.anchored], [2, 4, false]);
      s = s.tap(6); // 재시작 단일 6
      expect([s.from, s.to, s.anchored], [6, 6, true]);
    });

    test('동일 값은 == / hashCode가 같다', () {
      const a = VerseRangeSelection(from: 2, to: 4, anchored: true);
      const b = VerseRangeSelection(from: 2, to: 4, anchored: true);
      expect(a, b);
      expect(a.hashCode, b.hashCode);
    });
  });
}
