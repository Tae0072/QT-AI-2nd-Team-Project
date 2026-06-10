package com.qtai.domain.report.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.qtai.domain.report.api.dto.AdminReportDashboardSummary;

@ExtendWith(MockitoExtension.class)
class AdminReportDashboardSummaryServiceTest {

    @Mock
    ReportRepository reportRepository;

    @Test
    @DisplayName("RECEIVED와 REVIEWING 신고 건수를 dashboard summary로 반환한다")
    void counts_received_and_reviewing_reports() {
        when(reportRepository.countByStatus(ReportStatus.RECEIVED)).thenReturn(5L);
        when(reportRepository.countByStatus(ReportStatus.REVIEWING)).thenReturn(2L);
        AdminReportDashboardSummaryService service = new AdminReportDashboardSummaryService(reportRepository);

        AdminReportDashboardSummary summary = service.getDashboardSummary();

        assertThat(summary.receivedReportCount()).isEqualTo(5);
        assertThat(summary.reviewingReportCount()).isEqualTo(2);
    }
}
