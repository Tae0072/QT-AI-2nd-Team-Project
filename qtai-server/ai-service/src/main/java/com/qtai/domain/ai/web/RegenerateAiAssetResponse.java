package com.qtai.domain.ai.web;

import java.time.OffsetDateTime;

public record RegenerateAiAssetResponse(
        Long generationJobId,
        String status,
        OffsetDateTime createdAt
) {
}
