package com.qtai.domain.ai.api.admin.asset.dto;

import java.time.OffsetDateTime;

public record RegenerateAiAssetResult(
        Long generationJobId,
        String status,
        OffsetDateTime createdAt
) {
}
