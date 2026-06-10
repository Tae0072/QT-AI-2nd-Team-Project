package com.qtai.domain.admin.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.GetAdminDashboardUseCase;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminDashboardResponse;
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

/**
 * AD-01 관리자 대시보드 요약 조립 서비스.
 */
@Service
class AdminDashboardService implements GetAdminDashboardUseCase {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<String> DASHBOARD_ROLES = List.of("OPERATOR", "REVIEWER");
    private static final int RECENT_AUDIT_LOG_SIZE = 5;

    private final VerifyAdminRoleUseCase verifyAdminRoleUseCase;
    private final GetAdminAiMonitoringUseCase aiMonitoringUseCase;
    private final GetAdminReportDashboardSummaryUseCase reportSummaryUseCase;
    private final ListAdminDashboardAuditLogsUseCase auditLogsUseCase;
    private final GetTodayQtUseCase todayQtUseCase;
    private final Clock clock;

    AdminDashboardService(
            VerifyAdminRoleUseCase verifyAdminRoleUseCase,
            GetAdminAiMonitoringUseCase aiMonitoringUseCase,
            GetAdminReportDashboardSummaryUseCase reportSummaryUseCase,
            ListAdminDashboardAuditLogsUseCase auditLogsUseCase,
            GetTodayQtUseCase todayQtUseCase,
            Clock clock
    ) {
        this.verifyAdminRoleUseCase = verifyAdminRoleUseCase;
        this.aiMonitoringUseCase = aiMonitoringUseCase;
        this.reportSummaryUseCase = reportSummaryUseCase;
        this.auditLogsUseCase = auditLogsUseCase;
        this.todayQtUseCase = todayQtUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        AdminUserInfo admin = verifyAdminRoleUseCase.verifyAnyRole(memberId, DASHBOARD_ROLES);
        LocalDate today = LocalDate.now(clock.withZone(SEOUL_ZONE));

        AdminAiMonitoringResponse aiMonitoring = aiMonitoringUseCase.getAdminAiMonitoring(
                new GetAdminAiMonitoringQuery(
                        admin.adminUserId(),
                        "ADMIN",
                        admin.adminRole(),
                        today.toString(),
                        today.toString()
                )
        );
        AdminReportDashboardSummary reportSummary = reportSummaryUseCase.getDashboardSummary();
        List<AdminDashboardAuditLog> auditLogs = auditLogsUseCase.listRecentAuditLogs(RECENT_AUDIT_LOG_SIZE);

        return new AdminDashboardResponse(
                aiMonitoring.validation().waitingAssets(),
                reportSummary.receivedReportCount(),
                reportSummary.reviewingReportCount(),
                toTodayQt(today),
                auditLogs.stream()
                        .map(AdminDashboardService::toRecentAuditLog)
                        .toList()
        );
    }

    private AdminDashboardResponse.TodayQt toTodayQt(LocalDate today) {
        TodayQtResponse todayQt = todayQtUseCase.getToday(null);
        if (todayQt == null || todayQt.qtPassageId() == null) {
            return missingTodayQt(today);
        }
        return new AdminDashboardResponse.TodayQt(
                todayQt.passageDate() == null ? today.toString() : todayQt.passageDate(),
                todayQt.qtPassageId(),
                todayQt.title(),
                "READY",
                todayQt.simulatorStatus(),
                todayQt.hasExplanation(),
                todayQt.cacheStatus()
        );
    }

    private static AdminDashboardResponse.TodayQt missingTodayQt(LocalDate today) {
        return new AdminDashboardResponse.TodayQt(
                today.toString(),
                null,
                null,
                "MISSING",
                null,
                false,
                null
        );
    }

    private static AdminDashboardResponse.RecentAuditLog toRecentAuditLog(AdminDashboardAuditLog log) {
        return new AdminDashboardResponse.RecentAuditLog(
                log.id(),
                log.adminUserId(),
                log.actorType(),
                log.actionType(),
                log.targetType(),
                log.targetId(),
                log.createdAt()
        );
    }
}
