package com.qtai.domain.qtvideo.api.dto;

import java.util.List;

public record AdminQtVideoSourceListResponse(
        List<AdminQtVideoSourceItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
