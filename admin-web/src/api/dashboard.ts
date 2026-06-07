import { apiClient, unwrap } from './client';
import type { ApiResponse } from './types';

// ===== AD-01 관리자 대시보드 =====
// 연결 API (권한: ADMIN)
//   GET /api/v1/admin/dashboard
// 화면: 운영 요약 지표(대기 중 검증 건수, 신고 건수, 오늘 QT 상태 등)

// TODO: 실제 응답 필드는 04_API_명세서 AD-01 / 백엔드 DTO에 맞춰 확정한다.
export interface DashboardSummary {
  [key: string]: unknown;
}

export function getDashboard() {
  return unwrap<DashboardSummary>(
    apiClient.get<ApiResponse<DashboardSummary>>('/admin/dashboard'),
  );
}
