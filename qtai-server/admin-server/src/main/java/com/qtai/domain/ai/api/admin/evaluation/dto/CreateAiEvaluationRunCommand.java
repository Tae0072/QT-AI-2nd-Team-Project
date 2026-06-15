package com.qtai.domain.ai.api.admin.evaluation.dto;

public record CreateAiEvaluationRunCommand(
        Long adminId,
        String memberRole,
        String adminRole,
        Long evaluationSetId,
        Long promptVersionId
) {
}
