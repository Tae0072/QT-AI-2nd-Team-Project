import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-05 찬양 큐레이션 =====
// 연결 API (권한: OPERATOR, 04 §4.7.6)
//   GET   /api/v1/admin/praise-songs           목록   ✅ 구현됨
//   POST  /api/v1/admin/praise-songs           등록   ✅ 구현됨
//   PATCH /api/v1/admin/praise-songs/{id}      수정   ❌ 백엔드 미구현(대기)
//   POST  /api/v1/admin/praise-songs/{id}/hide 숨김   ❌ 백엔드 미구현(대기)
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
  title: string; // 곡명
  artist: string; // 아티스트명
  sourceType: PraiseSongSourceType;
  licenseNote: string | null; // 운영자가 남긴 저작권 확인 메모
  status: PraiseSongStatus;
  createdAt: string;
  updatedAt: string | null;
}

export interface PraiseSongListParams extends PageParams {
  status?: PraiseSongStatus; // 예: ACTIVE, HIDDEN
}

// 등록 요청 바디(04 §4.7.6). 메타데이터만 전송한다.
export interface CreatePraiseSongRequest {
  title: string;
  artist: string;
  sourceType: PraiseSongSourceType;
  licenseNote?: string;
  status: PraiseSongStatus;
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

// 숨김 — 백엔드 미구현. hide 엔드포인트가 나오면 화면에 연결한다.
export function hidePraiseSong(id: number) {
  return unwrap<PraiseSong>(
    apiClient.post<ApiResponse<PraiseSong>>(`/admin/praise-songs/${id}/hide`),
  );
}
