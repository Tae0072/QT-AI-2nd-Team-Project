package com.qtai.domain.report.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.domain.report.api.GetAdminReportDashboardSummaryUseCase;
import com.qtai.domain.report.api.dto.AdminReportDashboardSummary;

/**
 * 관리자 대시보드용 신고 요약 조회.
 */
@Service
class AdminReportDashboardSummaryService implements GetAdminReportDashboardSummaryUseCase {

    private final ReportRepository reportRepository;

    AdminReportDashboardSummaryService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReportDashboardSummary getDashboardSummary() {
        return new AdminReportDashboardSummary(
                reportRepository.countByStatus(ReportStatus.RECEIVED),
                reportRepository.countByStatus(ReportStatus.REVIEWING)
        );
    }
}
