package com.qtai.domain.ai.api.dto;

import java.time.OffsetDateTime;

public record AdminAiValidationChecklistResponse(
        Long id,
        String checklistType,
        String version,
        String contentHash,
        String status,
        Long createdByAdminId,
        OffsetDateTime createdAt,
        OffsetDateTime activatedAt,
        OffsetDateTime retiredAt
) {
}
