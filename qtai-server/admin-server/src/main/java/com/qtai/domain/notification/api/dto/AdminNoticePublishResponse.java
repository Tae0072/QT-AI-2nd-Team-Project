package com.qtai.domain.notification.api.dto;

import java.time.LocalDateTime;

public record AdminNoticePublishResponse(
        Long id,
        Long noticeId,
        String status,
        LocalDateTime publishedAt,
        NotificationResult notificationResult
) {

    public AdminNoticePublishResponse(
            Long id,
            String status,
            LocalDateTime publishedAt,
            NotificationResult notificationResult
    ) {
        this(id, id, status, publishedAt, notificationResult);
    }

    public record NotificationResult(
            long requestedCount,
            long targetMemberCount,
            long createdCount,
            long queuedCount,
            long failedCount
    ) {
        public NotificationResult(long requestedCount, long createdCount, long failedCount) {
            this(requestedCount, requestedCount, createdCount, createdCount, failedCount);
        }
    }
}
