package com.qtai.domain.ai.api.admin.evaluation.dto;

import java.time.OffsetDateTime;

public record AiEvaluationCaseResponse(
        Long id,
        Long evaluationSetId,
        String targetType,
        Long targetId,
        String sourceType,
        Long sourceId,
        String inputJson,
        String expectedOutputJson,
        String expectedPolicyJson,
        String status,
        Long reviewedByAdminId,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt
) {
}
