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

@Entity
@Table(name = "ai_prompt_versions")
class AiPromptVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "prompt_type", nullable = false, length = 30)
    private AiPromptType promptType;

    @Column(nullable = false, length = 30)
    private String version;

    @Column(name = "content_hash", nullable = false, length = 100)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiPromptVersionStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AiPromptVersion() {
    }

    private AiPromptVersion(
            Long id,
            AiPromptType promptType,
            String version,
            String contentHash,
            AiPromptVersionStatus status,
            OffsetDateTime createdAt
    ) {
        this.id = requirePositive(id, "id");
        this.promptType = Objects.requireNonNull(promptType, "promptType must not be null");
        this.version = requireText(version, "version");
        this.contentHash = requireText(contentHash, "contentHash");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static AiPromptVersion of(
            Long id,
            AiPromptType promptType,
            String version,
            String contentHash,
            AiPromptVersionStatus status,
            OffsetDateTime createdAt
    ) {
        return new AiPromptVersion(id, promptType, version, contentHash, status, createdAt);
    }

    public Long getId() {
        return id;
    }

    public AiPromptType getPromptType() {
        return promptType;
    }

    public String getVersion() {
        return version;
    }

    public String getContentHash() {
        return contentHash;
    }

    public AiPromptVersionStatus getStatus() {
        return status;
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

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
