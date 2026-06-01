/// 나눔 피드 목록 아이템.
class SharingPostItem {
  final int id;
  final String nicknameSnapshot;
  final String titleSnapshot;
  final String category;
  final String bodyPreview;
  final int likeCount;
  final int commentCount;
  final bool likedByMe;
  final DateTime? publishedAt;

  SharingPostItem({
    required this.id,
    required this.nicknameSnapshot,
    required this.titleSnapshot,
    required this.category,
    required this.bodyPreview,
    required this.likeCount,
    required this.commentCount,
    required this.likedByMe,
    this.publishedAt,
  });

  factory SharingPostItem.fromJson(Map<String, dynamic> json) {
    return SharingPostItem(
      id: json['id'] as int,
      nicknameSnapshot: json['nicknameSnapshot'] as String? ?? '',
      titleSnapshot: json['titleSnapshot'] as String? ?? '',
      category: json['category'] as String? ?? '',
      bodyPreview: json['bodyPreview'] as String? ?? '',
      likeCount: json['likeCount'] as int? ?? 0,
      commentCount: json['commentCount'] as int? ?? 0,
      likedByMe: json['likedByMe'] as bool? ?? false,
      publishedAt: json['publishedAt'] != null
          ? DateTime.parse(json['publishedAt'] as String)
          : null,
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
      ownedByMe: json['ownedByMe'] as bool? ?? false,
      publishedAt: json['publishedAt'] != null
          ? DateTime.parse(json['publishedAt'] as String)
          : null,
    );
  }
}
