import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/features/mypage/models/notification_response.dart';

void main() {
  group('NotificationItem.fromJson', () {
    test('서버 계약 title/body/read 필드를 파싱한다', () {
      final item = NotificationItem.fromJson({
        'id': 10,
        'type': 'NOTICE',
        'title': '공지 알림',
        'body': '오늘의 공지가 등록되었습니다.',
        'read': false,
        'readAt': null,
        'createdAt': '2026-06-12T10:00:00',
      });

      expect(item.id, 10);
      expect(item.type, 'NOTICE');
      expect(item.title, '공지 알림');
      expect(item.body, '오늘의 공지가 등록되었습니다.');
      expect(item.message, '공지 알림');
      expect(item.read, isFalse);
      expect(item.createdAt, DateTime.utc(2026, 6, 12, 1).toLocal());
    });

    test('read 필드가 없으면 readAt으로 읽음 상태를 판단한다', () {
      final item = NotificationItem.fromJson({
        'id': 11,
        'type': 'REPORT_RESULT',
        'title': '신고 처리 완료',
        'body': '',
        'readAt': '2026-06-12T10:01:00',
        'createdAt': '2026-06-12T10:00:00',
      });

      expect(item.read, isTrue);
    });

    test('구버전 message 응답도 제목 fallback으로 사용한다', () {
      final item = NotificationItem.fromJson({
        'id': 12,
        'type': 'NOTICE',
        'message': '레거시 알림',
        'readAt': null,
        'createdAt': '2026-06-12T10:00:00',
      });

      expect(item.title, '레거시 알림');
      expect(item.message, '레거시 알림');
      expect(item.body, isEmpty);
      expect(item.read, isFalse);
    });

    test('timezone 없는 서버 시간은 KST 기준으로 해석한다', () {
      final item = NotificationItem.fromJson({
        'id': 13,
        'type': 'NOTICE',
        'title': '시간 알림',
        'body': '',
        'read': false,
        'createdAt': '2026-06-12T19:52:00',
      });

      expect(item.createdAt.toUtc(), DateTime.utc(2026, 6, 12, 10, 52));
    });

    test('id와 createdAt이 누락되어도 기본값으로 파싱한다', () {
      final item = NotificationItem.fromJson({
        'type': 'NOTICE',
        'title': '기본값 알림',
      });

      expect(item.id, 0);
      expect(item.createdAt,
          DateTime.fromMillisecondsSinceEpoch(0, isUtc: true).toLocal());
    });

    test('숫자 문자열 id를 허용하고 잘못된 createdAt은 기본값으로 파싱한다', () {
      final item = NotificationItem.fromJson({
        'id': '15',
        'type': 'NOTICE',
        'title': '잘못된 시간 알림',
        'createdAt': 'not-a-date',
      });

      expect(item.id, 15);
      expect(item.createdAt,
          DateTime.fromMillisecondsSinceEpoch(0, isUtc: true).toLocal());
    });
  });

  group('NotificationListResponse.fromJson', () {
    test('페이지 content를 NotificationItem 목록으로 변환한다', () {
      final response = NotificationListResponse.fromJson({
        'content': [
          {
            'id': 10,
            'type': 'NOTICE',
            'title': '공지 알림',
            'body': '본문',
            'read': false,
            'createdAt': '2026-06-12T10:00:00',
          },
        ],
        'totalElements': 1,
        'last': false,
      });

      expect(response.items, hasLength(1));
      expect(response.items.first.title, '공지 알림');
      expect(response.totalElements, 1);
      expect(response.hasNext, isTrue);
    });
  });
}
