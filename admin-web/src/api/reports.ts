import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-04 신고 처리 =====
// 연결 API (권한: OPERATOR)
//   GET  /api/v1/admin/reports                    목록
//   POST /api/v1/admin/reports/{reportId}/resolve 처리(인정)
//   POST /api/v1/admin/reports/{reportId}/reject  반려

// 백엔드 신고 응답에 대응(04 §4.7.4). 미처리 신고는 processed* 가 null.
export interface Report {
  id: number;
  reporterMemberId: number | null;
  targetType: string; // POST, COMMENT, AI_QA_REQUEST, AI_ASSET
  targetId: number | null;
  reason: string; // INAPPROPRIATE, SPAM 등
  detail: string | null;
  status: string; // RECEIVED, RESOLVED, REJECTED
  processedByAdminId: number | null;
  processedAt: string | null;
  createdAt: string;
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
