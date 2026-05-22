package com.qtai.domain.ai.api.dto;

import java.time.OffsetDateTime;

public record RegenerateAiAssetResult(
        Long generationJobId,
        String status,
        OffsetDateTime createdAt
) {
}
