package com.qtai.domain.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminDashboardResponse;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.ai.api.admin.monitoring.GetAdminAiMonitoringUseCase;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiMonitoringResponse;
import com.qtai.domain.audit.api.ListAdminDashboardAuditLogsUseCase;
import com.qtai.domain.audit.api.dto.AdminDashboardAuditLog;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import com.qtai.domain.report.api.GetAdminReportDashboardSummaryUseCase;
import com.qtai.domain.report.api.dto.AdminReportDashboardSummary;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    @Mock
    GetAdminAiMonitoringUseCase aiMonitoringUseCase;

    @Mock
    GetAdminReportDashboardSummaryUseCase reportSummaryUseCase;

    @Mock
    ListAdminDashboardAuditLogsUseCase auditLogsUseCase;

    @Mock
    GetTodayQtUseCase todayQtUseCase;

    AdminDashboardService service;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardService(
                verifyAdminRoleUseCase,
                aiMonitoringUseCase,
                reportSummaryUseCase,
                auditLogsUseCase,
                todayQtUseCase,
                Clock.fixed(Instant.parse("2026-06-10T01:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    @Test
    @DisplayName("AI waitingAssets와 신고 count를 dashboard 응답에 매핑한다")
    void maps_counts() {
        arrangeBase();
        when(todayQtUseCase.getToday(null))
                .thenReturn(new TodayQtResponse(35L, "2026-06-10", "오늘의 QT", "READY", true, null, "HIT"));

        AdminDashboardResponse response = service.getDashboard(7L);

        assertThat(response.pendingAiValidationCount()).isEqualTo(3);
        assertThat(response.receivedReportCount()).isEqualTo(5);
        assertThat(response.reviewingReportCount()).isEqualTo(2);
        assertThat(response.todayQt().status()).isEqualTo("READY");
        assertThat(response.todayQt().qtPassageId()).isEqualTo(35L);
    }

    @Test
    @DisplayName("오늘 QT가 없으면 todayQt non-null MISSING 규칙을 따른다")
    void missing_today_qt_is_non_null() {
        arrangeBase();
        when(todayQtUseCase.getToday(null)).thenReturn(null);

        AdminDashboardResponse response = service.getDashboard(7L);

        assertThat(response.todayQt()).isNotNull();
        assertThat(response.todayQt().qtDate()).isEqualTo("2026-06-10");
        assertThat(response.todayQt().status()).isEqualTo("MISSING");
        assertThat(response.todayQt().qtPassageId()).isNull();
        assertThat(response.todayQt().title()).isNull();
        assertThat(response.todayQt().simulatorStatus()).isNull();
        assertThat(response.todayQt().cacheStatus()).isNull();
        assertThat(response.todayQt().hasExplanation()).isFalse();
    }

    @Test
    @DisplayName("최근 감사 로그는 sanitized DTO로만 매핑한다")
    void recent_audit_logs_are_sanitized() {
        arrangeBase();
        when(todayQtUseCase.getToday(null)).thenReturn(null);

        AdminDashboardResponse response = service.getDashboard(7L);

        assertThat(response.recentAuditLogs()).hasSize(1);
        AdminDashboardResponse.RecentAuditLog log = response.recentAuditLogs().get(0);
        assertThat(log.id()).isEqualTo(10L);
        assertThat(log.actionType()).isEqualTo("AI_ASSET_APPROVE");
    }

    private void arrangeBase() {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenReturn(new AdminUserInfo(1L, 7L, "OPERATOR"));
        when(aiMonitoringUseCase.getAdminAiMonitoring(any()))
                .thenReturn(aiMonitoring(3));
        when(reportSummaryUseCase.getDashboardSummary())
                .thenReturn(new AdminReportDashboardSummary(5, 2));
        when(auditLogsUseCase.listRecentAuditLogs(5))
                .thenReturn(List.of(new AdminDashboardAuditLog(
                        10L,
                        1L,
                        "ADMIN",
                        "AI_ASSET_APPROVE",
                        "AI_GENERATED_ASSET",
                        500L,
                        OffsetDateTime.parse("2026-06-10T10:00:00+09:00")
                )));
    }

    private static AdminAiMonitoringResponse aiMonitoring(long waitingAssets) {
        return new AdminAiMonitoringResponse(
                new AdminAiMonitoringResponse.Period(null, null, "Asia/Seoul"),
                new AdminAiMonitoringResponse.GenerationJobs(0, 0, 0, 0),
                new AdminAiMonitoringResponse.Validation(waitingAssets, 0, 0, 0, List.of()),
                new AdminAiMonitoringResponse.BatchRuns(0, 0, 0, List.of()),
                new AdminAiMonitoringResponse.Qa(0, 0, 0, 0, List.of()),
                List.of()
        );
    }
}
