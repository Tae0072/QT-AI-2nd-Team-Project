package com.qtai.domain.ai.api.qa.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RequestAiQaCommand(
        @NotNull @Positive Long memberId,
        @NotNull @Positive Long qtPassageId,
        @NotBlank String question,
        @NotNull OffsetDateTime requestedAt
) {
}
