package com.qtai.domain.ai.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GetAiQaResultCommand(
        @NotNull @Positive Long memberId,
        @NotNull @Positive Long requestId
) {
}
