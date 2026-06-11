package com.qtai.domain.ai.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegenerateAiAssetRequest(
        @NotBlank String reason,
        @NotNull @Positive Long promptVersionId
) {
}
