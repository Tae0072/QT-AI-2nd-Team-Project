package com.qtai.domain.ai.api.admin.evaluation.dto;

public record ListAiEvaluationCasesQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        Long evaluationSetId,
        String status,
        int page,
        int size
) {
}
