package com.qtai.domain.study.api.dto;

public record PublishApprovedGlossaryTermsResult(
        Long aiAssetId,
        int publishedCount,
        int hiddenCount
) {
}
