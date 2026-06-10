package com.qtai.domain.audit.api.dto;

import java.util.List;

public record AuditLogListResponse(
        List<AuditLogItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {
}
