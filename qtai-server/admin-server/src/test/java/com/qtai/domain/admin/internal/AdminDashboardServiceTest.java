package com.qtai.domain.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminDashboardResponse;
import com.qtai.domain.admin.api.dto.AdminDashboardResponse.TodayQtStatus;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.ai.api.admin.monitoring.GetAdminAiMonitoringUseCase;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiMonitoringResponse;
import com.qtai.domain.ai.api.admin.monitoring.dto.GetAdminAiMonitoringQuery;
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
    @DisplayName("AI waitingAssets and report counts are mapped to dashboard response")
    void maps_counts() {
        arrangeBase();
        when(todayQtUseCase.getToday(null))
                .thenReturn(new TodayQtResponse(35L, "2026-06-10", "Today QT", "READY", true, null, "HIT"));

        AdminDashboardResponse response = service.getDashboard(7L);

        assertThat(response.pendingAiValidationCount()).isEqualTo(3);
        assertThat(response.receivedReportCount()).isEqualTo(5);
        assertThat(response.reviewingReportCount()).isEqualTo(2);
        assertThat(response.todayQt().status()).isEqualTo(TodayQtStatus.READY);
        assertThat(response.todayQt().qtPassageId()).isEqualTo(35L);

        ArgumentCaptor<GetAdminAiMonitoringQuery> queryCaptor =
                ArgumentCaptor.forClass(GetAdminAiMonitoringQuery.class);
        verify(aiMonitoringUseCase).getAdminAiMonitoring(queryCaptor.capture());
        assertThat(queryCaptor.getValue().from()).isNull();
        assertThat(queryCaptor.getValue().to()).isNull();
    }

    @Test
    @DisplayName("Missing today QT returns non-null MISSING response")
    void missing_today_qt_is_non_null() {
        arrangeBase();
        when(todayQtUseCase.getToday(null)).thenReturn(null);

        AdminDashboardResponse response = service.getDashboard(7L);

        assertThat(response.todayQt()).isNotNull();
        assertThat(response.todayQt().qtDate()).isEqualTo("2026-06-10");
        assertThat(response.todayQt().status()).isEqualTo(TodayQtStatus.MISSING);
        assertThat(response.todayQt().qtPassageId()).isNull();
        assertThat(response.todayQt().title()).isNull();
        assertThat(response.todayQt().simulatorStatus()).isNull();
        assertThat(response.todayQt().cacheStatus()).isNull();
        assertThat(response.todayQt().hasExplanation()).isFalse();
    }

    @Test
    @DisplayName("EMPTY cache status is treated as missing today QT")
    void empty_cache_today_qt_is_missing() {
        arrangeBase();
        when(todayQtUseCase.getToday(null))
                .thenReturn(new TodayQtResponse(35L, "2026-06-10", "Today QT", "READY", true, null, "EMPTY"));

        AdminDashboardResponse response = service.getDashboard(7L);

        assertThat(response.todayQt().status()).isEqualTo(TodayQtStatus.MISSING);
        assertThat(response.todayQt().qtPassageId()).isNull();
        assertThat(response.todayQt().title()).isNull();
        assertThat(response.todayQt().simulatorStatus()).isNull();
        assertThat(response.todayQt().cacheStatus()).isNull();
        assertThat(response.todayQt().hasExplanation()).isFalse();
    }

    @Test
    @DisplayName("MISS cache without passage is treated as missing today QT")
    void miss_cache_without_passage_is_missing() {
        arrangeBase();
        when(todayQtUseCase.getToday(null))
                .thenReturn(new TodayQtResponse(null, null, null, "MISSING", false, null, "MISS"));

        AdminDashboardResponse response = service.getDashboard(7L);

        assertThat(response.todayQt().status()).isEqualTo(TodayQtStatus.MISSING);
        assertThat(response.todayQt().qtPassageId()).isNull();
        assertThat(response.todayQt().cacheStatus()).isNull();
    }

    @Test
    @DisplayName("Today QT status is separated from simulator status")
    void ready_today_qt_preserves_simulator_status() {
        arrangeBase();
        when(todayQtUseCase.getToday(null))
                .thenReturn(new TodayQtResponse(35L, "2026-06-10", "Today QT", "FAILED", true, null, "HIT"));

        AdminDashboardResponse response = service.getDashboard(7L);

        assertThat(response.todayQt().status()).isEqualTo(TodayQtStatus.READY);
        assertThat(response.todayQt().simulatorStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Null AI validation maps pending count to zero")
    void null_ai_validation_maps_to_zero() {
        arrangeBase();
        when(aiMonitoringUseCase.getAdminAiMonitoring(any()))
                .thenReturn(new AdminAiMonitoringResponse(
                        new AdminAiMonitoringResponse.Period(null, null, "Asia/Seoul"),
                        new AdminAiMonitoringResponse.GenerationJobs(0, 0, 0, 0),
                        null,
                        new AdminAiMonitoringResponse.BatchRuns(0, 0, 0, List.of()),
                        new AdminAiMonitoringResponse.Qa(0, 0, 0, 0, List.of()),
                        List.of()
                ));
        when(todayQtUseCase.getToday(null)).thenReturn(null);

        AdminDashboardResponse response = service.getDashboard(7L);

        assertThat(response.pendingAiValidationCount()).isZero();
    }

    @Test
    @DisplayName("Zero AI waitingAssets maps pending count to zero")
    void zero_ai_waiting_assets_maps_to_zero() {
        arrangeBase();
        when(aiMonitoringUseCase.getAdminAiMonitoring(any()))
                .thenReturn(aiMonitoring(0));
        when(todayQtUseCase.getToday(null)).thenReturn(null);

        AdminDashboardResponse response = service.getDashboard(7L);

        assertThat(response.pendingAiValidationCount()).isZero();
    }

    @Test
    @DisplayName("Invalid memberId throws UNAUTHORIZED")
    void invalid_member_id_is_unauthorized() {
        assertThatThrownBy(() -> service.getDashboard(null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        assertThatThrownBy(() -> service.getDashboard(0L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Recent audit logs are mapped through sanitized DTO only")
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
