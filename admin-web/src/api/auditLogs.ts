import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-07 감사 로그 =====
// 연결 API: GET /api/v1/admin/audit-logs
//   필터 예: actorType=ADMIN, actionType=AI_ASSET_APPROVE, from, to
// 참고: 배치/AI 내부 작업 주체는 SYSTEM_BATCH 로 기록된다 (CLAUDE.md §5)

export interface AuditLog {
  id: number;
  [key: string]: unknown;
}

export interface AuditLogListParams extends PageParams {
  actorType?: string; // 예: ADMIN, SYSTEM_BATCH
  actionType?: string; // 예: AI_ASSET_APPROVE
  from?: string;
  to?: string;
}

export function listAuditLogs(params: AuditLogListParams = {}) {
  return unwrap<Page<AuditLog>>(
    apiClient.get<ApiResponse<Page<AuditLog>>>('/admin/audit-logs', { params }),
  );
}
