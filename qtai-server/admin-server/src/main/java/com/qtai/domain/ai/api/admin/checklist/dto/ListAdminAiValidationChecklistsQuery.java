package com.qtai.domain.ai.api.admin.checklist.dto;

public record ListAdminAiValidationChecklistsQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        String checklistType,
        String status,
        int page,
        int size
) {
}
