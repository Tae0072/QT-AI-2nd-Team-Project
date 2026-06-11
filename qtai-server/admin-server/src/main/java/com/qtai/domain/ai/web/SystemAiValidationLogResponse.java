package com.qtai.domain.ai.web;

public record SystemAiValidationLogResponse(
        Long validationLogId,
        String result,
        String assetStatus
) {
}
