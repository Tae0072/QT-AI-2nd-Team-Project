package com.qtai.domain.ai.api.admin.checklist.dto;

public record CreateAdminAiValidationChecklistCommand(
        Long adminId,
        String memberRole,
        String adminRole,
        String checklistType,
        String version,
        String contentHash,
        String status
) {
}
