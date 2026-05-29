package com.qtai.domain.ai.api.dto;

public record ChangeAdminAiValidationChecklistStatusCommand(
        Long adminId,
        String memberRole,
        String adminRole,
        Long checklistId
) {
}
