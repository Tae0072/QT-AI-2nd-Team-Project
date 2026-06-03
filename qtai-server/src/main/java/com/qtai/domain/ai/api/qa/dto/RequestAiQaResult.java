package com.qtai.domain.ai.api.qa.dto;

public record RequestAiQaResult(
        Long requestId,
        String status,
        String blockedReason,
        Long generationJobId
) {
}
