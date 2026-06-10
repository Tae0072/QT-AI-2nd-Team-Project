package com.qtai.domain.study.api.dto;

public record ApprovedVerseExplanationResponse(
        Long verseId,
        String summary,
        String explanation,
        String sourceLabel,
        Long aiAssetId
) {
}
