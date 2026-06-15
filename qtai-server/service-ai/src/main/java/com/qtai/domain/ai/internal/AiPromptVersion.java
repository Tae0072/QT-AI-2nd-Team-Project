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
@Table(name = "ai_prompt_versions")
public class AiPromptVersion {

    static final int DEFAULT_MAX_TOKENS = 2_000;
    static final double DEFAULT_TEMPERATURE = 0.2;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            Return only a JSON object. The object must contain explanations[] and glossaryTerms[].
            Each explanation item must contain verseId, summary, and explanation.
            Each glossary term item must contain verseId, term, and meaning.
            Use only the provided verseIds and keep the tone calm, factual, and beginner-friendly.
            Do not include provider raw response, prompt text, validation reference text, secrets, or private data.
            """;

    private static final String DEFAULT_USER_PROMPT_TEMPLATE = """
            Create explanation JSON for the following Bible verses.
            Target type: {{targetType}}
            Target id: {{targetId}}
            {{qtPassageBlock}}Verses:
            {{versesBlock}}{{commentaryBlock}}
            """;

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

    @Column(name = "system_prompt", columnDefinition = "LONGTEXT")
    private String systemPrompt;

    @Column(name = "user_prompt_template", columnDefinition = "LONGTEXT")
    private String userPromptTemplate;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column
    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(length = 500)
    private String description;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "retired_at")
    private OffsetDateTime retiredAt;

    protected AiPromptVersion() {
    }

    private AiPromptVersion(
            Long id,
            AiPromptType promptType,
            String version,
            String contentHash,
            AiPromptVersionStatus status,
            String systemPrompt,
            String userPromptTemplate,
            String modelName,
            Double temperature,
            Integer maxTokens,
            String description,
            Long createdByAdminId,
            OffsetDateTime createdAt,
            OffsetDateTime activatedAt,
            OffsetDateTime retiredAt
    ) {
        if (id != null) {
            this.id = requirePositive(id, "id");
        }
        this.promptType = Objects.requireNonNull(promptType, "promptType must not be null");
        this.version = requireText(version, "version");
        this.contentHash = requireText(contentHash, "contentHash");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.systemPrompt = defaultIfBlank(systemPrompt, DEFAULT_SYSTEM_PROMPT);
        this.userPromptTemplate = defaultIfBlank(userPromptTemplate, DEFAULT_USER_PROMPT_TEMPLATE);
        this.modelName = blankToNull(modelName);
        this.temperature = temperature == null ? DEFAULT_TEMPERATURE : temperature;
        this.maxTokens = maxTokens == null ? DEFAULT_MAX_TOKENS : requirePositive(maxTokens, "maxTokens");
        this.description = blankToNull(description);
        this.createdByAdminId = createdByAdminId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.activatedAt = activatedAt;
        this.retiredAt = retiredAt;
    }

    public static AiPromptVersion of(
            Long id,
            AiPromptType promptType,
            String version,
            String contentHash,
            AiPromptVersionStatus status,
            OffsetDateTime createdAt
    ) {
        return new AiPromptVersion(
                id,
                promptType,
                version,
                contentHash,
                status,
                DEFAULT_SYSTEM_PROMPT,
                DEFAULT_USER_PROMPT_TEMPLATE,
                null,
                DEFAULT_TEMPERATURE,
                DEFAULT_MAX_TOKENS,
                null,
                null,
                createdAt,
                status == AiPromptVersionStatus.ACTIVE ? createdAt : null,
                null
        );
    }

    public static AiPromptVersion of(
            Long id,
            AiPromptType promptType,
            String version,
            String contentHash,
            AiPromptVersionStatus status,
            String systemPrompt,
            String userPromptTemplate,
            String modelName,
            Double temperature,
            Integer maxTokens,
            String description,
            Long createdByAdminId,
            OffsetDateTime createdAt,
            OffsetDateTime activatedAt,
            OffsetDateTime retiredAt
    ) {
        return new AiPromptVersion(
                id,
                promptType,
                version,
                contentHash,
                status,
                systemPrompt,
                userPromptTemplate,
                modelName,
                temperature,
                maxTokens,
                description,
                createdByAdminId,
                createdAt,
                activatedAt,
                retiredAt
        );
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

    public String getSystemPrompt() {
        return defaultIfBlank(systemPrompt, DEFAULT_SYSTEM_PROMPT);
    }

    public String getUserPromptTemplate() {
        return defaultIfBlank(userPromptTemplate, DEFAULT_USER_PROMPT_TEMPLATE);
    }

    public String getModelName() {
        return modelName;
    }

    public Double getTemperature() {
        return temperature == null ? DEFAULT_TEMPERATURE : temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens == null ? DEFAULT_MAX_TOKENS : maxTokens;
    }

    public String getDescription() {
        return description;
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

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static Integer requirePositive(Integer value, String fieldName) {
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

    private static String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
