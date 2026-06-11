import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-05 찬양 큐레이션 =====
// 연결 API (권한: OPERATOR, 04 §4.7.6)
//   GET    /api/v1/admin/praise-songs        목록   ✅
//   POST   /api/v1/admin/praise-songs        등록   ✅
//   PATCH  /api/v1/admin/praise-songs/{id}   수정   ✅
//   DELETE /api/v1/admin/praise-songs/{id}   삭제   ✅
//   숨김(POST /{id}/hide)은 v1 범위 제외.
//
// 🚫 금지(CLAUDE.md §8 / 07 F-09 / 04 §4.7.6):
//   가사·음원 파일·직접 YouTube URL 은 저장하지 않는다. 곡 메타데이터만 다룬다.

// 곡 출처. 관리자 큐레이션은 CURATED, 사용자 디바이스 음원은 DEVICE. (04 §8.2)
export type PraiseSongSourceType = 'CURATED' | 'DEVICE';
// 곡 노출 상태.
export type PraiseSongStatus = 'ACTIVE' | 'HIDDEN';

// 백엔드 찬양 곡 응답에 대응(04 §4.7.6). 가사·음원·URL 필드는 존재하지 않는다.
export interface PraiseSong {
  id: number;
  title: string;
  artist: string;
  sourceType: PraiseSongSourceType;
  licenseNote: string | null;
  status: PraiseSongStatus;
  createdAt: string;
  updatedAt: string | null;
}

export interface PraiseSongListParams extends PageParams {
  status?: PraiseSongStatus;
}

// 등록 요청 바디(04 §4.7.6). 메타데이터만 전송한다.
export interface CreatePraiseSongRequest {
  title: string;
  artist: string;
  sourceType: PraiseSongSourceType;
  licenseNote?: string;
  status: PraiseSongStatus;
}

// 수정 요청 바디(04 §4.7.6). title·artist·licenseNote 만 허용한다.
export interface UpdatePraiseSongRequest {
  title: string;
  artist: string;
  licenseNote?: string;
}

export function listPraiseSongs(params: PraiseSongListParams = {}) {
  return unwrap<Page<PraiseSong>>(
    apiClient.get<ApiResponse<Page<PraiseSong>>>('/admin/praise-songs', {
      params,
    }),
  );
}

export function createPraiseSong(payload: CreatePraiseSongRequest) {
  return unwrap<PraiseSong>(
    apiClient.post<ApiResponse<PraiseSong>>('/admin/praise-songs', payload),
  );
}

export function updatePraiseSong(id: number, payload: UpdatePraiseSongRequest) {
  return unwrap<PraiseSong>(
    apiClient.patch<ApiResponse<PraiseSong>>(`/admin/praise-songs/${id}`, payload),
  );
}

export function deletePraiseSong(id: number) {
  return apiClient.delete(`/admin/praise-songs/${id}`);
}
