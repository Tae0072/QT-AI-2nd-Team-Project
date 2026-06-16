package com.qtai.domain.qtvideo.api.dto;

import java.util.List;

public record AdminQtVideoClipListResponse(
        List<AdminQtVideoClipItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
