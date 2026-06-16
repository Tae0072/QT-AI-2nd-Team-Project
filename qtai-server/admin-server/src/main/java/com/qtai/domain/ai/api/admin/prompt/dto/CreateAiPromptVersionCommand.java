package com.qtai.domain.ai.api.admin.prompt.dto;

public record CreateAiPromptVersionCommand(
        Long adminId,
        String memberRole,
        String adminRole,
        String promptType,
        String version,
        String systemPrompt,
        String userPromptTemplate,
        String modelName,
        Double temperature,
        Integer maxTokens,
        String description
) {
}
