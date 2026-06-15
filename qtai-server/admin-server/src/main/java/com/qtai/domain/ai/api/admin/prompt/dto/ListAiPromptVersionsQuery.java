package com.qtai.domain.ai.api.admin.prompt.dto;

public record ListAiPromptVersionsQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        String promptType,
        String status,
        int page,
        int size
) {
}
