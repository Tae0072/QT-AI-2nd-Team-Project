package com.qtai.domain.study.api.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PublishApprovedVerseExplanationCommand(
        @NotNull @Positive Long bibleVerseId,
        @NotBlank String summary,
        @NotBlank String explanation,
        @NotBlank String sourceLabel,
        @NotNull @Positive Long aiAssetId,
        @NotNull OffsetDateTime approvedAt
) {
}
