import 'package:flutter/widgets.dart';

/// 앱 공통 치수 토큰 (③ Size·height 변수화).
///
/// 화면마다 흩어진 하드코딩 수치(여백·간격·반경) 대신 이 상수를 쓴다.
/// 단일 소스로 모아두면 간격 체계를 한 곳에서 바꿀 수 있고, 추후 반응형/테마
/// 확장의 토대가 된다. 신규/수정 코드부터 점진 적용한다.
abstract final class AppGap {
  AppGap._();

  static const double xs = 4;
  static const double sm = 8;
  static const double md = 12;
  static const double lg = 16;
  static const double xl = 24;
  static const double xxl = 32;

  /// 자주 쓰는 세로 간격 SizedBox (const 재사용).
  static const SizedBox h4 = SizedBox(height: xs);
  static const SizedBox h8 = SizedBox(height: sm);
  static const SizedBox h12 = SizedBox(height: md);
  static const SizedBox h16 = SizedBox(height: lg);
  static const SizedBox h24 = SizedBox(height: xl);

  /// 자주 쓰는 가로 간격 SizedBox.
  static const SizedBox w8 = SizedBox(width: sm);
  static const SizedBox w12 = SizedBox(width: md);
}

/// 모서리 반경 토큰.
abstract final class AppRadius {
  AppRadius._();

  static const double sm = 8;
  static const double md = 12;
  static const double lg = 16;
  static const double pill = 26;
}

/// 공통 패딩 토큰.
abstract final class AppPad {
  AppPad._();

  static const EdgeInsets all12 = EdgeInsets.all(AppGap.md);
  static const EdgeInsets all16 = EdgeInsets.all(AppGap.lg);
  static const EdgeInsets screen = EdgeInsets.all(AppGap.lg);
}
