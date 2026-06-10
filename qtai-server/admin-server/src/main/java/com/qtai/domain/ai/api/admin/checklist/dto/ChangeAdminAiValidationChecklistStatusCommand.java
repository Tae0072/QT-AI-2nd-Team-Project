package com.qtai.domain.ai.api.admin.checklist.dto;

public record ChangeAdminAiValidationChecklistStatusCommand(
        Long adminId,
        String memberRole,
        String adminRole,
        Long checklistId
) {
}
