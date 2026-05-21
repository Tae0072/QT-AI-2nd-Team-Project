package com.qtai.domain.ai.api.dto;

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
