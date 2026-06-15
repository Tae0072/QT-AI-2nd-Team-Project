package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

@Entity
@Table(name = "ai_evaluation_runs")
public class AiEvaluationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_set_id", nullable = false)
    private Long evaluationSetId;

    @Column(name = "prompt_version_id", nullable = false)
    private Long promptVersionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiEvaluationRunStatus status;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "passed_count", nullable = false)
    private int passedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "needs_review_count", nullable = false)
    private int needsReviewCount;

    @Column(name = "requested_by_admin_id", nullable = false)
    private Long requestedByAdminId;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AiEvaluationRun() {
    }

    private AiEvaluationRun(
            Long evaluationSetId,
            Long promptVersionId,
            Long requestedByAdminId,
            OffsetDateTime startedAt
    ) {
        this.evaluationSetId = requirePositive(evaluationSetId, "evaluationSetId");
        this.promptVersionId = requirePositive(promptVersionId, "promptVersionId");
        this.requestedByAdminId = requirePositive(requestedByAdminId, "requestedByAdminId");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.createdAt = startedAt;
        this.status = AiEvaluationRunStatus.RUNNING;
    }

    public static AiEvaluationRun start(
            Long evaluationSetId,
            Long promptVersionId,
            Long requestedByAdminId,
            OffsetDateTime startedAt
    ) {
        return new AiEvaluationRun(evaluationSetId, promptVersionId, requestedByAdminId, startedAt);
    }

    public void finish(int totalCount, int passedCount, int failedCount, int needsReviewCount, OffsetDateTime finishedAt) {
        if (status != AiEvaluationRunStatus.RUNNING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.totalCount = requireNonNegative(totalCount, "totalCount");
        this.passedCount = requireNonNegative(passedCount, "passedCount");
        this.failedCount = requireNonNegative(failedCount, "failedCount");
        this.needsReviewCount = requireNonNegative(needsReviewCount, "needsReviewCount");
        this.status = AiEvaluationRunStatus.SUCCEEDED;
        this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt must not be null");
    }

    public void fail(OffsetDateTime finishedAt) {
        if (status != AiEvaluationRunStatus.RUNNING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = AiEvaluationRunStatus.FAILED;
        this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt must not be null");
    }

    public Long getId() {
        return id;
    }

    public Long getEvaluationSetId() {
        return evaluationSetId;
    }

    public Long getPromptVersionId() {
        return promptVersionId;
    }

    public AiEvaluationRunStatus getStatus() {
        return status;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getNeedsReviewCount() {
        return needsReviewCount;
    }

    public Long getRequestedByAdminId() {
        return requestedByAdminId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }
}
