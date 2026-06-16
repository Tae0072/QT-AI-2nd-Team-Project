package com.qtai.domain.ai.api.admin.evaluation.dto;

public record CreateAiEvaluationCaseCommand(
        Long adminId,
        String memberRole,
        String adminRole,
        Long evaluationSetId,
        String targetType,
        Long targetId,
        String sourceType,
        Long sourceId,
        String inputJson,
        String expectedOutputJson,
        String expectedPolicyJson,
        String status
) {
}
