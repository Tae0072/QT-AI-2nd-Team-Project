package com.qtai.domain.ai.api.admin.evaluation.dto;

public record ListAiEvaluationSetsQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        String evalType,
        String targetType,
        String status,
        int page,
        int size
) {
}
