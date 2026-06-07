import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-02 오늘 QT 관리 =====
// 연결 API (권한: OPERATOR)
//   GET   /api/v1/admin/qt-passages              목록
//   POST  /api/v1/admin/qt-passages              등록
//   PATCH /api/v1/admin/qt-passages/{id}         수정
//   POST  /api/v1/admin/qt-passages/{id}/publish 게시
//   POST  /api/v1/admin/qt-passages/{id}/hide    숨김
// 참고: QT 공개 00:00 KST, 사용자 노출·캐시 갱신 04:00 KST (CLAUDE.md §6)

// TODO: 실제 필드는 04_API_명세서 AD-02 / 백엔드 DTO에 맞춘다.
export interface QtPassage {
  id: number;
  [key: string]: unknown;
}

export interface QtPassageListParams extends PageParams {
  status?: string; // 예: PUBLISHED, DRAFT
  from?: string; // 예: 2026-05-01
  to?: string; // 예: 2026-05-31
}

export function listQtPassages(params: QtPassageListParams = {}) {
  return unwrap<Page<QtPassage>>(
    apiClient.get<ApiResponse<Page<QtPassage>>>('/admin/qt-passages', { params }),
  );
}

export function publishQtPassage(id: number) {
  return unwrap<QtPassage>(
    apiClient.post<ApiResponse<QtPassage>>(`/admin/qt-passages/${id}/publish`),
  );
}

export function hideQtPassage(id: number) {
  return unwrap<QtPassage>(
    apiClient.post<ApiResponse<QtPassage>>(`/admin/qt-passages/${id}/hide`),
  );
}
