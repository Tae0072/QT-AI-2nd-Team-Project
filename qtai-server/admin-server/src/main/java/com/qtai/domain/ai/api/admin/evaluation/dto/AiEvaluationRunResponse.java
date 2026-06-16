package com.qtai.domain.ai.api.admin.evaluation.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AiEvaluationRunResponse(
        Long id,
        Long evaluationSetId,
        Long promptVersionId,
        String status,
        int totalCount,
        int passedCount,
        int failedCount,
        int needsReviewCount,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Long requestedByAdminId,
        List<AiEvaluationRunResultResponse> results
) {
}
