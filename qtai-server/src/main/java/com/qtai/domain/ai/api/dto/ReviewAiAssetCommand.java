package com.qtai.domain.ai.api.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReviewAiAssetCommand(
        @NotNull @Positive Long reviewerId,
        @NotNull @Positive Long assetId,
        @NotBlank String action,
        Long checklistVersionId,
        String reason,
        @NotNull OffsetDateTime reviewedAt
) {
}
