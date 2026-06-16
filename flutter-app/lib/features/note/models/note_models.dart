// 노트 도메인 모델.
//
// 작성 기준: 기존 `sharing_post_response.dart`와 동일한 방식
// (평범한 Dart 클래스 + 손으로 쓴 fromJson + null 안전 기본값).
// 필드는 04_API_명세서 §4.3.1(목록)·§4.3.4(생성)에 정의된 것만 V1 범위로 담는다.

/// 노트 카테고리 코드 상수(매직 스트링 방지).
const String kNoteCatMeditation = 'MEDITATION'; // QT
const String kNoteCatSermon = 'SERMON'; // 설교
const String kNoteCatPrayer = 'PRAYER';
const String kNoteCatRepentance = 'REPENTANCE';
const String kNoteCatGratitude = 'GRATITUDE';

/// 기록 탭이 아니라 각자 화면(오늘의 QT/성경)에서 작성하는 카테고리.
/// 기록 탭에서는 이 카테고리들의 작성 진입(FAB)을 두지 않는다(수정·삭제만).
const Set<String> tabAuthoredCategories = {kNoteCatMeditation, kNoteCatSermon};

/// 카테고리 코드 → 한글 라벨. (여러 화면에서 재사용)
const Map<String, String> noteCategoryLabels = {
  kNoteCatMeditation: 'QT',
  kNoteCatSermon: '설교',
  kNoteCatPrayer: '기도',
  kNoteCatRepentance: '회개',
  kNoteCatGratitude: '감사',
};

/// 카테고리 코드를 한글 라벨로 변환 (모르는 코드는 코드 그대로 반환).
String noteCategoryLabel(String code) => noteCategoryLabels[code] ?? code;

/// 사용자가 직접 새로 작성할 수 있는 자유 노트 카테고리 (N-02에서 선택).
/// 묵상(QT 화면)·설교(성경 화면)는 다른 화면에서 작성하므로 제외.
const List<String> writableNoteCategories = ['PRAYER', 'REPENTANCE', 'GRATITUDE'];

class NoteCategoryOption {
  final String category;
  final String label;
  final bool requiresQtPassage;
  final bool supportsVerseSelection;
  final bool writableFromList;

  const NoteCategoryOption({
    required this.category,
    required this.label,
    required this.requiresQtPassage,
    required this.supportsVerseSelection,
    required this.writableFromList,
  });

  factory NoteCategoryOption.fromJson(Map<String, dynamic> json) {
    final category = json['category'] as String? ?? '';
    return NoteCategoryOption(
      category: category,
      label: json['label'] as String? ?? noteCategoryLabel(category),
      requiresQtPassage: json['requiresQtPassage'] as bool? ?? false,
      supportsVerseSelection: json['supportsVerseSelection'] as bool? ?? false,
      writableFromList: json['writableFromList'] as bool? ?? false,
    );
  }
}

List<NoteCategoryOption> fallbackWritableNoteCategoryOptions() {
  return writableNoteCategories
      .map((code) => NoteCategoryOption(
            category: code,
            label: noteCategoryLabel(code),
            requiresQtPassage: false,
            supportsVerseSelection: false,
            writableFromList: true,
          ))
      .toList();
}

/// 노트 작성/수정(N-03) 라우트 인자 — **화면 간 계약(모델)**.
///
/// 화면 위젯이 아니라 이 계약만 의존하도록 모델에 둔다(bible 성경 화면 등 타 기능이
/// note 화면을 직접 import하지 않게 함). 모드: 작성=category, 수정=noteId,
/// 설교 노트(성경 화면)=verseIds + referenceText/versePreview로 인용 절·본문 동봉.
class NoteEditArgs {
  final String? category; // 작성 모드에서 필수
  final int? noteId; // 수정 모드에서 필수 (null이면 작성)

  /// 작성 진입 시 미리 동봉하는 인용 절(설교 노트 ②: 성경 화면에서 선택한 절).
  /// note_verses(verseIds)로 저장된다(§6.4.1). 자유노트(N-02)는 비운다.
  final List<int>? verseIds;

  /// 성경 본문에서 진입할 때 보여줄 선택 범위 라벨(예: "고린도전서 7:25-30"). 없으면 미표시.
  final String? referenceText;

  /// 선택 범위 본문 미리보기(인용). 작성 화면 상단에 읽기 전용으로 보여준다. 없으면 미표시.
  final String? versePreview;

  const NoteEditArgs({
    this.category,
    this.noteId,
    this.verseIds,
    this.referenceText,
    this.versePreview,
  });

  bool get isEdit => noteId != null;
}

/// 노트 목록 1건 (GET /api/v1/notes, 04 §4.3.1).
class NoteListItem {
  final int id;
  final String category; // MEDITATION/SERMON/PRAYER/REPENTANCE/GRATITUDE
  final String title;
  final String? bodyPreview; // 목록 카드용 본문 앞부분 발췌 (없을 수 있음)
  final String status; // DRAFT/SAVED
  final String visibility; // PRIVATE/PUBLIC
  final String? qtDate; // QT 노트만 값이 있음 (yyyy-MM-dd)
  final String? rangeLabel; // 예: "창세기 1:1-5" (없을 수 있음)
  final bool shared; // 나눔으로 공개됐는지
  final DateTime? createdAt;
  final DateTime? updatedAt;

  NoteListItem({
    required this.id,
    required this.category,
    required this.title,
    this.bodyPreview,
    required this.status,
    required this.visibility,
    this.qtDate,
    this.rangeLabel,
    required this.shared,
    this.createdAt,
    this.updatedAt,
  });

  // 왜 이렇게 짰냐면:
  // 서버 응답에 필드가 빠지거나 null이어도 앱이 죽지 않도록
  // `as String? ?? ''` 처럼 전부 기본값을 줬다. (기존 나눔 모델과 동일 방어)
  factory NoteListItem.fromJson(Map<String, dynamic> json) {
    return NoteListItem(
      id: json['id'] as int,
      category: json['category'] as String? ?? '',
      title: json['title'] as String? ?? '',
      bodyPreview: json['bodyPreview'] as String?,
      status: json['status'] as String? ?? '',
      visibility: json['visibility'] as String? ?? 'PRIVATE',
      qtDate: json['qtDate'] as String?,
      rangeLabel: json['rangeLabel'] as String?,
      shared: json['shared'] as bool? ?? false,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.parse(json['updatedAt'] as String)
          : null,
    );
  }
}

/// 노트 목록 응답 (페이징).
class NoteListResponse {
  final List<NoteListItem> items;
  final bool hasNext;

  NoteListResponse({required this.items, required this.hasNext});

  // 왜 이렇게 짰냐면:
  // 백엔드 페이징 응답은 content[] + last(마지막 페이지 여부)로 온다.
  // 무한 스크롤 판단을 쉽게 하려고 last를 뒤집어 hasNext로 보관한다.
  factory NoteListResponse.fromJson(Map<String, dynamic> json) {
    final content = json['content'] as List<dynamic>? ?? [];
    return NoteListResponse(
      items: content
          .map((e) => NoteListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      hasNext: !(json['last'] as bool? ?? true),
    );
  }
}

/// 노트에 인용된 성경 절 1건 (상세 응답의 verses[], 04 §4.3.5).
///
/// V1 노트 화면에서는 "표시만" 한다(작성 중 절 추가는 v2 @멘션 범위).
class NoteVerseRef {
  final int bibleVerseId;
  final String bookCode;
  final int? chapterNo;
  final int? verseNo;
  final int? displayOrder;

  NoteVerseRef({
    required this.bibleVerseId,
    required this.bookCode,
    this.chapterNo,
    this.verseNo,
    this.displayOrder,
  });

  // 왜 이렇게 짰냐면:
  // verses는 묵상/설교 노트에만 있고 자유노트엔 빈 배열이라,
  // 숫자 필드도 null 안전(int?)으로 두어 어떤 카테고리든 깨지지 않게 했다.
  factory NoteVerseRef.fromJson(Map<String, dynamic> json) {
    return NoteVerseRef(
      bibleVerseId: json['bibleVerseId'] as int,
      bookCode: json['bookCode'] as String? ?? '',
      chapterNo: json['chapterNo'] as int?,
      verseNo: json['verseNo'] as int?,
      displayOrder: json['displayOrder'] as int?,
    );
  }
}

/// 노트 상세 (GET /api/v1/notes/{id}, 04 §4.3.5).
///
/// 전 카테고리(QT/설교/기도/회개/감사)가 단일 `body`를 쓴다. QT 노트도 QT 에디터에서
/// 단일 body로 저장하므로, 과거 4섹션(느낀 점/기억할 구절/적용할 점/기도) 모델은 제거했다
/// (2026-06-12, 구현이 이미 단일 body — F-03 스펙 드리프트는 Lead 공유). 표시·편집은 모두 body 기준.
class NoteDetail {
  final int id;
  final String category;
  final int? qtPassageId;
  final String title;
  final String? body;
  final String status; // DRAFT/SAVED
  final String visibility; // PRIVATE/PUBLIC
  final String? qtDate;
  final String? rangeLabel;
  final bool shared;
  final DateTime? savedAt;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final List<NoteVerseRef> verses;

  NoteDetail({
    required this.id,
    required this.category,
    this.qtPassageId,
    required this.title,
    this.body,
    required this.status,
    required this.visibility,
    this.qtDate,
    this.rangeLabel,
    required this.shared,
    this.savedAt,
    this.createdAt,
    this.updatedAt,
    required this.verses,
  });

  // 왜 이렇게 짰냐면:
  // 목록 모델과 동일한 방어(null이면 기본값)로 어떤 응답이 와도 앱이 안 죽게 한다.
  // 응답에 과거 4섹션 키가 와도 읽지 않으므로(extra 키 무시) 안전하다.
  factory NoteDetail.fromJson(Map<String, dynamic> json) {
    final verses = (json['verses'] as List<dynamic>? ?? [])
        .map((e) => NoteVerseRef.fromJson(e as Map<String, dynamic>))
        .toList();
    return NoteDetail(
      id: json['id'] as int,
      category: json['category'] as String? ?? '',
      qtPassageId: json['qtPassageId'] as int?,
      title: json['title'] as String? ?? '',
      body: json['body'] as String?,
      status: json['status'] as String? ?? '',
      visibility: json['visibility'] as String? ?? 'PRIVATE',
      qtDate: json['qtDate'] as String?,
      rangeLabel: json['rangeLabel'] as String?,
      shared: json['shared'] as bool? ?? false,
      savedAt: json['savedAt'] != null
          ? DateTime.parse(json['savedAt'] as String)
          : null,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.parse(json['updatedAt'] as String)
          : null,
      verses: verses,
    );
  }
}

/// 묵상 달력 응답 (GET /api/v1/me/meditation-calendar, 04 §4.6.2).
class MeditationCalendar {
  final String month; // "2026-05"
  final List<CalendarDay> days;
  final CalendarSummary summary;

  MeditationCalendar({
    required this.month,
    required this.days,
    required this.summary,
  });

  factory MeditationCalendar.fromJson(Map<String, dynamic> json) {
    final days = (json['days'] as List<dynamic>? ?? [])
        .map((e) => CalendarDay.fromJson(e as Map<String, dynamic>))
        .toList();
    return MeditationCalendar(
      month: json['month'] as String? ?? '',
      days: days,
      summary: CalendarSummary.fromJson(
          json['summary'] as Map<String, dynamic>? ?? const {}),
    );
  }
}

/// 달력의 하루.
class CalendarDay {
  final DateTime date;
  final bool saved; // 그날 저장된 노트가 있는가 (점 표시 기준)
  final int savedNoteCount;
  final int? meditationNoteId; // 그날 묵상노트 id(있으면 탭 시 이동)
  final List<String> categories; // 그날 작성한 카테고리들

  CalendarDay({
    required this.date,
    required this.saved,
    required this.savedNoteCount,
    this.meditationNoteId,
    required this.categories,
  });

  factory CalendarDay.fromJson(Map<String, dynamic> json) {
    return CalendarDay(
      // ✏️ "2026-05-17" 문자열을 DateTime으로. 비교는 시간 떼고 해야 하므로 화면에서 처리.
      date: DateTime.parse(json['date'] as String),
      saved: json['saved'] as bool? ?? false,
      savedNoteCount: json['savedNoteCount'] as int? ?? 0,
      meditationNoteId: json['meditationNoteId'] as int?,
      categories: (json['categories'] as List<dynamic>? ?? [])
          .map((e) => e as String)
          .toList(),
    );
  }
}

/// 달력 요약(저장한 날 수, 노트 수, 연속 묵상일).
class CalendarSummary {
  final int savedDays;
  final int savedNoteCount;
  final int meditationStreakDays;

  CalendarSummary({
    required this.savedDays,
    required this.savedNoteCount,
    required this.meditationStreakDays,
  });

  factory CalendarSummary.fromJson(Map<String, dynamic> json) {
    return CalendarSummary(
      savedDays: json['savedDays'] as int? ?? 0,
      savedNoteCount: json['savedNoteCount'] as int? ?? 0,
      meditationStreakDays: json['meditationStreakDays'] as int? ?? 0,
    );
  }
}

/// 노트 생성 응답 (POST /api/v1/notes, 04 §4.3.4).
class NoteCreateResponse {
  final int id;
  final String category;
  final String status;
  final String visibility;
  final int? sharedPostId;
  final DateTime? createdAt;

  NoteCreateResponse({
    required this.id,
    required this.category,
    required this.status,
    required this.visibility,
    this.sharedPostId,
    this.createdAt,
  });

  factory NoteCreateResponse.fromJson(Map<String, dynamic> json) {
    return NoteCreateResponse(
      id: json['id'] as int,
      category: json['category'] as String? ?? '',
      status: json['status'] as String? ?? '',
      visibility: json['visibility'] as String? ?? 'PRIVATE',
      sharedPostId: json['sharedPostId'] as int?,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : null,
    );
  }
}
