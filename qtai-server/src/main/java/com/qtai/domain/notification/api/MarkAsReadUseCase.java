package com.qtai.domain.notification.api;

/**
 * 알림 읽음 처리 UseCase 포트.
 *
 * 단건 또는 전체 일괄 읽음 처리. 수신자 본인만 호출 가능.
 */
public interface MarkAsReadUseCase {

    // TODO: void markAsRead(Long memberId, Long notificationId);
    // TODO: int markAllAsRead(Long memberId);    — 일괄 읽음, 영향받은 행 수 반환
}
