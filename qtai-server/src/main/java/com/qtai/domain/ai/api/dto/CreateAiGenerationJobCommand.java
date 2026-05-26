package com.qtai.domain.ai.api.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

public record CreateAiGenerationJobCommand(
        @NotBlank String jobType,
        @NotBlank String targetType,
        @NotNull @Positive Long targetId,
        @NotNull @Positive Long promptVersionId,
        @NotBlank String requestedBy,
        @NotNull OffsetDateTime requestedAt
) {
}
