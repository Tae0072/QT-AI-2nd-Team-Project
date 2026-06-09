package com.qtai.domain.ai.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SystemAiGenerationJobRequest(
        @NotBlank String jobType,
        @NotBlank String targetType,
        @NotNull @Positive Long targetId,
        @NotNull @Positive Long promptVersionId
) {
}
