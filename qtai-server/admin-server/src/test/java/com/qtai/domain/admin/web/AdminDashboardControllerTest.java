package com.qtai.domain.admin.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.domain.ai.api.admin.monitoring.GetAdminAiMonitoringUseCase;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiMonitoringResponse;
import com.qtai.domain.audit.api.ListAdminDashboardAuditLogsUseCase;
import com.qtai.domain.audit.api.dto.AdminDashboardAuditLog;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import com.qtai.domain.report.api.GetAdminReportDashboardSummaryUseCase;
import com.qtai.domain.report.api.dto.AdminReportDashboardSummary;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDashboardControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    @MockBean
    GetAdminAiMonitoringUseCase aiMonitoringUseCase;

    @MockBean
    GetAdminReportDashboardSummaryUseCase reportSummaryUseCase;

    @MockBean
    ListAdminDashboardAuditLogsUseCase auditLogsUseCase;

    @MockBean
    GetTodayQtUseCase todayQtUseCase;

    @MockBean
    GetQtPassageContentContextUseCase qtPassageContentContextUseCase;

    @Test
    @DisplayName("OPERATOR 관리자 권한이면 dashboard를 조회한다")
    @WithMockUser(username = "7", roles = "ADMIN")
    void dashboard_operator_200() throws Exception {
        arrangeDashboard("OPERATOR");

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pendingAiValidationCount").value(3))
                .andExpect(jsonPath("$.data.receivedReportCount").value(5))
                .andExpect(jsonPath("$.data.reviewingReportCount").value(2))
                .andExpect(jsonPath("$.data.todayQt.status").value("READY"))
                .andExpect(jsonPath("$.data.recentAuditLogs[0].actionType").value("AI_ASSET_APPROVE"))
                .andExpect(jsonPath("$.data.recentAuditLogs[0].beforeJson").doesNotExist())
                .andExpect(jsonPath("$.data.recentAuditLogs[0].afterJson").doesNotExist());
    }

    @Test
    @DisplayName("REVIEWER 관리자 권한이면 dashboard를 조회한다")
    @WithMockUser(username = "7", roles = "ADMIN")
    void dashboard_reviewer_200() throws Exception {
        arrangeDashboard("REVIEWER");

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingAiValidationCount").value(3));
    }

    @Test
    @DisplayName("SUPER_ADMIN은 verifyAnyRole 우월권으로 dashboard를 조회한다")
    @WithMockUser(username = "7", roles = "ADMIN")
    void dashboard_superAdmin_200() throws Exception {
        arrangeDashboard("SUPER_ADMIN");

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingAiValidationCount").value(3));
    }

    @Test
    @DisplayName("CONTENT_CREATOR는 dashboard 2차 권한 검증에서 403")
    @WithMockUser(username = "7", roles = "ADMIN")
    void dashboard_contentCreator_403() throws Exception {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_ROLE_INSUFFICIENT));

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN이 아니면 dashboard 1차 권한 검증에서 403")
    @WithMockUser(username = "7", roles = "USER")
    void dashboard_userRole_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 dashboard 접근 시 401 또는 403")
    void dashboard_unauthenticated_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                        .isIn(401, 403));
    }

    private void arrangeDashboard(String adminRole) {
        when(verifyAdminRoleUseCase.verifyAnyRole(eq(7L), any()))
                .thenReturn(new AdminUserInfo(1L, 7L, adminRole));
        when(aiMonitoringUseCase.getAdminAiMonitoring(any()))
                .thenReturn(aiMonitoring(3));
        when(reportSummaryUseCase.getDashboardSummary())
                .thenReturn(new AdminReportDashboardSummary(5, 2));
        when(todayQtUseCase.getToday(null))
                .thenReturn(new TodayQtResponse(35L, "2026-06-10", "오늘의 QT", "MISSING", true, null, "HIT"));
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
