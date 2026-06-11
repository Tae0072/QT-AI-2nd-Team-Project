package com.qtai.domain.qt.api.admin.dto;

import java.util.List;

public record AdminQtPassageListResponse(
        List<AdminQtPassageResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
