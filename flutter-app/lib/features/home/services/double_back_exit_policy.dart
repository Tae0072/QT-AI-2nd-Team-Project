/// 뒤로가기 2번 종료 판정 정책.
///
/// 루트(홈)에서 뒤로가기 한 번에 앱이 바로 꺼지는 것을 막고,
/// 첫 입력은 안내만 하고 [window] 안에 다시 누를 때만 종료한다(안드로이드 관례).
///
/// 시간 판정을 순수 함수로 분리해 위젯 없이 창 경계를 단위 테스트한다.
class DoubleBackExitPolicy {
  DoubleBackExitPolicy._();

  /// 두 번째 입력을 종료로 인정하는 시간 창. 안내 스낵바 노출 시간과 일치시킨다.
  static const Duration window = Duration(seconds: 2);

  /// 종료 여부 판정 — [last]가 null(첫 입력)이거나 창을 넘기면 false(안내 단계).
  static bool shouldExit({required DateTime? last, required DateTime now}) {
    if (last == null) return false;
    return now.difference(last) <= window;
  }
}
