import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/home/services/double_back_exit_policy.dart';

/// 뒤로가기 2번 종료 판정 테스트 — 시간 창(2초) 경계를 고정한다.
void main() {
  final base = DateTime(2026, 6, 11, 12, 0, 0);

  group('DoubleBackExitPolicy.shouldExit', () {
    test('첫 입력(last=null)은 종료하지 않는다 — 안내 단계', () {
      expect(DoubleBackExitPolicy.shouldExit(last: null, now: base), isFalse);
    });

    test('창(2초) 안의 재입력은 종료한다 (경계 2.000초 포함)', () {
      expect(
        DoubleBackExitPolicy.shouldExit(
            last: base, now: base.add(const Duration(milliseconds: 500))),
        isTrue,
      );
      expect(
        DoubleBackExitPolicy.shouldExit(
            last: base, now: base.add(DoubleBackExitPolicy.window)),
        isTrue,
      );
    });

    test('창을 넘긴 재입력은 종료하지 않는다 — 다시 안내 단계로', () {
      expect(
        DoubleBackExitPolicy.shouldExit(
            last: base,
            now: base.add(DoubleBackExitPolicy.window +
                const Duration(milliseconds: 1))),
        isFalse,
      );
    });
  });
}
