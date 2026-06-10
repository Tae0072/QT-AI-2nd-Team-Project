import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-04 신고 처리 =====
// 연결 API (권한: OPERATOR)
//   GET  /api/v1/admin/reports                    목록
//   POST /api/v1/admin/reports/{reportId}/resolve 처리(인정)
//   POST /api/v1/admin/reports/{reportId}/reject  반려

export interface Report {
  id: number;
  [key: string]: unknown;
}

export interface ReportListParams extends PageParams {
  targetType?: string; // 예: POST, COMMENT
  status?: string; // 예: RECEIVED, RESOLVED, REJECTED
}

export function listReports(params: ReportListParams = {}) {
  return unwrap<Page<Report>>(
    apiClient.get<ApiResponse<Page<Report>>>('/admin/reports', { params }),
  );
}

export function resolveReport(
  reportId: number,
  payload?: Record<string, unknown>,
) {
  return unwrap<Report>(
    apiClient.post<ApiResponse<Report>>(
      `/admin/reports/${reportId}/resolve`,
      payload,
    ),
  );
}

export function rejectReport(
  reportId: number,
  payload?: Record<string, unknown>,
) {
  return unwrap<Report>(
    apiClient.post<ApiResponse<Report>>(
      `/admin/reports/${reportId}/reject`,
      payload,
    ),
  );
}
