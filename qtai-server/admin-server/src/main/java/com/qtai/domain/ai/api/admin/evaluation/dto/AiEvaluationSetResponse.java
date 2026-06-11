package com.qtai.domain.ai.api.admin.evaluation.dto;

import java.time.OffsetDateTime;

public record AiEvaluationSetResponse(
        Long id,
        String name,
        String evalType,
        String version,
        String targetType,
        String expectedPolicyJson,
        String description,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime activatedAt,
        OffsetDateTime retiredAt
) {
}
