package com.qtai.domain.ai.web;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SystemAiValidationLogRequest(
        @NotNull @Positive Long aiAssetId,
        @Positive Long validationReferenceJobId,
        @NotNull @Positive Long checklistVersionId,
        @Min(1) int layer,
        @NotBlank String result,
        @NotNull JsonNode checklistJson,
        @NotBlank String reviewerType,
        String errorMessage
) {
}
