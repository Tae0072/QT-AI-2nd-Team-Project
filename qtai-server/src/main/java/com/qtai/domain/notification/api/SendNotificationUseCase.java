package com.qtai.domain.notification.api;

import com.qtai.domain.notification.api.dto.NotificationSendRequest;

/**
 * 알림 발송 UseCase 포트.
 *
 * 호출자: 타 도메인 Service (좋아요, 댓글, 신고 결과 등) 또는 배치 작업.
 * 동기 INSERT 후 푸시는 @Async — 푸시 실패가 비즈니스 로직을 막지 않도록.
 */
public interface SendNotificationUseCase {

    void send(NotificationSendRequest request);
}
