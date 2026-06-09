import { apiClient, unwrap } from './client';
import type { ApiResponse } from './types';

// ===== AD-08 AI 운영 모니터링 =====
// GET /api/v1/admin/ai/monitoring (권한: OPERATOR — 집계만, 원문 미포함)
// 백엔드 ai/api/admin/monitoring/dto/AdminAiMonitoringResponse 와 1:1.

export interface AiMonitoringParams {
  from?: string;
  to?: string;
}

export interface AiMonitoringPeriod {
  from: string | null;
  to: string | null;
  timezone: string | null;
}

export interface GenerationJobs {
  queued: number;
  running: number;
  succeeded: number;
  failed: number;
}

export interface FailureReason {
  resultCode: string;
  count: number;
}

export interface ValidationSummary {
  waitingAssets: number;
  passCount: number;
  failCount: number;
  needsReviewCount: number;
  failureReasons: FailureReason[];
}

export interface BatchRunFailure {
  id: number;
  batchName: string;
  status: string;
  errorType: string | null;
  errorMessage: string | null;
  createdAt: string;
}

export interface BatchRuns {
  succeeded: number;
  partialFailed: number;
  failed: number;
  latestFailures: BatchRunFailure[];
}

export interface BlockedReason {
  blockedReason: string;
  count: number;
}

export interface QaSummary {
  requested: number;
  answered: number;
  blocked: number;
  failed: number;
  blockedReasons: BlockedReason[];
}

export interface ChecklistSummary {
  checklistType: string;
  activeVersion: string | null;
  passRate: number; // 0..1
}

export interface AiMonitoringSummary {
  period: AiMonitoringPeriod;
  generationJobs: GenerationJobs;
  validation: ValidationSummary;
  batchRuns: BatchRuns;
  qa: QaSummary;
  checklists: ChecklistSummary[];
}

export function getAiMonitoring(params: AiMonitoringParams = {}) {
  return unwrap<AiMonitoringSummary>(
    apiClient.get<ApiResponse<AiMonitoringSummary>>('/admin/ai/monitoring', {
      params,
    }),
  );
}
