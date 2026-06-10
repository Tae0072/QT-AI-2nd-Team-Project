package com.qtai.domain.notification.api.dto;

import java.time.LocalDateTime;

public record AdminNoticePublishResponse(
        Long id,
        String status,
        LocalDateTime publishedAt,
        NotificationResult notificationResult
) {

    public record NotificationResult(
            long requestedCount,
            long createdCount,
            long failedCount
    ) {
    }
}
