// 성경 본문 페이지(BiblePassageScreen)의 순수 로직.
// 위젯/Provider와 분리해 단위 테스트할 수 있게 한다(포커스 보정·범위 라벨).

/// 진입 시 포커스(첫 화면 노출·하이라이트)할 절 번호를 장의 실제 절 목록 안으로 보정한다.
///
/// - 절 목록이 비면 요청값(없으면 1)을 그대로 둔다.
/// - 요청 절이 목록에 있으면 그 절.
/// - 요청 절이 목록에 없거나 null이면 첫 절로 폴백.
int resolvePassageFocusVerse(List<int> verseNos, int? requested) {
  if (verseNos.isEmpty) return requested ?? 1;
  if (requested != null && verseNos.contains(requested)) return requested;
  return verseNos.first;
}

/// 절 범위 라벨. 단일이면 "25", 범위면 "25-30". (from <= to 전제)
String passageVerseLabel(int from, int to) =>
    from == to ? '$from' : '$from-$to';
