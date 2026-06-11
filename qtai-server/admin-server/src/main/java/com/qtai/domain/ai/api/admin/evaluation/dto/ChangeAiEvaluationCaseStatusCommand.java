package com.qtai.domain.ai.api.admin.evaluation.dto;

import java.time.OffsetDateTime;

public record ChangeAiEvaluationCaseStatusCommand(
        Long adminId,
        String memberRole,
        String adminRole,
        Long caseId,
        String reviewReason,
        OffsetDateTime reviewedAt
) {
}
