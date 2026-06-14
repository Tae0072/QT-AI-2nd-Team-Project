import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

/// 기기 OS 알림(상단 알림 표시줄)을 띄우는 로컬 알림 서비스.
///
/// - 개발자 모드의 "알림 보내기" 테스트와, 좋아요/댓글 등 활동 알림에 공용으로 쓴다.
/// - 첫 호출 시 1회 초기화(+Android13 권한 요청, 채널 생성)한다.
/// - 플러그인/권한 문제로 실패해도 예외를 삼켜 앱 흐름을 막지 않는다(best-effort).
class LocalNotificationService {
  LocalNotificationService._();
  static final LocalNotificationService instance = LocalNotificationService._();

  final FlutterLocalNotificationsPlugin _plugin =
      FlutterLocalNotificationsPlugin();
  bool _initialized = false;
  int _nextId = 0;

  static const String _channelId = 'qtai_activity';
  static const String _channelName = '활동 알림';
  static const String _channelDesc = '좋아요·댓글 등 내 나눔 활동 알림';

  Future<bool> _ensureInitialized() async {
    if (_initialized) return true;
    // 플러그인/권한/플랫폼 미초기화(특히 테스트 환경)에서도 절대 throw하지 않는다.
    try {
      const android = AndroidInitializationSettings('@mipmap/ic_launcher');
      const darwin = DarwinInitializationSettings(
        requestAlertPermission: true,
        requestBadgePermission: true,
        requestSoundPermission: true,
      );
      await _plugin.initialize(
        const InitializationSettings(android: android, iOS: darwin),
      );

      final android13 = _plugin.resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin>();
      // Android 13+ 알림 권한 요청 + 채널 생성.
      await android13?.requestNotificationsPermission();
      await android13?.createNotificationChannel(
        const AndroidNotificationChannel(
          _channelId,
          _channelName,
          description: _channelDesc,
          importance: Importance.high,
        ),
      );

      // iOS 권한 요청(이미 초기화에서 요청하지만 명시적으로 한 번 더).
      final ios = _plugin.resolvePlatformSpecificImplementation<
          IOSFlutterLocalNotificationsPlugin>();
      await ios?.requestPermissions(alert: true, badge: true, sound: true);

      _initialized = true;
      return true;
    } catch (e) {
      debugPrint('[LocalNotification] init 실패: $e');
      return false;
    }
  }

  /// 기기 알림 권한을 보장한다(첫 1회 초기화 + Android13/iOS 권한 요청).
  ///
  /// 설정에서 알림을 켤 때나 폴러 시작 시 호출해, 첫 알림 전에 OS 권한 다이얼로그가
  /// 뜨도록 한다. 여러 번 불러도 초기화는 1회만 수행한다(idempotent).
  Future<bool> ensurePermission() => _ensureInitialized();

  /// 즉시 알림 1건을 띄운다. 성공하면 true.
  Future<bool> show({required String title, required String body}) async {
    try {
      await _ensureInitialized();
      const details = NotificationDetails(
        android: AndroidNotificationDetails(
          _channelId,
          _channelName,
          channelDescription: _channelDesc,
          importance: Importance.high,
          priority: Priority.high,
        ),
        iOS: DarwinNotificationDetails(),
      );
      await _plugin.show(_nextId++, title, body, details);
      return true;
    } catch (e) {
      debugPrint('[LocalNotification] show 실패: $e');
      return false;
    }
  }

  // ── 활동 알림 프리셋(좋아요/댓글/마일스톤) ─────────────────────────────
  Future<bool> showLike({String? from}) => show(
        title: '좋아요',
        body: '${from ?? '누군가'}님이 회원님의 나눔을 좋아합니다.',
      );

  Future<bool> showComment({String? from}) => show(
        title: '새 댓글',
        body: '${from ?? '누군가'}님이 회원님의 나눔에 댓글을 남겼습니다.',
      );

  /// 좋아요/댓글 누적이 100·1000 등 마일스톤을 넘었을 때의 추가 알림.
  Future<bool> showMilestone({required int count}) => show(
        title: '🎉 $count 돌파!',
        body: '회원님의 나눔이 $count개의 반응을 받았어요.',
      );
}
