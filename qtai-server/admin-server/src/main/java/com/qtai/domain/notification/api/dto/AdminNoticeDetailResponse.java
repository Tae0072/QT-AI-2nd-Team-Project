package com.qtai.domain.notification.api.dto;

import java.time.LocalDateTime;

public record AdminNoticeDetailResponse(
        Long id,
        String title,
        String body,
        String status,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
