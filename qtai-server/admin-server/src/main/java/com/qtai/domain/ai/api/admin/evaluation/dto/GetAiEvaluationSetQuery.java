package com.qtai.domain.ai.api.admin.evaluation.dto;

public record GetAiEvaluationSetQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        Long setId
) {
}
