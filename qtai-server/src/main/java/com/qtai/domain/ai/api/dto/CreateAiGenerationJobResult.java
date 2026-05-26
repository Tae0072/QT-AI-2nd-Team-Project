package com.qtai.domain.ai.api.dto;

public record CreateAiGenerationJobResult(
        Long generationJobId,
        String status
) {
}
