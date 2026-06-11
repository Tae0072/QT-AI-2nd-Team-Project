package com.qtai.domain.report.api;

import com.qtai.domain.report.api.dto.AdminReportListQuery;
import com.qtai.domain.report.api.dto.AdminReportListResponse;

/**
 * 관리자 신고 목록 조회 UseCase 포트.
 *
 * <p>API 명세서 §4.7.4 (GET /api/v1/admin/reports). OPERATOR/SUPER_ADMIN 권한.
 */
public interface ListAdminReportsUseCase {

    AdminReportListResponse listReports(AdminReportListQuery query);
}
