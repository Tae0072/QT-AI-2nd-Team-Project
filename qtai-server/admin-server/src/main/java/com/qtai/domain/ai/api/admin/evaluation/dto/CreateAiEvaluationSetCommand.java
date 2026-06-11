package com.qtai.domain.ai.api.admin.evaluation.dto;

public record CreateAiEvaluationSetCommand(
        Long adminId,
        String memberRole,
        String adminRole,
        String name,
        String evalType,
        String version,
        String targetType,
        String expectedPolicyJson,
        String description,
        String status
) {
}
