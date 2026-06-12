import 'package:flutter/foundation.dart';

/// 성경 목차의 절 범위 선택 상태 — "탭-탭"으로 범위, "탭" 한 번이면 단일 절.
///
/// 분기(첫 탭 앵커 / 둘째 탭 전진·후진 정렬 / 셋째 탭 재시작)를 화면 State에서 분리해
/// 순수 함수로 단위 테스트할 수 있게 한다. UI는 [tap] 결과로 from/to/anchored만 갱신한다.
@immutable
class VerseRangeSelection {
  /// 선택 시작 절(1-based).
  final int from;

  /// 선택 끝 절(단일 선택이면 from과 동일).
  final int to;

  /// 첫 탭으로 시작점을 찍고 둘째 탭(범위 끝)을 기다리는 중인지.
  final bool anchored;

  const VerseRangeSelection({
    required this.from,
    required this.to,
    this.anchored = false,
  });

  /// 절 [verse]를 탭했을 때의 다음 선택 상태를 반환한다.
  ///
  /// - 앵커 전(첫 탭): 그 절로 단일 선택하고 범위 시작을 앵커한다.
  /// - 앵커 후(둘째 탭): 범위 끝을 지정하되 앞/뒤를 자동 정렬한다(작은 절이 from).
  ///   그리고 앵커를 해제해 다음 탭은 새 단일 선택이 된다.
  VerseRangeSelection tap(int verse) {
    if (!anchored) {
      return VerseRangeSelection(from: verse, to: verse, anchored: true);
    }
    if (verse >= from) {
      return VerseRangeSelection(from: from, to: verse);
    }
    return VerseRangeSelection(from: verse, to: from);
  }

  @override
  bool operator ==(Object other) =>
      other is VerseRangeSelection &&
      other.from == from &&
      other.to == to &&
      other.anchored == anchored;

  @override
  int get hashCode => Object.hash(from, to, anchored);

  @override
  String toString() => 'VerseRangeSelection($from-$to, anchored: $anchored)';
}
