package com.qtai.domain.report.api;

import com.qtai.domain.report.api.dto.AdminReportDashboardSummary;

/**
 * 관리자 대시보드용 신고 요약 조회 UseCase.
 */
public interface GetAdminReportDashboardSummaryUseCase {

    AdminReportDashboardSummary getDashboardSummary();
}
