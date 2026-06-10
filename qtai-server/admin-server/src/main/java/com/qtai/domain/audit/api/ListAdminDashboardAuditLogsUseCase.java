package com.qtai.domain.audit.api;

import java.util.List;

import com.qtai.domain.audit.api.dto.AdminDashboardAuditLog;

/**
 * 관리자 대시보드용 최근 감사 로그 조회 UseCase.
 */
public interface ListAdminDashboardAuditLogsUseCase {

    List<AdminDashboardAuditLog> listRecentAuditLogs(int size);
}
