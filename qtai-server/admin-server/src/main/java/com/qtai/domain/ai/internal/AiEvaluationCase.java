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
@Table(name = "ai_evaluation_cases")
public class AiEvaluationCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_set_id", nullable = false)
    private Long evaluationSetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private AiTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private AiEvaluationSourceType sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "input_json", nullable = false, columnDefinition = "LONGTEXT")
    private String inputJson;

    @Column(name = "expected_output_json", columnDefinition = "LONGTEXT")
    private String expectedOutputJson;

    @Column(name = "expected_policy_json", columnDefinition = "LONGTEXT")
    private String expectedPolicyJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiEvaluationCaseStatus status;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AiEvaluationCase() {
    }

    private AiEvaluationCase(
            Long evaluationSetId,
            AiTargetType targetType,
            Long targetId,
            AiEvaluationSourceType sourceType,
            Long sourceId,
            String inputJson,
            String expectedOutputJson,
            String expectedPolicyJson,
            OffsetDateTime createdAt
    ) {
        this.evaluationSetId = requirePositive(evaluationSetId, "evaluationSetId");
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetId = targetId;
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.sourceId = sourceId;
        this.inputJson = requireText(inputJson, "inputJson");
        this.expectedOutputJson = expectedOutputJson;
        this.expectedPolicyJson = expectedPolicyJson;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.status = AiEvaluationCaseStatus.CANDIDATE;
    }

    public static AiEvaluationCase create(
            Long evaluationSetId,
            AiTargetType targetType,
            Long targetId,
            AiEvaluationSourceType sourceType,
            Long sourceId,
            String inputJson,
            String expectedOutputJson,
            String expectedPolicyJson,
            OffsetDateTime createdAt
    ) {
        return new AiEvaluationCase(
                evaluationSetId,
                targetType,
                targetId,
                sourceType,
                sourceId,
                inputJson,
                expectedOutputJson,
                expectedPolicyJson,
                createdAt
        );
    }

    public void approve(Long adminId, OffsetDateTime reviewedAt) {
        review(AiEvaluationCaseStatus.APPROVED, adminId, reviewedAt);
    }

    public void reject(Long adminId, OffsetDateTime reviewedAt) {
        review(AiEvaluationCaseStatus.REJECTED, adminId, reviewedAt);
    }

    private void review(AiEvaluationCaseStatus nextStatus, Long adminId, OffsetDateTime reviewedAt) {
        if (status != AiEvaluationCaseStatus.CANDIDATE) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = nextStatus;
        this.reviewedByAdminId = requirePositive(adminId, "adminId");
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
    }

    public Long getId() {
        return id;
    }

    public Long getEvaluationSetId() {
        return evaluationSetId;
    }

    public AiTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public AiEvaluationSourceType getSourceType() {
        return sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getInputJson() {
        return inputJson;
    }

    public String getExpectedOutputJson() {
        return expectedOutputJson;
    }

    public String getExpectedPolicyJson() {
        return expectedPolicyJson;
    }

    public AiEvaluationCaseStatus getStatus() {
        return status;
    }

    public Long getReviewedByAdminId() {
        return reviewedByAdminId;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
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
