package com.qtai.domain.notification.api.dto;

import com.qtai.domain.notification.internal.Notification;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO.
 *
 * API 명세서 §4.6.3 기준.
 */
public record NotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        String linkType,
        Long linkId,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getLinkType(),
                notification.getLinkId(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
