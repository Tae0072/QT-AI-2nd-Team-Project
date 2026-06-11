package com.qtai.domain.study.api.dto;

public record HidePublishedGlossaryTermsResult(
        Long aiAssetId,
        int hiddenCount
) {
}
