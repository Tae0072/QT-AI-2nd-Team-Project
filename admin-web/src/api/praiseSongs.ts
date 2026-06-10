import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-05 찬양 큐레이션 =====
// 연결 API
//   GET   /api/v1/admin/praise-songs           목록
//   POST  /api/v1/admin/praise-songs           등록
//   PATCH /api/v1/admin/praise-songs/{id}      수정
//   POST  /api/v1/admin/praise-songs/{id}/hide 숨김
// 주의: 가사·음원 파일·직접 YouTube URL 은 저장하지 않는다 (CLAUDE.md §8)

export interface PraiseSong {
  id: number;
  [key: string]: unknown;
}

export interface PraiseSongListParams extends PageParams {
  status?: string; // 예: ACTIVE, HIDDEN
}

export function listPraiseSongs(params: PraiseSongListParams = {}) {
  return unwrap<Page<PraiseSong>>(
    apiClient.get<ApiResponse<Page<PraiseSong>>>('/admin/praise-songs', {
      params,
    }),
  );
}

export function hidePraiseSong(id: number) {
  return unwrap<PraiseSong>(
    apiClient.post<ApiResponse<PraiseSong>>(`/admin/praise-songs/${id}/hide`),
  );
}
