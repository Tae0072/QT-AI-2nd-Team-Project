package com.qtai.domain.ai.web;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SystemValidationReferenceJobRequest(
        @NotBlank @Size(max = 150) String sourceName,
        @NotBlank @Size(max = 255) String sourceFileName,
        @NotBlank @Size(max = 100) String sourceFileHash,
        @Size(max = 500) String storageUri,
        @Size(max = 500) String indexStorageUri,
        OffsetDateTime expiresAt
) {
}
