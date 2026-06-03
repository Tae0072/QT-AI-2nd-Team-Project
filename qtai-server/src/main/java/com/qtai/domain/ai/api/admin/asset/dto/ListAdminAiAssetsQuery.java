package com.qtai.domain.ai.api.admin.asset.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ListAdminAiAssetsQuery(
        @NotNull @Positive Long adminId,
        @NotBlank String memberRole,
        @NotBlank String adminRole,
        String assetType,
        String targetType,
        String status,
        @Positive Long promptVersionId,
        @Positive Long checklistVersionId,
        @PositiveOrZero int page,
        @Min(1) int size
) {
}
