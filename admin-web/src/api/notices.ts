import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-06 시스템 공지 =====
// 연결 API
//   GET   /api/v1/admin/notices              목록
//   POST  /api/v1/admin/notices              등록
//   PATCH /api/v1/admin/notices/{id}         수정
//   POST  /api/v1/admin/notices/{id}/publish 발행
//   POST  /api/v1/admin/notices/{id}/hide    숨김

export interface Notice {
  id: number;
  [key: string]: unknown;
}

export function listNotices(params: PageParams = {}) {
  return unwrap<Page<Notice>>(
    apiClient.get<ApiResponse<Page<Notice>>>('/admin/notices', { params }),
  );
}

export function publishNotice(id: number) {
  return unwrap<Notice>(
    apiClient.post<ApiResponse<Notice>>(`/admin/notices/${id}/publish`),
  );
}
