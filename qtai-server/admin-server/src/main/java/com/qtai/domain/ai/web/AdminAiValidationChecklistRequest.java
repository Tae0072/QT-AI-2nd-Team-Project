package com.qtai.domain.ai.web;

import jakarta.validation.constraints.NotBlank;

record AdminAiValidationChecklistRequest(
        @NotBlank
        String checklistType,

        @NotBlank
        String version,

        @NotBlank
        String contentHash,

        String status
) {
}
