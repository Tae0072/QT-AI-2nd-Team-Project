package com.qtai.domain.study.api.dto;

import java.util.List;

public record QtStudyContentResponse(
        String summary,
        List<ExplanationItem> explanations,
        List<GlossaryTermItem> glossaryTerms
) {
    public record ExplanationItem(
            Long verseId,
            String summary,
            String explanation,
            String sourceLabel,
            Long aiAssetId
    ) {
    }

    public record GlossaryTermItem(
            Long id,
            Long verseId,
            String term,
            String meaning,
            String sourceLabel
    ) {
    }
}
