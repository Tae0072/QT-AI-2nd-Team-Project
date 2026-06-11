package com.qtai.domain.ai.api.admin.evaluation.dto;

import java.util.List;

public record AiEvaluationSetListResponse(
        List<AiEvaluationSetResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {
}
