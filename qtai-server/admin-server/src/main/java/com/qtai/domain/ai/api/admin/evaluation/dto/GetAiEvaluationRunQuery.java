package com.qtai.domain.ai.api.admin.evaluation.dto;

public record GetAiEvaluationRunQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        Long runId
) {
}
