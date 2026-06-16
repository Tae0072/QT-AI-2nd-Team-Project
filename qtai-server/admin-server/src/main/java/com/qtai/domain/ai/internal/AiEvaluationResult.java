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
@Table(name = "ai_evaluation_results")
public class AiEvaluationResult {

    private static final int REASON_MAX_LENGTH = 1_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_run_id", nullable = false)
    private Long evaluationRunId;

    @Column(name = "evaluation_case_id", nullable = false)
    private Long evaluationCaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiEvaluationResultStatus result;

    @Column(length = REASON_MAX_LENGTH)
    private String reason;

    @Column(name = "output_summary_json", columnDefinition = "LONGTEXT")
    private String outputSummaryJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AiEvaluationResult() {
    }

    private AiEvaluationResult(
            Long evaluationRunId,
            Long evaluationCaseId,
            AiEvaluationResultStatus result,
            String reason,
            String outputSummaryJson,
            OffsetDateTime createdAt
    ) {
        this.evaluationRunId = requirePositive(evaluationRunId, "evaluationRunId");
        this.evaluationCaseId = requirePositive(evaluationCaseId, "evaluationCaseId");
        this.result = Objects.requireNonNull(result, "result must not be null");
        this.reason = truncate(blankToNull(reason), REASON_MAX_LENGTH);
        this.outputSummaryJson = outputSummaryJson;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static AiEvaluationResult create(
            Long evaluationRunId,
            Long evaluationCaseId,
            AiEvaluationResultStatus result,
            String reason,
            String outputSummaryJson,
            OffsetDateTime createdAt
    ) {
        return new AiEvaluationResult(evaluationRunId, evaluationCaseId, result, reason, outputSummaryJson, createdAt);
    }

    public Long getId() {
        return id;
    }

    public Long getEvaluationRunId() {
        return evaluationRunId;
    }

    public Long getEvaluationCaseId() {
        return evaluationCaseId;
    }

    public AiEvaluationResultStatus getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }

    public String getOutputSummaryJson() {
        return outputSummaryJson;
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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
