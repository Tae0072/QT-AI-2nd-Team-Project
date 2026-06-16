package com.qtai.domain.ai.api.admin.evaluation.dto;

public record CreateAiEvaluationAssetCandidateCommand(
        Long adminId,
        String memberRole,
        String adminRole,
        Long evaluationSetId,
        Long assetId,
        String expectedPolicyJson
) {
}
