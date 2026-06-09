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
@Table(name = "validation_reference_jobs")
class ValidationReferenceJob {

    private static final int SOURCE_NAME_MAX_LENGTH = 150;
    private static final int SOURCE_FILE_NAME_MAX_LENGTH = 255;
    private static final int SOURCE_FILE_HASH_MAX_LENGTH = 100;
    private static final int URI_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name", nullable = false, length = SOURCE_NAME_MAX_LENGTH)
    private String sourceName;

    @Column(name = "source_file_name", nullable = false, length = SOURCE_FILE_NAME_MAX_LENGTH)
    private String sourceFileName;

    @Column(name = "source_file_hash", nullable = false, length = SOURCE_FILE_HASH_MAX_LENGTH)
    private String sourceFileHash;

    @Column(name = "storage_uri", length = URI_MAX_LENGTH)
    private String storageUri;

    @Column(name = "index_storage_uri", length = URI_MAX_LENGTH)
    private String indexStorageUri;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValidationReferenceJobStatus status;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected ValidationReferenceJob() {
    }

    private ValidationReferenceJob(
            String sourceName,
            String sourceFileName,
            String sourceFileHash,
            String storageUri,
            String indexStorageUri,
            OffsetDateTime expiresAt,
            OffsetDateTime createdAt
    ) {
        this.sourceName = requireText(sourceName, "sourceName", SOURCE_NAME_MAX_LENGTH);
        this.sourceFileName = requireText(sourceFileName, "sourceFileName", SOURCE_FILE_NAME_MAX_LENGTH);
        this.sourceFileHash = requireText(sourceFileHash, "sourceFileHash", SOURCE_FILE_HASH_MAX_LENGTH);
        this.storageUri = requireLengthWhenPresent(storageUri, "storageUri", URI_MAX_LENGTH);
        this.indexStorageUri = requireLengthWhenPresent(indexStorageUri, "indexStorageUri", URI_MAX_LENGTH);
        this.expiresAt = expiresAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = createdAt;
        this.status = ValidationReferenceJobStatus.ACTIVE;
    }

    public static ValidationReferenceJob create(
            String sourceName,
            String sourceFileName,
            String sourceFileHash,
            String storageUri,
            String indexStorageUri,
            OffsetDateTime expiresAt,
            OffsetDateTime createdAt
    ) {
        return new ValidationReferenceJob(
                sourceName,
                sourceFileName,
                sourceFileHash,
                storageUri,
                indexStorageUri,
                expiresAt,
                createdAt
        );
    }

    public void expire(OffsetDateTime updatedAt) {
        requireTransition(ValidationReferenceJobStatus.EXPIRED, ValidationReferenceJobStatus.ACTIVE);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.status = ValidationReferenceJobStatus.EXPIRED;
    }

    void markDeleted(OffsetDateTime deletedAt) {
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
        this.updatedAt = deletedAt;
        this.status = ValidationReferenceJobStatus.DELETED;
    }

    public Long getId() {
        return id;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public String getSourceFileHash() {
        return sourceFileHash;
    }

    public String getStorageUri() {
        return storageUri;
    }

    public String getIndexStorageUri() {
        return indexStorageUri;
    }

    public ValidationReferenceJobStatus getStatus() {
        return status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return requireLengthWhenPresent(value, fieldName, maxLength);
    }

    private static String requireLengthWhenPresent(String value, String fieldName, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " length must be less than or equal to " + maxLength);
        }
        return value;
    }

    private void requireTransition(
            ValidationReferenceJobStatus nextStatus,
            ValidationReferenceJobStatus... allowedCurrentStatuses
    ) {
        for (ValidationReferenceJobStatus allowedCurrentStatus : allowedCurrentStatuses) {
            if (status == allowedCurrentStatus) {
                return;
            }
        }
        throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "Invalid validation reference job status transition: " + status + " -> " + nextStatus
        );
    }
}
