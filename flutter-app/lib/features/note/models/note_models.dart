// 노트 도메인 모델.
//
// 작성 기준: 기존 `sharing_post_response.dart`와 동일한 방식
// (평범한 Dart 클래스 + 손으로 쓴 fromJson + null 안전 기본값).
// 필드는 04_API_명세서 §4.3.1(목록)·§4.3.4(생성)에 정의된 것만 V1 범위로 담는다.

/// 카테고리 코드 → 한글 라벨. (여러 화면에서 재사용)
const Map<String, String> noteCategoryLabels = {
  'MEDITATION': '묵상',
  'SERMON': '설교',
  'PRAYER': '기도',
  'REPENTANCE': '회개',
  'GRATITUDE': '감사',
};

/// 카테고리 코드를 한글 라벨로 변환 (모르는 코드는 코드 그대로 반환).
String noteCategoryLabel(String code) => noteCategoryLabels[code] ?? code;

/// 사용자가 직접 새로 작성할 수 있는 자유 노트 카테고리 (N-02에서 선택).
/// 묵상(QT 화면)·설교(성경 화면)는 다른 화면에서 작성하므로 제외.
const List<String> writableNoteCategories = ['PRAYER', 'REPENTANCE', 'GRATITUDE'];

/// 노트 목록 1건 (GET /api/v1/notes, 04 §4.3.1).
class NoteListItem {
  final int id;
  final String category; // MEDITATION/SERMON/PRAYER/REPENTANCE/GRATITUDE
  final String title;
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
