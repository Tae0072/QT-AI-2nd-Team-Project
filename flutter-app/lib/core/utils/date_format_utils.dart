/// 날짜·상대시간 표시 라벨 유틸 (UI 전용, 순수 함수 — 단위 테스트 대상).
///
/// l10n에 의존하지 않는 한국어 표기. 시점이 필요한 함수는 [now]를 주입할 수 있어
/// 테스트에서 결정적으로 검증할 수 있다.
library;

/// DateTime → "M월 d일".
String monthDayLabel(DateTime d) => '${d.month}월 ${d.day}일';

/// "yyyy-MM-dd" → "M월 d일". 형식이 아니면 원문을 그대로 돌려준다.
String monthDayFromIso(String iso) {
  final parts = iso.split('-');
  if (parts.length != 3) return iso;
  final m = int.tryParse(parts[1]);
  final d = int.tryParse(parts[2]);
  if (m == null || d == null) return iso;
  return '$m월 $d일';
}

/// 게시 시각 → 상대 시간 라벨.
/// - 1분 미만: "방금"
/// - 1시간 미만: "N분 전"
/// - 24시간 미만: "N시간 전"
/// - 7일 미만: "N일 전"
/// - 그 이상: "yyyy.MM.dd"
/// null이면 빈 문자열. [now]를 주입하면 기준 시점을 고정할 수 있다(테스트용).
String relativeTimeLabel(DateTime? t, {DateTime? now}) {
  if (t == null) return '';
  final ref = now ?? DateTime.now();
  final diff = ref.difference(t);
  if (diff.inMinutes < 1) return '방금';
  if (diff.inMinutes < 60) return '${diff.inMinutes}분 전';
  if (diff.inHours < 24) return '${diff.inHours}시간 전';
  if (diff.inDays < 7) return '${diff.inDays}일 전';
  return '${t.year}.${t.month.toString().padLeft(2, '0')}.'
      '${t.day.toString().padLeft(2, '0')}';
}
