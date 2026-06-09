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
        name = "ai_validation_checklist_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_checklist_type_version",
                columnNames = {"checklist_type", "version"}
        )
)
class AiValidationChecklistVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "checklist_type", nullable = false, length = 30)
    private AiValidationChecklistType checklistType;

    @Column(nullable = false, length = 30)
    private String version;

    @Column(name = "content_hash", nullable = false, length = 100)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiValidationChecklistStatus status;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "retired_at")
    private OffsetDateTime retiredAt;

    protected AiValidationChecklistVersion() {
    }

    private AiValidationChecklistVersion(
            AiValidationChecklistType checklistType,
            String version,
            String contentHash,
            Long createdByAdminId,
            OffsetDateTime createdAt
    ) {
        this.checklistType = Objects.requireNonNull(checklistType, "checklistType must not be null");
        this.version = requireText(version, "version");
        this.contentHash = requireText(contentHash, "contentHash");
        this.createdByAdminId = createdByAdminId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.status = AiValidationChecklistStatus.DRAFT;
    }

    public static AiValidationChecklistVersion create(
            AiValidationChecklistType checklistType,
            String version,
            String contentHash,
            Long createdByAdminId,
            OffsetDateTime createdAt
    ) {
        return new AiValidationChecklistVersion(checklistType, version, contentHash, createdByAdminId, createdAt);
    }

    public void activate(OffsetDateTime activatedAt) {
        requireTransition(AiValidationChecklistStatus.ACTIVE, AiValidationChecklistStatus.DRAFT);
        this.activatedAt = Objects.requireNonNull(activatedAt, "activatedAt must not be null");
        this.retiredAt = null;
        this.status = AiValidationChecklistStatus.ACTIVE;
    }

    public void retire(OffsetDateTime retiredAt) {
        requireTransition(AiValidationChecklistStatus.RETIRED, AiValidationChecklistStatus.ACTIVE);
        this.retiredAt = Objects.requireNonNull(retiredAt, "retiredAt must not be null");
        this.status = AiValidationChecklistStatus.RETIRED;
    }

    public Long getId() {
        return id;
    }

    public AiValidationChecklistType getChecklistType() {
        return checklistType;
    }

    public String getVersion() {
        return version;
    }

    public String getContentHash() {
        return contentHash;
    }

    public AiValidationChecklistStatus getStatus() {
        return status;
    }

    public Long getCreatedByAdminId() {
        return createdByAdminId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public OffsetDateTime getRetiredAt() {
        return retiredAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private void requireTransition(
            AiValidationChecklistStatus nextStatus,
            AiValidationChecklistStatus... allowedCurrentStatuses
    ) {
        for (AiValidationChecklistStatus allowedCurrentStatus : allowedCurrentStatuses) {
            if (status == allowedCurrentStatus) {
                return;
            }
        }
        throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "Invalid AI validation checklist status transition: " + status + " -> " + nextStatus
        );
    }
}
