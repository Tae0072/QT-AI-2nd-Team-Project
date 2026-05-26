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
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_validation_logs")
public class AiValidationLog {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 1_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ai_asset_id", nullable = false)
    private Long aiAssetId;

    @Column(name = "validation_reference_job_id")
    private Long validationReferenceJobId;

    @Column(nullable = false)
    private int layer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiValidationResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "reviewer_type", nullable = false, length = 30)
    private AiValidationReviewerType reviewerType;

    @Column(name = "checklist_version_id")
    private Long checklistVersionId;

    @Lob
    @Column(name = "checklist_json")
    private String checklistJson;

    @Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AiValidationLog() {
    }

    private AiValidationLog(
            Long aiAssetId,
            Long validationReferenceJobId,
            int layer,
            AiValidationResult result,
            AiValidationReviewerType reviewerType,
            Long checklistVersionId,
            String checklistJson,
            String errorMessage,
            OffsetDateTime createdAt
    ) {
        if (layer < 1) {
            throw new IllegalArgumentException("layer must be greater than zero");
        }
        this.aiAssetId = Objects.requireNonNull(aiAssetId, "aiAssetId must not be null");
        this.validationReferenceJobId = requirePositiveWhenPresent(
                validationReferenceJobId,
                "validationReferenceJobId"
        );
        this.layer = layer;
        this.result = Objects.requireNonNull(result, "result must not be null");
        this.reviewerType = Objects.requireNonNull(reviewerType, "reviewerType must not be null");
        this.checklistVersionId = checklistVersionId;
        this.checklistJson = AiJsonStorageGuard.rejectRawProviderOrReferenceText(checklistJson, "checklistJson");
        this.errorMessage = normalizeErrorMessage(errorMessage);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static AiValidationLog create(
            Long aiAssetId,
            Long validationReferenceJobId,
            int layer,
            AiValidationResult result,
            AiValidationReviewerType reviewerType,
            Long checklistVersionId,
            String checklistJson,
            String errorMessage,
            OffsetDateTime createdAt
    ) {
        return new AiValidationLog(
                aiAssetId,
                validationReferenceJobId,
                layer,
                result,
                reviewerType,
                checklistVersionId,
                checklistJson,
                errorMessage,
                createdAt
        );
    }

    public Long getId() {
        return id;
    }

    public Long getAiAssetId() {
        return aiAssetId;
    }

    public Long getValidationReferenceJobId() {
        return validationReferenceJobId;
    }

    public int getLayer() {
        return layer;
    }

    public AiValidationResult getResult() {
        return result;
    }

    public AiValidationReviewerType getReviewerType() {
        return reviewerType;
    }

    public Long getChecklistVersionId() {
        return checklistVersionId;
    }

    public String getChecklistJson() {
        return checklistJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    private static String normalizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        if (errorMessage.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }

    private static Long requirePositiveWhenPresent(Long value, String fieldName) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
