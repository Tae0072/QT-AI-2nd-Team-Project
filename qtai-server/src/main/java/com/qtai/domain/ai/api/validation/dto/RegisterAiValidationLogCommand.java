package com.qtai.domain.ai.api.validation.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterAiValidationLogCommand(
        @NotNull @Positive Long assetId,
        @Positive Long validationReferenceJobId,
        @Min(1) int layer,
        @NotBlank String result,
        @NotBlank String reviewerType,
        Long checklistVersionId,
        String checklistJson,
        String errorMessage,
        @NotNull OffsetDateTime createdAt
) {
}
