package com.qtai.domain.ai.api.dto;

import java.time.OffsetDateTime;

public record ValidationReferenceJobResponse(
        Long id,
        String sourceName,
        String sourceFileName,
        String status,
        OffsetDateTime expiresAt,
        OffsetDateTime deletedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
