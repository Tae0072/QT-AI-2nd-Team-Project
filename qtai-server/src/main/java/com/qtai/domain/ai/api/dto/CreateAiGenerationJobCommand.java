package com.qtai.domain.ai.api.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAiGenerationJobCommand(
        @NotBlank String jobType,
        @NotBlank String targetType,
        @NotNull @Positive Long targetId,
        @NotBlank String promptVersion,
        @NotBlank String requestedBy,
        @NotNull OffsetDateTime requestedAt
) {
}
