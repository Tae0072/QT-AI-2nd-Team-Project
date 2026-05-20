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

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

@Entity
@Table(name = "ai_generated_assets")
public class AiGeneratedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "generation_job_id", nullable = false)
    private Long generationJobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 40)
    private AiGeneratedAssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private AiTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "prompt_version", nullable = false, length = 80)
    private String promptVersion;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "source_label", length = 255)
    private String sourceLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiGeneratedAssetStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    protected AiGeneratedAsset() {
    }

    private AiGeneratedAsset(
            Long generationJobId,
            AiGeneratedAssetType assetType,
            AiTargetType targetType,
            Long targetId,
            String promptVersion,
            String payloadJson,
            String sourceLabel,
            OffsetDateTime createdAt
    ) {
        this.generationJobId = Objects.requireNonNull(generationJobId, "generationJobId must not be null");
        this.assetType = Objects.requireNonNull(assetType, "assetType must not be null");
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetId = Objects.requireNonNull(targetId, "targetId must not be null");
        this.promptVersion = requireText(promptVersion, "promptVersion");
        this.payloadJson = AiJsonStorageGuard.rejectRawProviderOrReferenceText(
                requireText(payloadJson, "payloadJson"),
                "payloadJson"
        );
        this.sourceLabel = sourceLabel;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.status = AiGeneratedAssetStatus.VALIDATING;
    }

    public static AiGeneratedAsset create(
            Long generationJobId,
            AiGeneratedAssetType assetType,
            AiTargetType targetType,
            Long targetId,
            String promptVersion,
            String payloadJson,
            String sourceLabel,
            OffsetDateTime createdAt
    ) {
        return new AiGeneratedAsset(
                generationJobId,
                assetType,
                targetType,
                targetId,
                promptVersion,
                payloadJson,
                sourceLabel,
                createdAt
        );
    }

    public void approve(OffsetDateTime reviewedAt) {
        requireTransition(AiGeneratedAssetStatus.APPROVED, AiGeneratedAssetStatus.VALIDATING);
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
        this.status = AiGeneratedAssetStatus.APPROVED;
    }

    public void reject(OffsetDateTime reviewedAt) {
        requireTransition(AiGeneratedAssetStatus.REJECTED, AiGeneratedAssetStatus.VALIDATING);
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
        this.status = AiGeneratedAssetStatus.REJECTED;
    }

    public void hide(OffsetDateTime reviewedAt) {
        requireTransition(AiGeneratedAssetStatus.HIDDEN, AiGeneratedAssetStatus.VALIDATING, AiGeneratedAssetStatus.APPROVED);
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
        this.status = AiGeneratedAssetStatus.HIDDEN;
    }

    public Long getId() {
        return id;
    }

    public Long getGenerationJobId() {
        return generationJobId;
    }

    public AiGeneratedAssetType getAssetType() {
        return assetType;
    }

    public AiTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public AiGeneratedAssetStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private void requireTransition(AiGeneratedAssetStatus nextStatus, AiGeneratedAssetStatus... allowedCurrentStatuses) {
        for (AiGeneratedAssetStatus allowedCurrentStatus : allowedCurrentStatuses) {
            if (status == allowedCurrentStatus) {
                return;
            }
        }
        throw new BusinessException(
                ErrorCode.INVALID_INPUT,
                "Invalid AI generated asset status transition: " + status + " -> " + nextStatus
        );
    }
}
