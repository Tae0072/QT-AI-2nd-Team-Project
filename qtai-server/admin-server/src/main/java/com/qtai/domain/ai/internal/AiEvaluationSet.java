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
@Table(name = "ai_evaluation_sets")
public class AiEvaluationSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "eval_type", nullable = false, length = 30)
    private AiEvaluationType evalType;

    @Column(nullable = false, length = 30)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private AiTargetType targetType;

    @Column(name = "expected_policy_json", columnDefinition = "LONGTEXT")
    private String expectedPolicyJson;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiEvaluationSetStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "retired_at")
    private OffsetDateTime retiredAt;

    protected AiEvaluationSet() {
    }

    private AiEvaluationSet(
            String name,
            AiEvaluationType evalType,
            String version,
            AiTargetType targetType,
            String expectedPolicyJson,
            String description,
            OffsetDateTime createdAt
    ) {
        this.name = requireText(name, "name");
        this.evalType = Objects.requireNonNull(evalType, "evalType must not be null");
        this.version = requireText(version, "version");
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.expectedPolicyJson = expectedPolicyJson;
        this.description = description;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.status = AiEvaluationSetStatus.DRAFT;
    }

    public static AiEvaluationSet create(
            String name,
            AiEvaluationType evalType,
            String version,
            AiTargetType targetType,
            String expectedPolicyJson,
            String description,
            OffsetDateTime createdAt
    ) {
        return new AiEvaluationSet(name, evalType, version, targetType, expectedPolicyJson, description, createdAt);
    }

    public void activate(OffsetDateTime activatedAt) {
        requireStatus(AiEvaluationSetStatus.DRAFT);
        this.status = AiEvaluationSetStatus.ACTIVE;
        this.activatedAt = Objects.requireNonNull(activatedAt, "activatedAt must not be null");
    }

    public void retire(OffsetDateTime retiredAt) {
        if (status == AiEvaluationSetStatus.RETIRED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = AiEvaluationSetStatus.RETIRED;
        this.retiredAt = Objects.requireNonNull(retiredAt, "retiredAt must not be null");
    }

    private void requireStatus(AiEvaluationSetStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AiEvaluationType getEvalType() {
        return evalType;
    }

    public String getVersion() {
        return version;
    }

    public AiTargetType getTargetType() {
        return targetType;
    }

    public String getExpectedPolicyJson() {
        return expectedPolicyJson;
    }

    public String getDescription() {
        return description;
    }

    public AiEvaluationSetStatus getStatus() {
        return status;
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
}
