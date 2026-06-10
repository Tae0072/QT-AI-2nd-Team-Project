package com.qtai.domain.ai.api.validation.dto;

public record RegisterAiValidationLogResult(
        Long validationLogId,
        String result,
        String assetStatus
) {
}
