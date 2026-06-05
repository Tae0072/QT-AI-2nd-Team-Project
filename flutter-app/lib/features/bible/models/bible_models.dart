import 'bible_reference.dart';

class BibleBook {
  final int id;
  final String testament;
  final String code;
  final String koreanName;
  final String englishName;
  final int displayOrder;

  const BibleBook({
    required this.id,
    required this.testament,
    required this.code,
    required this.koreanName,
    required this.englishName,
    required this.displayOrder,
  });

  factory BibleBook.fromJson(Map<String, dynamic> json) {
    return BibleBook(
      id: json['id'] as int,
      testament: json['testament'] as String,
      code: json['code'] as String,
      koreanName: json['koreanName'] as String,
      englishName: json['englishName'] as String,
      displayOrder: json['displayOrder'] as int,
    );
  }

  bool matchesReference(BibleReference reference) {
    final normalizedEnglishName = reference.englishBookName?.toLowerCase();
    return koreanName == reference.koreanBookName ||
        code.toLowerCase() == reference.koreanBookName.toLowerCase() ||
        (normalizedEnglishName != null &&
            englishName.toLowerCase() == normalizedEnglishName);
  }
}

class BibleVerseBook {
  final String code;
  final String koreanName;
  final String englishName;
  final int chapter;

  const BibleVerseBook({
    required this.code,
    required this.koreanName,
    required this.englishName,
    required this.chapter,
  });

  factory BibleVerseBook.fromJson(Map<String, dynamic> json) {
    return BibleVerseBook(
      code: json['code'] as String,
      koreanName: json['koreanName'] as String,
      englishName: json['englishName'] as String,
      chapter: json['chapter'] as int,
    );
  }
}

class BibleVerse {
  final int id;
  final String bookCode;
  final int chapterNo;
  final int verseNo;
  final String? koreanText;
  final String? englishText;

  const BibleVerse({
    required this.id,
    required this.bookCode,
    required this.chapterNo,
    required this.verseNo,
    required this.koreanText,
    required this.englishText,
  });

  factory BibleVerse.fromJson(Map<String, dynamic> json) {
    return BibleVerse(
      id: json['id'] as int,
      bookCode: json['bookCode'] as String,
      chapterNo: json['chapterNo'] as int,
      verseNo: json['verseNo'] as int,
      koreanText: json['koreanText'] as String?,
      englishText: json['englishText'] as String?,
    );
  }
}

class BibleVerseRange {
  final BibleVerseBook book;
  final List<BibleVerse> verses;

  const BibleVerseRange({
    required this.book,
    required this.verses,
  });

  factory BibleVerseRange.fromJson(Map<String, dynamic> json) {
    return BibleVerseRange(
      book: BibleVerseBook.fromJson(json['book'] as Map<String, dynamic>),
      verses: (json['verses'] as List<dynamic>)
          .map((item) => BibleVerse.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}

class TodayQtPassage {
  final int? qtPassageId;
  final String? passageDate;
  final String? title;
  final String? cacheStatus;
  final bool hasExplanation;
  final BibleReference reference;
  final BibleVerseBook book;
  final List<BibleVerse> verses;

  const TodayQtPassage({
    this.qtPassageId,
    this.passageDate,
    this.title,
    this.cacheStatus,
    this.hasExplanation = false,
    required this.reference,
    required this.book,
    required this.verses,
  });
}

class TodayQtSummary {
  final int? qtPassageId;
  final String? passageDate;
  final String? title;
  final String? cacheStatus;
  final bool hasExplanation;
  final TodayQtRange? range;

  const TodayQtSummary({
    required this.qtPassageId,
    required this.passageDate,
    required this.title,
    required this.cacheStatus,
    required this.hasExplanation,
    required this.range,
  });

  factory TodayQtSummary.fromJson(Map<String, dynamic> json) {
    final rangeJson = json['range'];
    return TodayQtSummary(
      qtPassageId: json['qtPassageId'] as int?,
      passageDate: json['passageDate'] as String?,
      title: json['title'] as String?,
      cacheStatus: json['cacheStatus'] as String?,
      hasExplanation: json['hasExplanation'] as bool? ?? false,
      range: rangeJson == null
          ? null
          : TodayQtRange.fromJson(rangeJson as Map<String, dynamic>),
    );
  }
}

class TodayQtRange {
  final String testament;
  final String bookCode;
  final String koreanBookName;
  final String englishBookName;
  final int chapter;
  final int verseFrom;
  final int verseTo;
  final String displayText;

  const TodayQtRange({
    required this.testament,
    required this.bookCode,
    required this.koreanBookName,
    required this.englishBookName,
    required this.chapter,
    required this.verseFrom,
    required this.verseTo,
    required this.displayText,
  });

  factory TodayQtRange.fromJson(Map<String, dynamic> json) {
    return TodayQtRange(
      testament: json['testament'] as String,
      bookCode: json['bookCode'] as String,
      koreanBookName: json['koreanBookName'] as String,
      englishBookName: json['englishBookName'] as String,
      chapter: json['chapter'] as int,
      verseFrom: json['verseFrom'] as int,
      verseTo: json['verseTo'] as int,
      displayText: json['displayText'] as String,
    );
  }

  BibleReference toReference() {
    return BibleReference(
      koreanBookName: koreanBookName,
      englishBookName: englishBookName,
      chapter: chapter,
      verseFrom: verseFrom,
      verseTo: verseTo,
    );
  }
}

class QtStudyContent {
  final String? summary;
  final List<QtStudyExplanation> explanations;
  final List<QtStudyGlossaryTerm> glossaryTerms;

  const QtStudyContent({
    required this.summary,
    required this.explanations,
    required this.glossaryTerms,
  });

  factory QtStudyContent.fromJson(Map<String, dynamic> json) {
    return QtStudyContent(
      summary: json['summary'] as String?,
      explanations: ((json['explanations'] as List<dynamic>?) ?? const [])
          .map((item) =>
              QtStudyExplanation.fromJson(item as Map<String, dynamic>))
          .toList(),
      glossaryTerms: ((json['glossaryTerms'] as List<dynamic>?) ?? const [])
          .map((item) =>
              QtStudyGlossaryTerm.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }

  bool get hasVisibleContent {
    final summaryText = summary?.trim();
    return (summaryText != null && summaryText.isNotEmpty) ||
        explanations.isNotEmpty ||
        glossaryTerms.isNotEmpty;
  }

  /// TTS 낭독용 주석 전체 텍스트 (절 순서대로, 빈 항목 제외).
  String get readableText {
    final buf = StringBuffer();
    for (final item in explanations) {
      final text = item.explanation.trim();
      if (text.isNotEmpty) {
        buf.writeln(text);
      }
    }
    return buf.toString().trim();
  }
}

class QtStudyExplanation {
  final int verseId;
  final String? summary;
  final String explanation;
  final String? sourceLabel;
  final int? aiAssetId;

  const QtStudyExplanation({
    required this.verseId,
    required this.summary,
    required this.explanation,
    required this.sourceLabel,
    required this.aiAssetId,
  });

  factory QtStudyExplanation.fromJson(Map<String, dynamic> json) {
    return QtStudyExplanation(
      verseId: json['verseId'] as int,
      summary: json['summary'] as String?,
      explanation: json['explanation'] as String? ?? '',
      sourceLabel: json['sourceLabel'] as String?,
      aiAssetId: json['aiAssetId'] as int?,
    );
  }
}

class QtStudyGlossaryTerm {
  final int id;
  final int verseId;
  final String term;
  final String meaning;
  final String? sourceLabel;

  const QtStudyGlossaryTerm({
    required this.id,
    required this.verseId,
    required this.term,
    required this.meaning,
    required this.sourceLabel,
  });

  factory QtStudyGlossaryTerm.fromJson(Map<String, dynamic> json) {
    return QtStudyGlossaryTerm(
      id: json['id'] as int,
      verseId: json['verseId'] as int,
      term: json['term'] as String? ?? '',
      meaning: json['meaning'] as String? ?? '',
      sourceLabel: json['sourceLabel'] as String?,
    );
  }
}
