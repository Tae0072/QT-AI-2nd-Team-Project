package com.qtai.domain.study.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Approved glossary release command for one AI asset.
 *
 * <p>{@code terms} is the complete approved glossary set for the asset. An empty list intentionally hides the
 * currently approved terms for the asset and publishes nothing.
 */
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
