package com.qtai.domain.ai.web;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SystemAiAssetRequest(
        @NotNull @Positive Long generationJobId,
        @NotBlank String assetType,
        @NotBlank String targetType,
        @NotNull @Positive Long targetId,
        @NotNull JsonNode payloadJson,
        String sourceLabel,
        String status
) {
}
