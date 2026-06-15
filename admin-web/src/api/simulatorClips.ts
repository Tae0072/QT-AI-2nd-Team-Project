import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-14 시뮬레이터 관리 (조회 + 숨김) =====
// 연결 API:
//   GET  /api/v1/admin/simulator-clips            목록(상태/본문 필터)
//   POST /api/v1/admin/simulator-clips/{aiAssetId}/hide  숨김
// 주의: 원문(sceneScriptJson)은 목록에 포함하지 않는다. 게시(Publish)는 후속.

export interface SimulatorClip {
  id: number;
  qtPassageId: number;
  title: string;
  status: string; // PENDING / APPROVED / REJECTED / HIDDEN
  aiAssetId: number | null;
  approvedAt: string | null;
}

export interface SimulatorClipListParams extends PageParams {
  status?: string;
  qtPassageId?: number;
}

export interface HideSimulatorClipResult {
  aiAssetId: number;
  hiddenCount: number;
}

export function listSimulatorClips(params: SimulatorClipListParams = {}) {
  return unwrap<Page<SimulatorClip>>(
    apiClient.get<ApiResponse<Page<SimulatorClip>>>('/admin/simulator-clips', { params }),
  );
}

export function hideSimulatorClip(aiAssetId: number) {
  return unwrap<HideSimulatorClipResult>(
    apiClient.post<ApiResponse<HideSimulatorClipResult>>(
      `/admin/simulator-clips/${aiAssetId}/hide`,
      {},
    ),
  );
}
