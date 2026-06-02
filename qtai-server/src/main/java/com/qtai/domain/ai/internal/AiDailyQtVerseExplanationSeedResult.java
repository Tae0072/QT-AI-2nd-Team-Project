package com.qtai.domain.ai.internal;

record AiDailyQtVerseExplanationSeedResult(
        int createdCount,
        int failedCount,
        String failureReason
) {

    AiDailyQtVerseExplanationSeedResult(int createdCount, int failedCount) {
        this(createdCount, failedCount, null);
    }

    boolean hasFailureReason() {
        return failureReason != null && !failureReason.isBlank();
    }
}
