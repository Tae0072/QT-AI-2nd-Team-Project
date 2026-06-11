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
        name = "ai_event_outbox",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_event_outbox_event_id",
                columnNames = "event_id"
        )
)
class AiEventOutbox {

    private static final int ERROR_CODE_MAX_LENGTH = 100;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 80)
    private String eventId;

    @Column(name = "event_name", nullable = false, length = 120)
    private String eventName;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 120)
    private String aggregateId;

    @Column(name = "schema_version", nullable = false, length = 30)
    private String schemaVersion;

    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiEventOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error_code", length = ERROR_CODE_MAX_LENGTH)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String lastErrorMessage;

    @Column(name = "trace_id", length = 120)
    private String traceId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    protected AiEventOutbox() {
    }

    private AiEventOutbox(
            String eventId,
            String eventName,
            String aggregateType,
            String aggregateId,
            String schemaVersion,
            String payloadJson,
            String traceId,
            OffsetDateTime createdAt
    ) {
        this.eventId = requireText(eventId, "eventId");
        this.eventName = requireText(eventName, "eventName");
        this.aggregateType = requireText(aggregateType, "aggregateType");
        this.aggregateId = requireText(aggregateId, "aggregateId");
        this.schemaVersion = requireText(schemaVersion, "schemaVersion");
        this.payloadJson = AiJsonStorageGuard.rejectRawProviderOrReferenceText(
                requireText(payloadJson, "payloadJson"),
                "payloadJson"
        );
        this.traceId = traceId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.status = AiEventOutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public static AiEventOutbox create(
            String eventId,
            String eventName,
            String aggregateType,
            String aggregateId,
            String schemaVersion,
            String payloadJson,
            String traceId,
            OffsetDateTime createdAt
    ) {
        return new AiEventOutbox(
                eventId,
                eventName,
                aggregateType,
                aggregateId,
                schemaVersion,
                payloadJson,
                traceId,
                createdAt
        );
    }

    public void markPublished(OffsetDateTime publishedAt) {
        requireStatus(AiEventOutboxStatus.PUBLISHED, AiEventOutboxStatus.PENDING);
        this.status = AiEventOutboxStatus.PUBLISHED;
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void markFailed(String errorCode, String errorMessage) {
        requireStatus(AiEventOutboxStatus.FAILED, AiEventOutboxStatus.PENDING);
        this.status = AiEventOutboxStatus.FAILED;
        this.retryCount++;
        this.lastErrorCode = truncate(requireText(errorCode, "errorCode"), ERROR_CODE_MAX_LENGTH);
        this.lastErrorMessage = truncate(requireText(errorMessage, "errorMessage"), ERROR_MESSAGE_MAX_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public AiEventOutboxStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public String getTraceId() {
        return traceId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
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

    private void requireStatus(AiEventOutboxStatus nextStatus, AiEventOutboxStatus allowedCurrentStatus) {
        if (status == allowedCurrentStatus) {
            return;
        }
        throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "Invalid AI event outbox status transition: " + status + " -> " + nextStatus
        );
    }
}
