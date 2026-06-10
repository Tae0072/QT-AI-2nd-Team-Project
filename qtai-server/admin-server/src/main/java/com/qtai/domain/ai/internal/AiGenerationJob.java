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
import jakarta.persistence.UniqueConstraint;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

@Entity
@Table(
        name = "ai_generation_jobs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ai_generation_jobs_active_target_prompt",
                        columnNames = {"job_type", "target_type", "target_id", "prompt_version_id",
                                "active_unique_key"}
                ),
                @UniqueConstraint(
                        name = "uk_ai_generation_jobs_active_target",
                        columnNames = {"job_type", "target_type", "target_id", "active_unique_key"}
                )
        }
)
public class AiGenerationJob {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 1_000;
    private static final String ACTIVE_UNIQUE_KEY = "ACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 40)
    private AiGenerationJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private AiTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "prompt_version_id", nullable = false)
    private Long promptVersionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiGenerationJobStatus status;

    @Column(name = "active_unique_key", length = 20)
    private String activeUniqueKey;

    @Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    protected AiGenerationJob() {
    }

    private AiGenerationJob(
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId,
            Long promptVersionId,
            OffsetDateTime createdAt
    ) {
        this.jobType = Objects.requireNonNull(jobType, "jobType must not be null");
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetId = requirePositive(targetId, "targetId");
        this.promptVersionId = requirePositive(promptVersionId, "promptVersionId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.status = AiGenerationJobStatus.QUEUED;
        this.activeUniqueKey = ACTIVE_UNIQUE_KEY;
    }

    public static AiGenerationJob queue(
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId,
            Long promptVersionId,
            OffsetDateTime createdAt
    ) {
        return new AiGenerationJob(jobType, targetType, targetId, promptVersionId, createdAt);
    }

    public void markRunning(OffsetDateTime startedAt) {
        requireTransition(AiGenerationJobStatus.RUNNING, AiGenerationJobStatus.QUEUED);
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.status = AiGenerationJobStatus.RUNNING;
    }

    public void markSucceeded(OffsetDateTime finishedAt) {
        requireTransition(AiGenerationJobStatus.SUCCEEDED, AiGenerationJobStatus.RUNNING);
        this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt must not be null");
        this.status = AiGenerationJobStatus.SUCCEEDED;
        this.activeUniqueKey = null;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage, OffsetDateTime finishedAt) {
        requireTransition(AiGenerationJobStatus.FAILED, AiGenerationJobStatus.QUEUED, AiGenerationJobStatus.RUNNING);
        this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt must not be null");
        this.status = AiGenerationJobStatus.FAILED;
        this.activeUniqueKey = null;
        this.errorMessage = truncate(requireText(errorMessage, "errorMessage"), ERROR_MESSAGE_MAX_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public AiGenerationJobType getJobType() {
        return jobType;
    }

    public AiTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Long getPromptVersionId() {
        return promptVersionId;
    }

    public AiGenerationJobStatus getStatus() {
        return status;
    }

    public String getActiveUniqueKey() {
        return activeUniqueKey;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void requireTransition(AiGenerationJobStatus nextStatus, AiGenerationJobStatus... allowedCurrentStatuses) {
        for (AiGenerationJobStatus allowedCurrentStatus : allowedCurrentStatuses) {
            if (status == allowedCurrentStatus) {
                return;
            }
        }
        throw new BusinessException(
                ErrorCode.INVALID_INPUT,
                "Invalid AI generation job status transition: " + status + " -> " + nextStatus
        );
    }
}
