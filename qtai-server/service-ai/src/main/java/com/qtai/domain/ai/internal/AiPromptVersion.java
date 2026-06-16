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
            JSON 객체만 반환하세요. 객체는 explanations[]와 glossaryTerms[]를 반드시 포함해야 합니다.
            각 explanation 항목은 verseId, summary, explanation을 포함해야 합니다.
            각 glossaryTerms 항목은 verseId, term, meaning을 포함해야 합니다.
            제공된 verseId만 사용하고, 차분하고 사실 기반이며 초심자에게 친절한 어조를 유지하세요.
            provider raw response, prompt text, validation reference text, secrets, private data를 포함하지 마세요.
            """;

    private static final String DEFAULT_USER_PROMPT_TEMPLATE = """
            초심자도 이해할 수 있게 차분하고 사실 기반으로 설명하세요.
            summary는 한 문장으로 간결하게 작성하고, explanation은 제공된 본문과 참고 자료 안에서만 작성하세요.
            용어 설명은 본문 이해에 필요한 핵심 단어만 포함하세요.
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
        this.systemPrompt = defaultIfBlank(systemPrompt, defaultSystemPrompt());
        this.userPromptTemplate = defaultIfBlank(userPromptTemplate, defaultUserPromptTemplate());
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
                defaultSystemPrompt(),
                defaultUserPromptTemplate(),
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
        return defaultIfBlank(systemPrompt, defaultSystemPrompt());
    }

    public String getUserPromptTemplate() {
        return defaultIfBlank(userPromptTemplate, defaultUserPromptTemplate());
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

    static String defaultSystemPrompt() {
        return DEFAULT_SYSTEM_PROMPT;
    }

    static String defaultUserPromptTemplate() {
        return DEFAULT_USER_PROMPT_TEMPLATE;
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
