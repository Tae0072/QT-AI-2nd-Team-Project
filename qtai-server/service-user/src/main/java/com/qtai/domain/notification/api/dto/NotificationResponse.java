package com.qtai.domain.notification.api.dto;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO.
 *
 * <p>API 명세서 §4.6.3 기준.
 * <p>도메인 경계 정책: api/dto 는 internal 패키지를 import 하지 않는다.
 */
public record NotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        String linkType,
        Long linkId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}
