import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-04 신고 처리 =====
// 연결 API (권한: OPERATOR / SUPER_ADMIN)
//   GET  /api/v1/admin/reports                    목록
//   POST /api/v1/admin/reports/{reportId}/resolve 처리(인정)
//   POST /api/v1/admin/reports/{reportId}/reject  반려

// 백엔드 report/api/dto/AdminReportListResponse.Item 과 1:1 대응 (관리자 전용 필드)
export interface Report {
  id: number;
  reporterMemberId: number | null;
  targetType: string; // POST/COMMENT/AI_QA_REQUEST/AI_ASSET
  targetId: number | null;
  reason: string | null;
  detail: string | null;
  status: string; // RECEIVED/REVIEWING/RESOLVED/REJECTED
  processedByAdminId: number | null;
  processedAt: string | null; // ISO
  createdAt: string; // ISO
}

export interface ReportListParams extends PageParams {
  targetType?: string; // POST/COMMENT/AI_QA_REQUEST/AI_ASSET
  status?: string; // RECEIVED/REVIEWING/RESOLVED/REJECTED
}

// resolve/reject 요청 본문 (백엔드 ProcessReportRequest: reason, notifyReporter)
export interface ProcessReportPayload {
  action?: 'HIDE_TARGET';
  reason?: string;
  notifyReporter?: boolean;
}

// 처리 결과 (백엔드 ProcessReportResult)
export interface ProcessReportResult {
  reportId: number;
  status: string;
  processedByAdminId: number | null;
  processedAt: string | null;
}

export function listReports(params: ReportListParams = {}) {
  return unwrap<Page<Report>>(
    apiClient.get<ApiResponse<Page<Report>>>('/admin/reports', { params }),
  );
}

export function resolveReport(reportId: number, payload?: ProcessReportPayload) {
  return unwrap<ProcessReportResult>(
    apiClient.post<ApiResponse<ProcessReportResult>>(
      `/admin/reports/${reportId}/resolve`,
      payload ?? {},
    ),
  );
}

export function rejectReport(reportId: number, payload?: ProcessReportPayload) {
  return unwrap<ProcessReportResult>(
    apiClient.post<ApiResponse<ProcessReportResult>>(
      `/admin/reports/${reportId}/reject`,
      payload ?? {},
    ),
  );
}

// ===== AD-18 자가진단(셀프테스트) =====
//   POST /api/v1/admin/reports/test-seed          자가진단용 테스트 신고 생성

// 테스트 신고 생성 결과 (백엔드 ReportResponse: id/status/createdAt)
export interface ReportSeedResult {
  id: number;
  status: string;
  createdAt: string;
}

// 자가진단용 테스트 신고 생성 — 백엔드가 COMMENT 대상 + 고유 targetId + 사유 TEST로 접수.
export function seedTestReport() {
  return unwrap<ReportSeedResult>(
    apiClient.post<ApiResponse<ReportSeedResult>>('/admin/reports/test-seed'),
  );
}
