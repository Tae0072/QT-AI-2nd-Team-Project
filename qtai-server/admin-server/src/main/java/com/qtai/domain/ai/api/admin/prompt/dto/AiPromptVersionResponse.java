package com.qtai.domain.ai.api.admin.prompt.dto;

import java.time.OffsetDateTime;

public record AiPromptVersionResponse(
        Long id,
        String promptType,
        String version,
        String contentHash,
        String status,
        String systemPrompt,
        String userPromptTemplate,
        String modelName,
        Double temperature,
        Integer maxTokens,
        String description,
        Long createdByAdminId,
        OffsetDateTime createdAt,
        OffsetDateTime activatedAt,
        OffsetDateTime retiredAt
) {
}
