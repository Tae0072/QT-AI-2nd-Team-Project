/// 성경 본문 범위 표기.
class BibleReference {
  final String koreanBookName;
  final String? englishBookName;
  final int chapter;
  final int verseFrom;
  final int verseTo;

  const BibleReference({
    required this.koreanBookName,
    required this.englishBookName,
    required this.chapter,
    required this.verseFrom,
    required this.verseTo,
  });

  String get displayText => '$koreanBookName $chapter:$verseFrom-$verseTo';
}

/// 성서유니온 오늘 본문 표기에서 권, 장, 절만 추출한다.
class BibleReferenceParser {
  static final _rangePattern = RegExp(
    r'^\s*([^(\d]+?)\s*(?:\(([^)]+)\))?\s*(\d+)\s*:\s*(\d+)\s*-\s*(?:(\d+)\s*:\s*)?(\d+)\s*$',
  );

  static BibleReference parse(String text) {
    final match = _rangePattern.firstMatch(text);
    if (match == null) {
      throw FormatException('성경 본문 범위를 읽을 수 없습니다: $text');
    }

    final chapter = int.parse(match.group(3)!);
    final endChapterText = match.group(5);
    final endChapter =
        endChapterText == null ? chapter : int.parse(endChapterText);

    if (endChapter != chapter) {
      throw const FormatException('다중 장 범위는 현재 Flutter v1 범위가 아닙니다.');
    }

    final verseFrom = int.parse(match.group(4)!);
    final verseTo = int.parse(match.group(6)!);

    if (verseFrom > verseTo) {
      throw const FormatException('시작 절은 끝 절보다 클 수 없습니다.');
    }

    return BibleReference(
      koreanBookName: match.group(1)!.trim(),
      englishBookName: match.group(2)?.trim(),
      chapter: chapter,
      verseFrom: verseFrom,
      verseTo: verseTo,
    );
  }
}
