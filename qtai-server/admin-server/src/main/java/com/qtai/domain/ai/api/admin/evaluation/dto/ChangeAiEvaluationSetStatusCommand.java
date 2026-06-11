package com.qtai.domain.ai.api.admin.evaluation.dto;

public record ChangeAiEvaluationSetStatusCommand(
        Long adminId,
        String memberRole,
        String adminRole,
        Long setId
) {
}
