package com.qtai.domain.ai.api.admin.asset.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReviewAiAssetCommand(
        @NotNull @Positive Long reviewerId,
        @NotNull @Positive Long assetId,
        @NotBlank String memberRole,
        @NotBlank String adminRole,
        @NotBlank String action,
        String reason,
        boolean activateForTarget,
        @NotNull OffsetDateTime reviewedAt
) {
}
