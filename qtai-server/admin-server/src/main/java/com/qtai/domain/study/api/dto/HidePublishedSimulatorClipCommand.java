package com.qtai.domain.study.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record HidePublishedSimulatorClipCommand(
        @NotNull @Positive Long aiAssetId
) {
}
