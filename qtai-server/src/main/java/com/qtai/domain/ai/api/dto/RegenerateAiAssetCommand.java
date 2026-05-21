package com.qtai.domain.ai.api.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegenerateAiAssetCommand(
        @NotNull @Positive Long adminId,
        @NotNull @Positive Long assetId,
        @NotBlank String memberRole,
        @NotBlank String adminRole,
        @NotBlank String reason,
        @NotNull @Positive Long promptVersionId,
        @NotNull OffsetDateTime requestedAt
) {
}
