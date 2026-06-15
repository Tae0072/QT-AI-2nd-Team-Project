package com.qtai.domain.ai.api.admin.evaluation.dto;

import java.time.OffsetDateTime;

public record AiEvaluationRunResultResponse(
        Long id,
        Long evaluationCaseId,
        String result,
        String reason,
        String outputSummaryJson,
        OffsetDateTime createdAt
) {
}
