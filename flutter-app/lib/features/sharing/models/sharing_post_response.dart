/// 나눔 피드 목록 아이템.
class SharingPostItem {
  final int id;
  final String nicknameSnapshot;
  final String titleSnapshot;
  final String category;
  final String bodyPreview;

  // 본문 범위 라벨 스냅샷(예 고전 6:7). 없으면 null. JSON: verseSnapshot.rangeLabel.
  final String? verseLabel;
  final int likeCount;
  final int commentCount;
  final bool likedByMe;

  // 내가 저장(북마크)했는지. JSON: bookmarkedByMe.
  final bool bookmarkedByMe;
  final DateTime? publishedAt;

  SharingPostItem({
    required this.id,
    required this.nicknameSnapshot,
    required this.titleSnapshot,
    required this.category,
    required this.bodyPreview,
    this.verseLabel,
    required this.likeCount,
    required this.commentCount,
    required this.likedByMe,
    this.bookmarkedByMe = false,
    this.publishedAt,
  });

  factory SharingPostItem.fromJson(Map<String, dynamic> json) {
    return SharingPostItem(
      id: json['id'] as int,
      nicknameSnapshot: json['nicknameSnapshot'] as String? ?? '',
      titleSnapshot: json['titleSnapshot'] as String? ?? '',
      category: json['category'] as String? ?? '',
      bodyPreview: json['bodyPreview'] as String? ?? '',
      verseLabel: _parseVerseLabel(json['verseSnapshot']),
      likeCount: json['likeCount'] as int? ?? 0,
      commentCount: json['commentCount'] as int? ?? 0,
      likedByMe: json['likedByMe'] as bool? ?? false,
      bookmarkedByMe: json['bookmarkedByMe'] as bool? ?? false,
      publishedAt: json['publishedAt'] != null
          ? DateTime.parse(json['publishedAt'] as String)
          : null,
    );
  }

  /// 낙관적 업데이트용 — 좋아요/저장 상태·수만 바꾼 복제본.
  SharingPostItem copyWith({int? likeCount, bool? likedByMe, bool? bookmarkedByMe}) {
    return SharingPostItem(
      id: id,
      nicknameSnapshot: nicknameSnapshot,
      titleSnapshot: titleSnapshot,
      category: category,
      bodyPreview: bodyPreview,
      verseLabel: verseLabel,
      likeCount: likeCount ?? this.likeCount,
      commentCount: commentCount,
      likedByMe: likedByMe ?? this.likedByMe,
      bookmarkedByMe: bookmarkedByMe ?? this.bookmarkedByMe,
      publishedAt: publishedAt,
    );
  }
}

/// 나눔 피드 목록 응답.
class SharingPostListResponse {
  final List<SharingPostItem> items;
  final bool hasNext;

  SharingPostListResponse({required this.items, required this.hasNext});

  factory SharingPostListResponse.fromJson(Map<String, dynamic> json) {
    final content = json['content'] as List<dynamic>? ?? [];
    return SharingPostListResponse(
      items: content.map((e) => SharingPostItem.fromJson(e as Map<String, dynamic>)).toList(),
      hasNext: !(json['last'] as bool? ?? true),
    );
  }
}

/// 내 나눔 글 1건 (GET /api/v1/me/sharing-posts, 04 §4.4.5, 화면 M-05).
///
/// 공개 피드(SharingPostItem)와 달리 작성자 본인 관리용이라
/// 닉네임·본문 미리보기·likedByMe가 없고, status(공개/숨김)로 액션을 분기한다.
class MySharingPostItem {
  final int id;
  final String titleSnapshot;
  final String category;
  final String status; // PUBLISHED / HIDDEN
  final bool commentsEnabled;
  final DateTime? sourceNoteDeletedAt; // null 아니면 원본 노트 삭제됨
  final int likeCount;
  final int commentCount;
  final DateTime? publishedAt;

  MySharingPostItem({
    required this.id,
    required this.titleSnapshot,
    required this.category,
    required this.status,
    required this.commentsEnabled,
    this.sourceNoteDeletedAt,
    required this.likeCount,
    required this.commentCount,
    this.publishedAt,
  });

  bool get isHidden => status == 'HIDDEN';

  factory MySharingPostItem.fromJson(Map<String, dynamic> json) {
    return MySharingPostItem(
      id: json['id'] as int,
      titleSnapshot: json['titleSnapshot'] as String? ?? '',
      category: json['category'] as String? ?? '',
      status: json['status'] as String? ?? 'PUBLISHED',
      commentsEnabled: json['commentsEnabled'] as bool? ?? true,
      sourceNoteDeletedAt: json['sourceNoteDeletedAt'] != null
          ? DateTime.parse(json['sourceNoteDeletedAt'] as String)
          : null,
      likeCount: json['likeCount'] as int? ?? 0,
      commentCount: json['commentCount'] as int? ?? 0,
      publishedAt: json['publishedAt'] != null
          ? DateTime.parse(json['publishedAt'] as String)
          : null,
    );
  }
}

/// 내 나눔 목록 응답 (페이징).
class MySharingPostListResponse {
  final List<MySharingPostItem> items;
  final bool hasNext;

  MySharingPostListResponse({required this.items, required this.hasNext});

  factory MySharingPostListResponse.fromJson(Map<String, dynamic> json) {
    final content = json['content'] as List<dynamic>? ?? [];
    return MySharingPostListResponse(
      items: content
          .map((e) => MySharingPostItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      hasNext: !(json['last'] as bool? ?? true),
    );
  }
}

/// 나눔 댓글 1건 (GET /sharing-posts/{id}/comments, 04 §4.4.4).
class CommentItem {
  final int id;
  final String nickname;
  final String body;
  final bool ownedByMe; // 내 댓글이면 삭제 버튼 노출
  final DateTime? createdAt;

  CommentItem({
    required this.id,
    required this.nickname,
    required this.body,
    required this.ownedByMe,
    this.createdAt,
  });

  factory CommentItem.fromJson(Map<String, dynamic> json) {
    return CommentItem(
      id: json['id'] as int,
      nickname: json['nickname'] as String? ?? '',
      body: json['body'] as String? ?? '',
      ownedByMe: json['ownedByMe'] as bool? ?? false,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : null,
    );
  }
}

/// 나눔 글 상세 응답.
class SharingPostDetail {
  final int id;
  final int noteId;
  final int memberId;
  final String nicknameSnapshot;
  final String titleSnapshot;
  final String bodySnapshot;
  final String category;
  final bool commentsEnabled;
  final int likeCount;
  final int commentCount;
  final bool likedByMe;
  final bool bookmarkedByMe;
  final bool ownedByMe;
  final DateTime? publishedAt;

  SharingPostDetail({
    required this.id,
    required this.noteId,
    required this.memberId,
    required this.nicknameSnapshot,
    required this.titleSnapshot,
    required this.bodySnapshot,
    required this.category,
    required this.commentsEnabled,
    required this.likeCount,
    required this.commentCount,
    required this.likedByMe,
    this.bookmarkedByMe = false,
    required this.ownedByMe,
    this.publishedAt,
  });

  factory SharingPostDetail.fromJson(Map<String, dynamic> json) {
    return SharingPostDetail(
      id: json['id'] as int,
      noteId: json['noteId'] as int? ?? 0,
      memberId: json['memberId'] as int? ?? 0,
      nicknameSnapshot: json['nicknameSnapshot'] as String? ?? '',
      titleSnapshot: json['titleSnapshot'] as String? ?? '',
      bodySnapshot: json['bodySnapshot'] as String? ?? '',
      category: json['category'] as String? ?? '',
      commentsEnabled: json['commentsEnabled'] as bool? ?? true,
      likeCount: json['likeCount'] as int? ?? 0,
      commentCount: json['commentCount'] as int? ?? 0,
      likedByMe: json['likedByMe'] as bool? ?? false,
      bookmarkedByMe: json['bookmarkedByMe'] as bool? ?? false,
      ownedByMe: json['ownedByMe'] as bool? ?? false,
      publishedAt: json['publishedAt'] != null
          ? DateTime.parse(json['publishedAt'] as String)
          : null,
    );
  }

  /// 낙관적 업데이트용 — 좋아요/저장 상태·수만 바꾼 복제본.
  SharingPostDetail copyWith({int? likeCount, bool? likedByMe, bool? bookmarkedByMe}) {
    return SharingPostDetail(
      id: id,
      noteId: noteId,
      memberId: memberId,
      nicknameSnapshot: nicknameSnapshot,
      titleSnapshot: titleSnapshot,
      bodySnapshot: bodySnapshot,
      category: category,
      commentsEnabled: commentsEnabled,
      likeCount: likeCount ?? this.likeCount,
      commentCount: commentCount,
      likedByMe: likedByMe ?? this.likedByMe,
      bookmarkedByMe: bookmarkedByMe ?? this.bookmarkedByMe,
      ownedByMe: ownedByMe,
      publishedAt: publishedAt,
    );
  }
}

/// verseSnapshot.rangeLabel 을 안전하게 추출한다.
/// Map 이 아니거나 rangeLabel 이 없거나(또는 null) 공백뿐이면 null, 아니면 트림한 값.
String? _parseVerseLabel(dynamic verseSnapshot) {
  if (verseSnapshot is! Map) return null;
  final raw = verseSnapshot['rangeLabel'];
  if (raw is! String) return null;
  final trimmed = raw.trim();
  return trimmed.isEmpty ? null : trimmed;
}
