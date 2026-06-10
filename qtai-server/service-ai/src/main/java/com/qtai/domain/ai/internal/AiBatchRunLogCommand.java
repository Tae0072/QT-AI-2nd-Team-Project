package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;
import java.util.Objects;

record AiBatchRunLogCommand(
        AiBatchName batchName,
        AiBatchRunStatus status,
        int createdCount,
        int failedCount,
        int processedCount,
        String errorType,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {

    AiBatchRunLogCommand {
        Objects.requireNonNull(batchName, "batchName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        finishedAt = Objects.requireNonNull(finishedAt, "finishedAt must not be null");
        requireNonNegative(createdCount, "createdCount");
        requireNonNegative(failedCount, "failedCount");
        requireNonNegative(processedCount, "processedCount");
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}
