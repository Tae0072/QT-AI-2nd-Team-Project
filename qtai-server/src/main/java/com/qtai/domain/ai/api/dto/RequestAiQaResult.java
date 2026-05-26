package com.qtai.domain.ai.api.dto;

public record RequestAiQaResult(
        Long requestId,
        String status,
        String blockedReason,
        Long generationJobId
) {
}
