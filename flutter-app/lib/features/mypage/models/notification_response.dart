/// 알림 응답 모델.
class NotificationItem {
  final int id;
  final String type;
  final String title;
  final String body;
  final bool read;
  final DateTime createdAt;

  NotificationItem({
    required this.id,
    required this.type,
    required this.title,
    required this.body,
    required this.read,
    required this.createdAt,
  });

  String get message => title.isNotEmpty ? title : body;

  factory NotificationItem.fromJson(Map<String, dynamic> json) {
    final title = json['title'] as String?;
    final body = json['body'] as String?;
    final legacyMessage = json['message'] as String?;

    return NotificationItem(
      id: _parseInt(json['id']),
      type: json['type'] as String? ?? '',
      title: title ?? legacyMessage ?? '',
      body: body ?? '',
      read: json['read'] as bool? ?? json['readAt'] != null,
      createdAt: _parseServerDateTime(json['createdAt']),
    );
  }

  static int _parseInt(Object? value) {
    if (value is int) {
      return value;
    }
    if (value is num) {
      return value.toInt();
    }
    if (value is String) {
      return int.tryParse(value) ?? 0;
    }
    return 0;
  }

  static DateTime _parseServerDateTime(Object? rawValue) {
    if (rawValue is! String || rawValue.trim().isEmpty) {
      return _fallbackCreatedAt();
    }

    final parsed = DateTime.tryParse(rawValue);
    if (parsed == null) {
      return _fallbackCreatedAt();
    }
    if (parsed.isUtc ||
        rawValue.endsWith('Z') ||
        rawValue.contains(RegExp(r'[+-]\d\d:\d\d$'))) {
      return parsed.toLocal();
    }

    // Server DTO currently serializes LocalDateTime without an offset.
    // Treat that value as Asia/Seoul time so GMT emulators do not read it as a future local time.
    return DateTime.utc(
      parsed.year,
      parsed.month,
      parsed.day,
      parsed.hour - 9,
      parsed.minute,
      parsed.second,
      parsed.millisecond,
      parsed.microsecond,
    ).toLocal();
  }

  static DateTime _fallbackCreatedAt() =>
      DateTime.fromMillisecondsSinceEpoch(0, isUtc: true).toLocal();
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
      items: content
          .map((e) => NotificationItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      totalElements: json['totalElements'] as int? ?? 0,
      hasNext: !(json['last'] as bool? ?? true),
    );
  }
}
