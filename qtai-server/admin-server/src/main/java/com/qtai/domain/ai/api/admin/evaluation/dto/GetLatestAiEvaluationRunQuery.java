package com.qtai.domain.ai.api.admin.evaluation.dto;

public record GetLatestAiEvaluationRunQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        Long evaluationSetId
) {
}
