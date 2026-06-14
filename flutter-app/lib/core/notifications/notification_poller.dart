import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/providers/auth_providers.dart';
import '../../features/mypage/providers/mypage_providers.dart';
import 'local_notification_service.dart';

/// 서버 인앱 알림 → 기기 로컬 OS 알림(배너) 브리지.
///
/// 배경: v1에는 FCM 푸시가 없어, 관리자 알림 발송(NOTICE)이나 좋아요·댓글 알림은
/// 서버 `notifications` 테이블에만 쌓이고 기기 배너로는 뜨지 않았다. 이 폴러는 앱이
/// 떠 있는 동안 주기적으로 미읽음 알림을 조회해, 새로 도착한 항목을 기기 로컬 알림으로
/// 띄워 준다(앱이 백그라운드/종료면 OS가 폴링을 보장하지 않으므로 포그라운드 기준).
///
/// 정책:
/// - 로그인 상태에서만 동작한다.
/// - 첫 조회는 "기준선"만 잡고 배너를 띄우지 않는다(기존 미읽음 알림 폭주 방지).
/// - 이후 조회에서 마지막으로 본 id보다 큰 새 알림만 배너로 띄운다.
class NotificationPoller {
  NotificationPoller(this._ref);

  final Ref _ref;
  Timer? _timer;
  int _lastSeenId = 0;
  bool _primed = false; // 첫 폴에서 기준선만 잡았는지
  bool _busy = false;

  static const Duration _interval = Duration(seconds: 20);

  void start() {
    _timer ??= Timer.periodic(_interval, (_) => _tick());
    // 시작 즉시 1회 기준선 설정(로그인 상태면).
    unawaited(_tick());
  }

  Future<void> _tick() async {
    if (_busy) return;
    if (_ref.read(authStatusProvider) != AuthStatus.authenticated) return;

    // 설정에서 알림을 끈 경우 배너를 띄우지 않는다(값 미로딩 시에는 허용).
    final settings = _ref.read(settingsProvider).valueOrNull;
    if (settings != null && !settings.notificationEnabled) return;

    // 첫 알림 전에 OS 권한 다이얼로그가 뜨도록 권한을 보장한다(idempotent).
    await LocalNotificationService.instance.ensurePermission();

    _busy = true;
    try {
      final repo = _ref.read(myPageRepositoryProvider);
      final res = await repo.getNotifications(unreadOnly: true, page: 0);
      final items = res.items;
      if (items.isEmpty) return;

      final maxId = items.map((e) => e.id).reduce((a, b) => a > b ? a : b);

      // 첫 조회: 기존 알림을 배너로 쏟지 않도록 기준선만 잡는다.
      if (!_primed) {
        _lastSeenId = maxId;
        _primed = true;
        return;
      }

      final fresh = items.where((e) => e.id > _lastSeenId).toList()
        ..sort((a, b) => a.id.compareTo(b.id));
      if (fresh.isEmpty) return;

      if (fresh.length == 1) {
        final n = fresh.first;
        await LocalNotificationService.instance.show(
          title: n.title.isNotEmpty ? n.title : '새 알림',
          body: n.body,
        );
      } else {
        final last = fresh.last;
        await LocalNotificationService.instance.show(
          title: '새 알림 ${fresh.length}건',
          body: last.title.isNotEmpty ? last.title : last.body,
        );
      }
      _lastSeenId = maxId;
    } catch (e) {
      // 네트워크/일시 오류는 다음 주기에 다시 시도. 앱 흐름은 막지 않는다.
      debugPrint('[NotificationPoller] tick 실패: $e');
    } finally {
      _busy = false;
    }
  }

  void dispose() {
    _timer?.cancel();
    _timer = null;
  }
}

/// 폴러 수명 관리 provider. 메인 앱에서 watch하면 세션 동안 살아 있는다.
final notificationPollerProvider = Provider<NotificationPoller>((ref) {
  final poller = NotificationPoller(ref);
  ref.onDispose(poller.dispose);
  poller.start();
  return poller;
});
