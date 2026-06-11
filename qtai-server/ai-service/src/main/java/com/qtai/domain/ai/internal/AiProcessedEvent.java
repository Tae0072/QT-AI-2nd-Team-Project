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

@Entity
@Table(
        name = "ai_processed_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_processed_events_event_handler",
                columnNames = {"event_id", "handler_name"}
        )
)
class AiProcessedEvent {

    private static final int ERROR_CODE_MAX_LENGTH = 100;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 80)
    private String eventId;

    @Column(name = "handler_name", nullable = false, length = 120)
    private String handlerName;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 120)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiProcessedEventStatus status;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    @Column(name = "last_error_code", length = ERROR_CODE_MAX_LENGTH)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String lastErrorMessage;

    protected AiProcessedEvent() {
    }

    private AiProcessedEvent(
            String eventId,
            String handlerName,
            String aggregateType,
            String aggregateId,
            AiProcessedEventStatus status,
            OffsetDateTime processedAt,
            String lastErrorCode,
            String lastErrorMessage
    ) {
        this.eventId = requireText(eventId, "eventId");
        this.handlerName = requireText(handlerName, "handlerName");
        this.aggregateType = requireText(aggregateType, "aggregateType");
        this.aggregateId = requireText(aggregateId, "aggregateId");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt must not be null");
        this.lastErrorCode = lastErrorCode == null ? null : truncate(requireText(lastErrorCode, "lastErrorCode"), ERROR_CODE_MAX_LENGTH);
        this.lastErrorMessage = lastErrorMessage == null ? null : truncate(
                requireText(lastErrorMessage, "lastErrorMessage"),
                ERROR_MESSAGE_MAX_LENGTH
        );
    }

    public static AiProcessedEvent succeeded(
            String eventId,
            String handlerName,
            String aggregateType,
            String aggregateId,
            OffsetDateTime processedAt
    ) {
        return new AiProcessedEvent(
                eventId,
                handlerName,
                aggregateType,
                aggregateId,
                AiProcessedEventStatus.SUCCEEDED,
                processedAt,
                null,
                null
        );
    }

    public static AiProcessedEvent failed(
            String eventId,
            String handlerName,
            String aggregateType,
            String aggregateId,
            String errorCode,
            String errorMessage,
            OffsetDateTime processedAt
    ) {
        return new AiProcessedEvent(
                eventId,
                handlerName,
                aggregateType,
                aggregateId,
                AiProcessedEventStatus.FAILED,
                processedAt,
                errorCode,
                errorMessage
        );
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public AiProcessedEventStatus getStatus() {
        return status;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
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
}
