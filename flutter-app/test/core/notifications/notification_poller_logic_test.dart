import 'package:flutter_test/flutter_test.dart';
import 'package:qtai_app/core/notifications/notification_poller.dart';
import 'package:qtai_app/features/mypage/models/notification_response.dart';

/// [decideNotificationsToShow] 순수 로직 검증 — 폴러의 "무엇을 띄울지" 판단부.
void main() {
  NotificationItem item(int id) => NotificationItem(
        id: id,
        type: 'NOTICE',
        title: '알림 $id',
        body: '본문 $id',
        read: false,
        createdAt: DateTime(2026, 6, 19),
      );

  group('decideNotificationsToShow', () {
    test('미읽음이 비면 띄울 것이 없고 기준선만 유지한다', () {
      final d = decideNotificationsToShow(
        unreadItems: const [],
        storedLastSeenId: 7,
      );
      expect(d.toShow, isEmpty);
      expect(d.prime, isFalse);
      expect(d.newLastSeenId, 7);
    });

    test('최초 실행(저장값 없음)은 기준선만 잡고 배너를 생략한다(prime)', () {
      final d = decideNotificationsToShow(
        unreadItems: [item(3), item(5), item(4)],
        storedLastSeenId: null,
      );
      expect(d.prime, isTrue);
      expect(d.toShow, isEmpty);
      expect(d.newLastSeenId, 5); // maxId
    });

    test('저장 id보다 큰 새 알림만 id 오름차순으로 모두 띄운다', () {
      final d = decideNotificationsToShow(
        unreadItems: [item(7), item(5), item(6), item(4)],
        storedLastSeenId: 5,
      );
      expect(d.prime, isFalse);
      expect(d.toShow.map((e) => e.id).toList(), [6, 7]);
      expect(d.newLastSeenId, 7);
    });

    test('새 알림이 없으면(모두 저장 id 이하) 저장 id를 그대로 유지한다', () {
      final d = decideNotificationsToShow(
        unreadItems: [item(4), item(5)],
        storedLastSeenId: 10,
      );
      expect(d.prime, isFalse);
      expect(d.toShow, isEmpty);
      expect(d.newLastSeenId, 10);
    });

    test('앱 재시작 후에도 저장 id 덕분에 그 사이 도착한 알림을 누락하지 않는다', () {
      // 저장 id=2 인 상태에서 앱을 껐다 켠 뒤 3,4번 알림이 도착해 있던 상황.
      final d = decideNotificationsToShow(
        unreadItems: [item(2), item(3), item(4)],
        storedLastSeenId: 2,
      );
      expect(d.toShow.map((e) => e.id).toList(), [3, 4]);
      expect(d.newLastSeenId, 4);
    });
  });
}
