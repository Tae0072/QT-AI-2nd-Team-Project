package com.qtai.domain.notification.api.dto;

/**
 * 알림 발송 요청 DTO.
 *
 * 호출자: 타 도메인 Service (좋아요, 댓글, 신고 결과 등).
 */
public record NotificationSendRequest(
        Long memberId,
        String type,
        String title,
        String body,
        Long noticeId,
        String linkType,
        Long linkId,
        String eventKey
) {}
