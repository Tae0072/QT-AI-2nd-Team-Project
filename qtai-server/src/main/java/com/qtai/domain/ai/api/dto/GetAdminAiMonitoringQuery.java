package com.qtai.domain.ai.api.dto;

public record GetAdminAiMonitoringQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        String from,
        String to
) {
}
