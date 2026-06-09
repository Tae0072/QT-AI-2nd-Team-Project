package com.qtai.domain.study.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Publishes the complete approved glossary set for one AI asset.
 *
 * <p>Callers must pass one term per bible verse after the AI asset has passed validation. Empty publish payloads are
 * rejected; use {@code HidePublishedGlossaryTermsUseCase} to unpublish the active glossary terms for an asset.
 */
public record PublishApprovedGlossaryTermsCommand(
        @NotNull @Positive Long aiAssetId,
        @NotBlank String sourceLabel,
        @NotNull OffsetDateTime approvedAt,
        @NotEmpty List<@Valid Term> terms
) {

    public record Term(
            @NotNull @Positive Long bibleVerseId,
            @NotBlank String term,
            @NotBlank String meaning
    ) {
    }
}
