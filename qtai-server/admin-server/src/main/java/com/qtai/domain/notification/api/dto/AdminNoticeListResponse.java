package com.qtai.domain.notification.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminNoticeListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {

    public record Item(
            Long id,
            String title,
            String bodyPreview,
            String status,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
