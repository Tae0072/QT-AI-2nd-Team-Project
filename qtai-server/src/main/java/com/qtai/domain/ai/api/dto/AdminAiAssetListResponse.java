package com.qtai.domain.ai.api.dto;

import java.util.List;

public record AdminAiAssetListResponse(
        List<AdminAiAssetListItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {
}
