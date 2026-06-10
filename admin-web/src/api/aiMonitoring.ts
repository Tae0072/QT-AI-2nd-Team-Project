import { apiClient, unwrap } from './client';
import type { ApiResponse } from './types';

// ===== AD-08 AI 운영 모니터링 =====
// 연결 API: GET /api/v1/admin/ai/monitoring  (권한: OPERATOR — 집계만)
//   OPERATOR 는 실패율/대기 건수/차단 건수 같은 '집계'만 본다.
//   산출물 '원문' 조회는 AD-03(REVIEWER/SUPER_ADMIN)에서만 가능하다. (04 §AD-08 권한 상세)

export interface AiMonitoringSummary {
  [key: string]: unknown;
}

export interface AiMonitoringParams {
  from?: string;
  to?: string;
}

export function getAiMonitoring(params: AiMonitoringParams = {}) {
  return unwrap<AiMonitoringSummary>(
    apiClient.get<ApiResponse<AiMonitoringSummary>>('/admin/ai/monitoring', {
      params,
    }),
  );
}
