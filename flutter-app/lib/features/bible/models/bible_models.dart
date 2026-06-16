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

/// 성경 본문 범위의 해설 진입점 가용성 — QT 도메인의 `/qt/passage-study`가 제공한다.
///
/// `/bible/verses`(bible 도메인)는 qt·study에 의존할 수 없어(모듈 순환 방지) 해설
/// 가용성을 줄 수 없다. 대신 qt 도메인이 bible·study를 조합해 별도로 노출한다.
class BiblePassageStudy {
  /// 선택 범위가 속한 QT 본문 ID (해설 콘텐츠 조회용). 없으면 null.
  final int? qtPassageId;

  /// 승인된 해설 존재 여부 — 해설 진입점 활성 기준.
  final bool hasExplanation;

  const BiblePassageStudy({
    required this.qtPassageId,
    required this.hasExplanation,
  });

  static const BiblePassageStudy none =
      BiblePassageStudy(qtPassageId: null, hasExplanation: false);

  /// 해설 진입점을 띄울 수 있는지 — 콘텐츠 조회에 qtPassageId가 필요하다.
  bool get explanationReady => hasExplanation && qtPassageId != null;

  factory BiblePassageStudy.fromJson(Map<String, dynamic> json) {
    return BiblePassageStudy(
      qtPassageId: json['qtPassageId'] as int?,
      hasExplanation: json['hasExplanation'] as bool? ?? false,
    );
  }
}

class TodayQtPassage {
  final int? qtPassageId;
  final String? passageDate;
  final String? title;
  final String? cacheStatus;

  /// 시뮬레이터 상태 — READY/MISSING/FAILED/DISABLED (CLAUDE.md §6).
  /// 버튼은 READY일 때만 활성화한다. 미지의 값은 MISSING으로 다룬다.
  final String simulatorStatus;

  /// 승인된 해설 존재 여부 — 해설 진입점 활성 기준.
  final bool hasExplanation;

  /// 사용자 DRAFT 묵상 노트 ID (없으면 null).
  final int? draftNoteId;

  final BibleReference reference;
  final BibleVerseBook book;
  final List<BibleVerse> verses;

  const TodayQtPassage({
    this.qtPassageId,
    this.passageDate,
    this.title,
    this.cacheStatus,
    this.simulatorStatus = 'MISSING',
    this.hasExplanation = false,
    this.draftNoteId,
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

  /// 시뮬레이터 상태 — 서버 enum READY/MISSING/FAILED/DISABLED (CLAUDE.md §6).
  final String simulatorStatus;

  /// 승인된 해설 존재 여부 — 해설 진입점 활성 기준.
  final bool hasExplanation;

  /// 사용자 DRAFT 묵상 노트 ID (없으면 null).
  final int? draftNoteId;

  final TodayQtRange? range;

  const TodayQtSummary({
    required this.qtPassageId,
    required this.passageDate,
    required this.title,
    required this.cacheStatus,
    this.simulatorStatus = 'MISSING',
    this.hasExplanation = false,
    this.draftNoteId,
    required this.range,
  });

  /// 서버가 보장하는 시뮬레이터 상태 4값 — 미지의 값은 MISSING으로 방어한다.
  static const Set<String> _knownSimulatorStatuses = {
    'READY',
    'MISSING',
    'FAILED',
    'DISABLED',
  };

  factory TodayQtSummary.fromJson(Map<String, dynamic> json) {
    final rangeJson = json['range'];
    final rawSimulatorStatus = json['simulatorStatus'] as String?;
    return TodayQtSummary(
      qtPassageId: json['qtPassageId'] as int?,
      passageDate: json['passageDate'] as String?,
      title: json['title'] as String?,
      cacheStatus: json['cacheStatus'] as String?,
      simulatorStatus: _knownSimulatorStatuses.contains(rawSimulatorStatus)
          ? rawSimulatorStatus!
          : 'MISSING',
      hasExplanation: json['hasExplanation'] as bool? ?? false,
      draftNoteId: json['draftNoteId'] as int?,
      range: rangeJson == null
          ? null
          : TodayQtRange.fromJson(rangeJson as Map<String, dynamic>),
    );
  }
}

class QtVideoClip {
  final String status;
  final int? clipId;
  final int? qtPassageId;
  final String? title;
  final String? videoUrl;
  final int? sourceVideoId;
  final double? startTimeSec;
  final double? endTimeSec;
  final String? compositionType;
  final String? clipStatus;

  const QtVideoClip({
    required this.status,
    required this.clipId,
    required this.qtPassageId,
    required this.title,
    required this.videoUrl,
    required this.sourceVideoId,
    required this.startTimeSec,
    required this.endTimeSec,
    required this.compositionType,
    required this.clipStatus,
  });

  bool get isReady =>
      status == 'READY' && videoUrl != null && videoUrl!.isNotEmpty;

  factory QtVideoClip.fromJson(Map<String, dynamic> json) {
    return QtVideoClip(
      status: json['status'] as String? ?? 'MISSING',
      clipId: json['clipId'] as int?,
      qtPassageId: json['qtPassageId'] as int?,
      title: json['title'] as String?,
      videoUrl: json['videoUrl'] as String?,
      sourceVideoId: json['sourceVideoId'] as int?,
      startTimeSec: (json['startTimeSec'] as num?)?.toDouble(),
      endTimeSec: (json['endTimeSec'] as num?)?.toDouble(),
      compositionType: json['compositionType'] as String?,
      clipStatus: json['clipStatus'] as String?,
    );
  }
}

class TodayQtRange {
  final String testament;
  final String bookCode;
  final String koreanBookName;
  final String englishBookName;
  final int chapter; // 시작 장
  final int endChapter; // 종료 장 (같은 장이면 chapter와 동일)
  final int verseFrom;
  final int verseTo;
  final String displayText;

  const TodayQtRange({
    required this.testament,
    required this.bookCode,
    required this.koreanBookName,
    required this.englishBookName,
    required this.chapter,
    required this.endChapter,
    required this.verseFrom,
    required this.verseTo,
    required this.displayText,
  });

  /// 장 교차 본문 여부(예: 10:14-11:1).
  bool get isCrossChapter => endChapter != chapter;

  factory TodayQtRange.fromJson(Map<String, dynamic> json) {
    final chapter = json['chapter'] as int;
    return TodayQtRange(
      testament: json['testament'] as String,
      bookCode: json['bookCode'] as String,
      koreanBookName: json['koreanBookName'] as String,
      englishBookName: json['englishBookName'] as String,
      chapter: chapter,
      // 구 백엔드 호환: endChapter가 없으면 시작 장과 동일(단일 장)로 본다.
      endChapter: json['endChapter'] as int? ?? chapter,
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
      endChapter: endChapter,
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

  /// TTS 낭독용 해설 전체 텍스트 (절 순서대로, 빈 항목 제외).
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
