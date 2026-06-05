package com.qtai.domain.study.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PublishApprovedGlossaryTermsCommand(
        @NotNull @Positive Long aiAssetId,
        @NotBlank String sourceLabel,
        @NotNull OffsetDateTime approvedAt,
        @NotNull List<@Valid Term> terms
) {

    public record Term(
            @NotNull @Positive Long bibleVerseId,
            @NotBlank String term,
            @NotBlank String meaning
    ) {
    }
}
