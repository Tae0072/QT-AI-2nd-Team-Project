package com.qtai.domain.ai.api.generation.dto;

public record CreateAiGenerationJobResult(
        Long generationJobId,
        String status
) {
}
