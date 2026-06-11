package com.qtai.domain.ai.api.admin.evaluation.dto;

public record GetAiEvaluationCaseQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        Long caseId
) {
}
