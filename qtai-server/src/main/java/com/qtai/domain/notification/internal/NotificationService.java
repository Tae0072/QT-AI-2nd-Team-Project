package com.qtai.domain.notification.internal;

/**
 * 알림 도메인 진입점. 3개 UseCase 구현 + 트랜잭션 경계.
 *
 * 발송 실패 정책: 푸시 전송 실패는 swallow + 로그만 (비즈니스 흐름 보호).
 * DB INSERT 실패는 전파 — 호출자가 인지하고 재시도.
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements SendNotificationUseCase, ListNotificationUseCase, MarkAsReadUseCase
public class NotificationService {

    // TODO: final NotificationRepository notificationRepository;
    // TODO: final GetMemberUseCase getMemberUseCase;
    // TODO: 푸시 클라이언트 (FCM 등) — 도입 시 추가

    // TODO: @Transactional send(request) — 수신자 검증(WITHDRAWN 차단) → INSERT → @Async 푸시
    // TODO: listMy(memberId, unreadOnly, pageable) 구현
    // TODO: @Transactional markAsRead(memberId, notificationId) — 본인 알림 검증 후 read=true
    // TODO: @Transactional markAllAsRead(memberId) — 일괄 update, 영향 행 수 반환
}
