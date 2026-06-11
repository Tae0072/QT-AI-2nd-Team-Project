package com.qtai.domain.ai.api.generation.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterAiGeneratedAssetCommand(
        @NotNull @Positive Long generationJobId,
        @NotBlank String assetType,
        @NotBlank String targetType,
        @NotNull @Positive Long targetId,
        @NotBlank String payloadJson,
        String sourceLabel,
        @NotNull OffsetDateTime createdAt
) {
}
