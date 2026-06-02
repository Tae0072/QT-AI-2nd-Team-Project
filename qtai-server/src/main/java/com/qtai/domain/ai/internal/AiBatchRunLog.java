package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "ai_batch_run_logs",
        indexes = {
                @Index(name = "idx_ai_batch_run_logs_batch_created", columnList = "batch_name, created_at"),
                @Index(name = "idx_ai_batch_run_logs_status_created", columnList = "status, created_at")
        }
)
class AiBatchRunLog {

    private static final int ERROR_TYPE_MAX_LENGTH = 100;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1_000;
    private static final String REDACTED_ERROR_MESSAGE = "REDACTED_SENSITIVE_ERROR_MESSAGE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "batch_name", nullable = false, length = 80)
    private AiBatchName batchName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiBatchRunStatus status;

    @Column(name = "created_count", nullable = false)
    private int createdCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "processed_count", nullable = false)
    private int processedCount;

    @Column(name = "error_type", length = ERROR_TYPE_MAX_LENGTH)
    private String errorType;

    @Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private OffsetDateTime finishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AiBatchRunLog() {
    }

    private AiBatchRunLog(AiBatchRunLogCommand command) {
        this.batchName = Objects.requireNonNull(command.batchName(), "batchName must not be null");
        this.status = Objects.requireNonNull(command.status(), "status must not be null");
        this.createdCount = command.createdCount();
        this.failedCount = command.failedCount();
        this.processedCount = command.processedCount();
        this.errorType = truncate(blankToNull(command.errorType()), ERROR_TYPE_MAX_LENGTH);
        this.errorMessage = normalizeErrorMessage(command.errorMessage());
        this.startedAt = Objects.requireNonNull(command.startedAt(), "startedAt must not be null");
        this.finishedAt = Objects.requireNonNull(command.finishedAt(), "finishedAt must not be null");
        this.createdAt = this.finishedAt;
    }

    static AiBatchRunLog create(AiBatchRunLogCommand command) {
        return new AiBatchRunLog(command);
    }

    Long getId() {
        return id;
    }

    AiBatchName getBatchName() {
        return batchName;
    }

    AiBatchRunStatus getStatus() {
        return status;
    }

    int getCreatedCount() {
        return createdCount;
    }

    int getFailedCount() {
        return failedCount;
    }

    int getProcessedCount() {
        return processedCount;
    }

    String getErrorType() {
        return errorType;
    }

    String getErrorMessage() {
        return errorMessage;
    }

    OffsetDateTime getStartedAt() {
        return startedAt;
    }

    OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    private static String normalizeErrorMessage(String errorMessage) {
        String normalized = blankToNull(errorMessage);
        if (normalized == null) {
            return null;
        }
        if (containsSensitiveKeyword(normalized)) {
            return REDACTED_ERROR_MESSAGE;
        }
        return truncate(normalized, ERROR_MESSAGE_MAX_LENGTH);
    }

    private static boolean containsSensitiveKeyword(String value) {
        String lowerValue = value.toLowerCase(Locale.ROOT);
        return lowerValue.contains("password")
                || lowerValue.contains("token")
                || lowerValue.contains("secret")
                || lowerValue.contains("private key")
                || lowerValue.contains("authorization")
                || lowerValue.contains("api-key")
                || lowerValue.contains("apikey");
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
