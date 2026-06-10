import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-10 AI 배치 실행 로그 =====
// 연결 API (권한: OPERATOR / REVIEWER / SUPER_ADMIN, AdminAiAuthentication.requireMonitoring)
//   GET /api/v1/admin/ai/batch-run-logs   목록 (필터: batchName, status, from, to)
// 참고: AI 해설/시뮬레이터 사전 생성·검증 배치의 실행 결과를 추적한다(읽기 전용, 원문 미포함).

// 배치 실행 상태 (백엔드 AiBatchRunStatus)
export type BatchRunStatus = 'SUCCEEDED' | 'PARTIAL_FAILED' | 'FAILED';

// 백엔드 AdminAiBatchRunLogItem 과 1:1 대응
export interface AiBatchRunLog {
  id: number;
  batchName: string;
  status: string; // SUCCEEDED / PARTIAL_FAILED / FAILED
  createdCount: number; // 생성 건수
  failedCount: number; // 실패 건수
  processedCount: number; // 처리 건수
  errorType: string | null;
  errorMessage: string | null;
  startedAt: string | null; // ISO
  finishedAt: string | null; // ISO
  createdAt: string; // ISO
}

export interface AiBatchRunLogListParams extends PageParams {
  batchName?: string;
  status?: string;
  from?: string; // 예: 2026-06-01
  to?: string; // 예: 2026-06-30
}

export function listAiBatchRunLogs(params: AiBatchRunLogListParams = {}) {
  return unwrap<Page<AiBatchRunLog>>(
    apiClient.get<ApiResponse<Page<AiBatchRunLog>>>(
      '/admin/ai/batch-run-logs',
      { params },
    ),
  );
}
