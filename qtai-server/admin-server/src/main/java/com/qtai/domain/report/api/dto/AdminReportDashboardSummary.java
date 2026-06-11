package com.qtai.domain.report.api.dto;

/**
 * 관리자 대시보드용 신고 상태 요약.
 */
public record AdminReportDashboardSummary(
        long receivedReportCount,
        long reviewingReportCount
) {
}
