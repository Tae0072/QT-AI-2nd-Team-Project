package com.qtai.domain.ai.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public record AdminAiAssetDetailResponse(
        Long id,
        String assetType,
        String targetType,
        Long targetId,
        String status,
        JsonNode payloadJson,
        String sourceLabel,
        OffsetDateTime createdAt,
        OffsetDateTime reviewedAt,
        GenerationJobSummary generationJob,
        PromptVersionSummary promptVersion,
        List<AdminAiValidationLogItem> validationLogs
) {

    public record GenerationJobSummary(
            Long id,
            String jobType,
            String targetType,
            Long targetId,
            Long promptVersionId,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            String errorMessage
    ) {
    }

    public record PromptVersionSummary(
            Long id,
            String promptType,
            String version,
            String status
    ) {
    }
}
