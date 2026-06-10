package com.qtai.domain.ai.api.validation.dto;

import java.time.OffsetDateTime;

public record CreateValidationReferenceJobCommand(
        String sourceName,
        String sourceFileName,
        String sourceFileHash,
        String storageUri,
        String indexStorageUri,
        OffsetDateTime expiresAt
) {
}
