package com.qtai.domain.ai.web;

import java.time.OffsetDateTime;

public record SystemAiGenerationJobResponse(
        Long generationJobId,
        String status,
        OffsetDateTime createdAt
) {
}
