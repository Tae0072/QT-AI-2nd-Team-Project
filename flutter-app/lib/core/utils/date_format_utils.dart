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

/// DateTime → "yyyy.MM.dd" (기록 카드 상단 날짜).
String dateDotLabel(DateTime d) =>
    '${d.year}.${d.month.toString().padLeft(2, '0')}.'
    '${d.day.toString().padLeft(2, '0')}';

/// "yyyy-MM-dd" → "yyyy.MM.dd". 형식이 아니면 원문을 그대로 돌려준다.
String dateDotFromIso(String iso) {
  final parts = iso.split('-');
  if (parts.length != 3) return iso;
  return '${parts[0]}.${parts[1]}.${parts[2]}';
}

/// DateTime → 12시간제 시각 "hh:mm AM/PM" (예: "07:00 AM", "08:30 PM").
String clockLabel(DateTime d) {
  final isPm = d.hour >= 12;
  var h = d.hour % 12;
  if (h == 0) h = 12;
  final hh = h.toString().padLeft(2, '0');
  final mm = d.minute.toString().padLeft(2, '0');
  return '$hh:$mm ${isPm ? 'PM' : 'AM'}';
}

/// DateTime → 시간대 한글 라벨("오전"/"오후"). 기록 카드 배지용.
String amPmKoLabel(DateTime d) => d.hour < 12 ? '오전' : '오후';

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
