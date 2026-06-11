package com.qtai.domain.ai.api.admin.monitoring.dto;

import java.time.OffsetDateTime;

public record AdminAiBatchRunLogItem(
        Long id,
        String batchName,
        String status,
        int createdCount,
        int failedCount,
        int processedCount,
        String errorType,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime createdAt
) {
}
