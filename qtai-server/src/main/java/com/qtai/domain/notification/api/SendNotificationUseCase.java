package com.qtai.domain.notification.api;

/**
 * 알림 발송 UseCase 포트.
 *
 * 호출자: 타 도메인 Service (예: praise → 칭찬 받은 회원에게 알림) 또는 배치 작업.
 * 발송 채널: 내부 알림 행 INSERT (앱 내 알림 센터용) + 선택적으로 푸시(FCM 등).
 */
public interface SendNotificationUseCase {

    // TODO: void send(NotificationSendRequest request);
    //       동기 INSERT 후 푸시는 @Async — 푸시 실패가 비즈니스 로직을 막지 않도록
}
