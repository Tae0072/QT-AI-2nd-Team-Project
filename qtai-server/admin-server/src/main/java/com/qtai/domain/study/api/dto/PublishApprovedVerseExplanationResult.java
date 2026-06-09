package com.qtai.domain.study.api.dto;

public record PublishApprovedVerseExplanationResult(
        Long bibleVerseId,
        Long aiAssetId,
        String status
) {
}
