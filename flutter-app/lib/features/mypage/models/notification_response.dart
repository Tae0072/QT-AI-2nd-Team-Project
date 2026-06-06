/// 알림 응답 모델.
class NotificationItem {
  final int id;
  final String type;
  final String message;
  final bool read;
  final DateTime createdAt;

  NotificationItem({
    required this.id,
    required this.type,
    required this.message,
    required this.read,
    required this.createdAt,
  });

  factory NotificationItem.fromJson(Map<String, dynamic> json) {
    return NotificationItem(
      id: json['id'] as int,
      type: json['type'] as String? ?? '',
      message: json['message'] as String? ?? '',
      read: json['readAt'] != null,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}

/// 알림 목록 응답.
class NotificationListResponse {
  final List<NotificationItem> items;
  final int totalElements;
  final bool hasNext;

  NotificationListResponse({
    required this.items,
    required this.totalElements,
    required this.hasNext,
  });

  factory NotificationListResponse.fromJson(Map<String, dynamic> json) {
    final content = json['content'] as List<dynamic>? ?? [];
    return NotificationListResponse(
      items: content.map((e) => NotificationItem.fromJson(e as Map<String, dynamic>)).toList(),
      totalElements: json['totalElements'] as int? ?? 0,
      hasNext: !(json['last'] as bool? ?? true),
    );
  }
}
