import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-03 AI 산출물 검증 =====
// 연결 API (권한: REVIEWER / SUPER_ADMIN)
//   GET  /api/v1/admin/ai/assets              목록 (메타데이터만, 원문 미포함)
//   GET  /api/v1/admin/ai/assets/{id}         상세 (검수 권한 화면)
//   POST /api/v1/admin/ai/assets/{id}/approve 승인 (activateForTarget=게시)
//   POST /api/v1/admin/ai/assets/{id}/reject  반려
//   POST /api/v1/admin/ai/assets/{id}/hide    숨김
//   POST /api/v1/admin/ai/assets/{id}/regenerate 재생성 요청
// 주의: 승인 전 원문·검증 참조 자료는 사용자·일반 목록에 노출하지 않는다. (CLAUDE.md §7)

export interface PromptVersionSummary {
  id: number | null;
  promptType: string | null;
  version: string | null;
  status: string | null;
}

// 백엔드 ai/api/admin/asset/dto/AdminAiAssetListItem 과 1:1 (원문 미포함, 메타데이터만)
export interface AiAsset {
  id: number;
  assetType: string; // EXPLANATION / BIBLE_VERSE
  targetType: string | null;
  targetId: number | null;
  status: string; // VALIDATING / NEEDS_REVIEW / APPROVED / REJECTED / HIDDEN
  promptVersion: PromptVersionSummary | null;
  checklistVersionId: number | null;
  latestValidationResult: string | null;
  sourceLabelPresent: boolean;
  createdAt: string; // ISO
}

export interface AiAssetGenerationJobSummary {
  id: number;
  jobType: string;
  targetType: string | null;
  targetId: number | null;
  promptVersionId: number;
  status: string;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  errorMessage: string | null;
}

export interface AiValidationLog {
  validationLogId: number;
  validationReferenceJobId: number | null;
  checklistVersionId: number | null;
  layer: number;
  result: string;
  reviewerType: string;
  errorMessage: string | null;
  createdAt: string;
}

export interface AiAssetDetail {
  id: number;
  assetType: string;
  targetType: string | null;
  targetId: number | null;
  status: string;
  payloadJson: unknown;
  sourceLabel: string | null;
  createdAt: string;
  reviewedAt: string | null;
  generationJob: AiAssetGenerationJobSummary;
  promptVersion: PromptVersionSummary | null;
  validationLogs: AiValidationLog[];
}

export interface AiAssetListParams extends PageParams {
  assetType?: string;
  targetType?: string;
  status?: string;
}

// 백엔드 ReviewAiAssetResult
export interface ReviewAiAssetResult {
  assetId: number;
  status: string;
}

// approve/reject/hide 요청 본문 (백엔드 AdminAiAssetReviewRequest: reason, activateForTarget)
export interface ReviewPayload {
  reason?: string;
  activateForTarget?: boolean; // approve 시 대상에 게시(활성화) 여부
}

export interface RegeneratePayload {
  reason: string;
  promptVersionId: number;
}

export interface RegenerateAiAssetResult {
  generationJobId: number;
  status: string;
  createdAt: string;
}

export function listAiAssets(params: AiAssetListParams = {}) {
  return unwrap<Page<AiAsset>>(
    apiClient.get<ApiResponse<Page<AiAsset>>>('/admin/ai/assets', { params }),
  );
}

export function getAiAsset(assetId: number) {
  return unwrap<AiAssetDetail>(
    apiClient.get<ApiResponse<AiAssetDetail>>(`/admin/ai/assets/${assetId}`),
  );
}

export function approveAiAsset(assetId: number, payload?: ReviewPayload) {
  return unwrap<ReviewAiAssetResult>(
    apiClient.post<ApiResponse<ReviewAiAssetResult>>(
      `/admin/ai/assets/${assetId}/approve`,
      payload ?? {},
    ),
  );
}

export function regenerateAiAsset(assetId: number, payload: RegeneratePayload) {
  return unwrap<RegenerateAiAssetResult>(
    apiClient.post<ApiResponse<RegenerateAiAssetResult>>(
      `/admin/ai/assets/${assetId}/regenerate`,
      payload,
    ),
  );
}

export function rejectAiAsset(assetId: number, reason?: string) {
  return unwrap<ReviewAiAssetResult>(
    apiClient.post<ApiResponse<ReviewAiAssetResult>>(
      `/admin/ai/assets/${assetId}/reject`,
      { reason },
    ),
  );
}

export function hideAiAsset(assetId: number, reason?: string) {
  return unwrap<ReviewAiAssetResult>(
    apiClient.post<ApiResponse<ReviewAiAssetResult>>(
      `/admin/ai/assets/${assetId}/hide`,
      { reason },
    ),
  );
}
