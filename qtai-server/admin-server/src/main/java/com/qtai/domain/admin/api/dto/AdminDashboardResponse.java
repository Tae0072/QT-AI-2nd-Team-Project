package com.qtai.domain.admin.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * AD-01 관리자 대시보드 응답.
 */
public record AdminDashboardResponse(
        long pendingAiValidationCount,
        long receivedReportCount,
        long reviewingReportCount,
        TodayQt todayQt,
        List<RecentAuditLog> recentAuditLogs
) {

    public AdminDashboardResponse {
        recentAuditLogs = recentAuditLogs == null ? List.of() : List.copyOf(recentAuditLogs);
    }

    public record TodayQt(
            String qtDate,
            Long qtPassageId,
            String title,
            String status,
            String simulatorStatus,
            boolean hasExplanation,
            String cacheStatus
    ) {
    }

    public record RecentAuditLog(
            Long id,
            Long adminUserId,
            String actorType,
            String actionType,
            String targetType,
            Long targetId,
            OffsetDateTime createdAt
    ) {
    }
}
