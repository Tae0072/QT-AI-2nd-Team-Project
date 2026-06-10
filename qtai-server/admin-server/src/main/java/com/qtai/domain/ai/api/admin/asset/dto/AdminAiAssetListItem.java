package com.qtai.domain.ai.api.admin.asset.dto;

import java.time.OffsetDateTime;

public record AdminAiAssetListItem(
        Long id,
        String assetType,
        String targetType,
        Long targetId,
        String status,
        PromptVersionSummary promptVersion,
        Long checklistVersionId,
        String latestValidationResult,
        boolean sourceLabelPresent,
        OffsetDateTime createdAt
) {

    public record PromptVersionSummary(
            Long id,
            String promptType,
            String version,
            String status
    ) {
    }
}
