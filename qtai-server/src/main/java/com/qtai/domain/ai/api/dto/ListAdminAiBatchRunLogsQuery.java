package com.qtai.domain.ai.api.dto;

public record ListAdminAiBatchRunLogsQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        String batchName,
        String status,
        String from,
        String to,
        int page,
        int size
) {
}
