package com.qtai.domain.study.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record HidePublishedGlossaryTermsCommand(
        @NotNull @Positive Long aiAssetId
) {
}
