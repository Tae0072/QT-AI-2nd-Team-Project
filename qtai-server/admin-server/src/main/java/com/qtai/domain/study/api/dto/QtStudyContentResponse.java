package com.qtai.domain.study.api.dto;

import java.util.List;

public record QtStudyContentResponse(
        String summary,
        List<ExplanationItem> explanations,
        List<GlossaryTermItem> glossaryTerms
) {
    // aiAssetId(내부 AI 자산 PK)는 사용자 응답에 노출하지 않는다(P2, 리뷰 §3 study).
    // 자산 연계 추적은 내부 read 모델(ApprovedVerseExplanationResponse)·관리자 경로에서만 사용한다.
    public record ExplanationItem(
            Long verseId,
            String summary,
            String explanation,
            String sourceLabel
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
