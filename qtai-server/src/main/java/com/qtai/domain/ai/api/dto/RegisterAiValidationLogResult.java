package com.qtai.domain.ai.api.dto;

public record RegisterAiValidationLogResult(
        Long validationLogId,
        String result,
        String assetStatus
) {
}
