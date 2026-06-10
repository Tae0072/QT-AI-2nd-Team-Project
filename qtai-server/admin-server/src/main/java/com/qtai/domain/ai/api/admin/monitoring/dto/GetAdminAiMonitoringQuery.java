package com.qtai.domain.ai.api.admin.monitoring.dto;

public record GetAdminAiMonitoringQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        String from,
        String to
) {
}
