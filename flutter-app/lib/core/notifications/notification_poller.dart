import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/providers/auth_providers.dart';
import '../../features/mypage/models/notification_response.dart';
import '../../features/mypage/providers/mypage_providers.dart';
import '../../features/onboarding/providers/onboarding_providers.dart'
    show sharedPreferencesProvider;
import 'local_notification_service.dart';

/// 폴 1회의 결과로 무엇을 기기 알림으로 띄울지 결정하는 순수 로직.
///
/// 타이머·플러그인·저장소 같은 부수효과 없이 입력만으로 결과를 계산하므로
/// 단위 테스트가 쉽다(see test/core/notifications/notification_poller_logic_test.dart).
class PollDecision {
  const PollDecision({
    required this.toShow,
    required this.newLastSeenId,
    required this.prime,
  });

  /// 개별 배너로 띄울 새 알림(id 오름차순).
  final List<NotificationItem> toShow;

  /// 다음 폴까지 기억할 "마지막으로 처리한 알림 id".
  final int newLastSeenId;

  /// 최초 실행이라 기준선만 잡고 배너는 생략하는지 여부.
  final bool prime;
}

/// 미읽음 알림 목록과 "기기에 저장된 마지막 본 id"로, 새로 띄울 알림을 결정한다.
///
/// 정책:
/// - 목록이 비면 띄울 것 없음(저장 id 유지).
/// - [storedLastSeenId]가 null이면 = 기기에 저장값이 없는 **최초 실행**.
///   기존 미읽음 알림을 한꺼번에 쏟지 않도록 기준선(maxId)만 잡고 배너는 생략한다(prime).
/// - 그 외에는 저장 id보다 큰 알림만 새 알림으로 보고, id 오름차순으로 모두 띄운다.
///   (앱을 껐다 켠 사이 도착한 알림도 저장 id 덕분에 누락되지 않는다.)
PollDecision decideNotificationsToShow({
  required List<NotificationItem> unreadItems,
  required int? storedLastSeenId,
}) {
  if (unreadItems.isEmpty) {
    return PollDecision(
      toShow: const [],
      newLastSeenId: storedLastSeenId ?? 0,
      prime: false,
    );
  }

  final maxId =
      unreadItems.map((e) => e.id).reduce((a, b) => a > b ? a : b);

  // 최초 실행: 과거 미읽음 폭주 방지로 기준선만 잡는다.
  if (storedLastSeenId == null) {
    return PollDecision(toShow: const [], newLastSeenId: maxId, prime: true);
  }

  final fresh = unreadItems.where((e) => e.id > storedLastSeenId).toList()
    ..sort((a, b) => a.id.compareTo(b.id));

  return PollDecision(
    toShow: fresh,
    newLastSeenId: fresh.isEmpty ? storedLastSeenId : maxId,
    prime: false,
  );
}

/// 서버 인앱 알림 → 기기 로컬 OS 알림(배너) 브리지.
///
/// 배경: v1에는 FCM 푸시가 없어, 관리자 공지(NOTICE)·좋아요·댓글·신고 처리 결과 등
/// F-05 알림은 서버 `notifications` 테이블에만 쌓이고 기기 배너로는 뜨지 않았다. 이 폴러는
/// 앱이 떠 있는 동안 주기적으로 미읽음 알림을 조회해, 새로 도착한 항목을 기기 로컬 알림으로
/// 띄워 준다. (앱이 백그라운드/종료면 OS가 폴링을 보장하지 않으므로 포그라운드 기준이며,
/// 실시간 FCM 푸시는 v1 범위 밖이다 — F-05/Lead 결정 2026-06-19.)
///
/// 정책:
/// - 로그인 + 설정 "알림 수신" ON 상태에서만 동작한다.
/// - "마지막으로 처리한 알림 id"를 기기(SharedPreferences)에 저장해, 앱을 껐다 켜도
///   그 사이 도착한 새 알림을 누락 없이 띄운다.
/// - 최초 실행(저장값 없음) 1회만 기준선을 잡아 과거 알림 폭주를 막는다.
/// - 새 알림은 개별 배너로 띄운다(한 번에 너무 많으면 상한 + 요약).
class NotificationPoller {
  NotificationPoller(this._ref);

  final Ref _ref;
  Timer? _timer;
  bool _busy = false;

  static const Duration _interval = Duration(seconds: 20);

  /// 기기 로컬에 저장하는 "마지막으로 처리한 알림 id" 키.
  static const String _lastSeenKey = 'notif_last_seen_id';

  /// 한 번의 폴에서 개별 배너로 띄우는 최대 개수. 초과분은 요약 한 건으로 묶는다.
  static const int _maxIndividual = 5;

  void start() {
    _timer ??= Timer.periodic(_interval, (_) => _tick());
    // 시작 즉시 1회 실행(로그인 상태면).
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

      final prefs = _ref.read(sharedPreferencesProvider);
      // getInt이 null이면 = 기기에 저장값 없음 = 최초 실행.
      final stored = prefs.getInt(_lastSeenKey);

      final decision = decideNotificationsToShow(
        unreadItems: res.items,
        storedLastSeenId: stored,
      );

      // 최초 실행: 기준선만 저장하고 배너 생략.
      if (decision.prime) {
        await prefs.setInt(_lastSeenKey, decision.newLastSeenId);
        return;
      }

      if (decision.toShow.isNotEmpty) {
        await _showAll(decision.toShow);
      }
      // 처리한 지점을 기기에 저장(앱 재시작에도 유지). 값이 바뀔 때만 기록해
      // 새 알림이 없는 매 주기마다 같은 값을 덮어쓰는 낭비를 막는다.
      if (decision.newLastSeenId != stored) {
        await prefs.setInt(_lastSeenKey, decision.newLastSeenId);
      }
    } catch (e) {
      // 네트워크/일시 오류는 다음 주기에 다시 시도. 앱 흐름은 막지 않는다.
      debugPrint('[NotificationPoller] tick 실패: $e');
    } finally {
      _busy = false;
    }
  }

  /// 새 알림을 개별 배너로 띄운다. [_maxIndividual]를 넘으면 초과분은 요약 한 건으로 묶는다.
  Future<void> _showAll(List<NotificationItem> fresh) async {
    final individual =
        fresh.length <= _maxIndividual ? fresh : fresh.sublist(0, _maxIndividual);
    for (final n in individual) {
      await LocalNotificationService.instance.show(
        title: n.title.isNotEmpty ? n.title : '새 알림',
        body: n.body,
      );
    }

    final overflow = fresh.length - individual.length;
    if (overflow > 0) {
      await LocalNotificationService.instance.show(
        title: '새 알림 $overflow건 더',
        body: '마이페이지 알림 목록에서 확인하세요.',
      );
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
