package com.qtai.domain.ai.api.admin.prompt.dto;

public record GetAiPromptVersionQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        Long promptVersionId
) {
}
