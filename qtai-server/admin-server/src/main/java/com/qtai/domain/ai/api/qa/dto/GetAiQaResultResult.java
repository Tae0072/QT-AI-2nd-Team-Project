package com.qtai.domain.ai.api.qa.dto;

import java.time.OffsetDateTime;

public record GetAiQaResultResult(
        Long requestId,
        String status,
        String answer,
        String sourceLabel,
        String blockedReason,
        Long qaResponseAssetId,
        OffsetDateTime answeredAt
) {
}
