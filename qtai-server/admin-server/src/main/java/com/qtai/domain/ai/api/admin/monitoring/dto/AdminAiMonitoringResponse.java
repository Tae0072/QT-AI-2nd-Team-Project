package com.qtai.domain.ai.api.admin.monitoring.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminAiMonitoringResponse(
        Period period,
        GenerationJobs generationJobs,
        Validation validation,
        BatchRuns batchRuns,
        Qa qa,
        List<Checklist> checklists
) {

    public record Period(
            LocalDate from,
            LocalDate to,
            String timezone
    ) {
    }

    public record GenerationJobs(
            long queued,
            long running,
            long succeeded,
            long failed
    ) {
    }

    public record Validation(
            long waitingAssets,
            long approvedAssets,
            long rejectedAssets,
            long hiddenAssets,
            long passCount,
            long failCount,
            long needsReviewCount,
            List<FailureReason> failureReasons
    ) {
        public Validation(
                long waitingAssets,
                long passCount,
                long failCount,
                long needsReviewCount,
                List<FailureReason> failureReasons
        ) {
            this(waitingAssets, 0, 0, 0, passCount, failCount, needsReviewCount, failureReasons);
        }
    }

    public record FailureReason(
            String resultCode,
            long count
    ) {
    }

    public record BatchRuns(
            long succeeded,
            long partialFailed,
            long failed,
            List<BatchRunFailure> latestFailures
    ) {
    }

    public record BatchRunFailure(
            Long id,
            String batchName,
            String status,
            String errorType,
            String errorMessage,
            OffsetDateTime createdAt
    ) {
    }

    public record Qa(
            long requested,
            long answered,
            long blocked,
            long failed,
            List<BlockedReason> blockedReasons
    ) {
    }

    public record BlockedReason(
            String blockedReason,
            long count
    ) {
    }

    public record Checklist(
            String checklistType,
            String activeVersion,
            double passRate
    ) {
    }
}
