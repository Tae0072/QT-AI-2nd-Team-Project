import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-03 AI 산출물 검증 =====
// 연결 API (권한: REVIEWER / SUPER_ADMIN)
//   GET  /api/v1/admin/ai/assets                      목록
//   GET  /api/v1/admin/ai/assets/{assetId}            상세
//   POST /api/v1/admin/ai/assets/{assetId}/approve    승인
//   POST /api/v1/admin/ai/assets/{assetId}/reject     반려
//   POST /api/v1/admin/ai/assets/{assetId}/hide       숨김
//   POST /api/v1/admin/ai/assets/{assetId}/regenerate 재생성
// 주의: 승인 전 원문·검증 참조 자료는 사용자·일반 목록에 노출하지 않는다 (CLAUDE.md §7)

export interface AiAsset {
  id: number;
  [key: string]: unknown;
}

export interface AiAssetListParams extends PageParams {
  assetType?: string; // 예: EXPLANATION, BIBLE_VERSE
  status?: string; // 예: VALIDATING, APPROVED, REJECTED
}

export function listAiAssets(params: AiAssetListParams = {}) {
  return unwrap<Page<AiAsset>>(
    apiClient.get<ApiResponse<Page<AiAsset>>>('/admin/ai/assets', { params }),
  );
}

export function getAiAsset(assetId: number) {
  return unwrap<AiAsset>(
    apiClient.get<ApiResponse<AiAsset>>(`/admin/ai/assets/${assetId}`),
  );
}

export function approveAiAsset(assetId: number) {
  return unwrap<AiAsset>(
    apiClient.post<ApiResponse<AiAsset>>(`/admin/ai/assets/${assetId}/approve`),
  );
}

export function rejectAiAsset(assetId: number, reason?: string) {
  return unwrap<AiAsset>(
    apiClient.post<ApiResponse<AiAsset>>(`/admin/ai/assets/${assetId}/reject`, {
      reason,
    }),
  );
}
