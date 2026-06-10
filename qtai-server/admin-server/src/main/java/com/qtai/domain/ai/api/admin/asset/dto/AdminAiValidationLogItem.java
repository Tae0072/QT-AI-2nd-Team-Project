package com.qtai.domain.ai.api.admin.asset.dto;

import java.time.OffsetDateTime;

public record AdminAiValidationLogItem(
        Long validationLogId,
        Long validationReferenceJobId,
        Long checklistVersionId,
        int layer,
        String result,
        String reviewerType,
        String errorMessage,
        OffsetDateTime createdAt
) {
}
