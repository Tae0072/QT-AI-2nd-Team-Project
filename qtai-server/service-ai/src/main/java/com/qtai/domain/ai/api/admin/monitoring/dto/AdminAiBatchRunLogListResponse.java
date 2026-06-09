package com.qtai.domain.ai.api.admin.monitoring.dto;

import java.util.List;

public record AdminAiBatchRunLogListResponse(
        List<AdminAiBatchRunLogItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {
}
