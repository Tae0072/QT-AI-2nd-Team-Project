package com.qtai.domain.study.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record HidePublishedVerseExplanationCommand(
        @NotNull @Positive Long aiAssetId
) {
}
