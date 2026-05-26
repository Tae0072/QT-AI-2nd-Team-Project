package com.qtai.domain.ai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GetAdminAiAssetQuery(
        @NotNull @Positive Long adminId,
        @NotBlank String memberRole,
        @NotBlank String adminRole,
        @NotNull @Positive Long assetId
) {
}
