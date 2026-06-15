import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-15 나눔 공유글 관리 (F-10) =====
// 연결 API (권한: OPERATOR / SUPER_ADMIN)
//   GET    /api/v1/admin/sharing-posts                목록·검색(전체 상태, 본문 미리보기)
//   GET    /api/v1/admin/sharing-posts/{id}           상세(전체 본문·절·QT날짜 포함)
//   PATCH  /api/v1/admin/sharing-posts/{id}/hide      숨김
//   PATCH  /api/v1/admin/sharing-posts/{id}/restore   숨김 복원(공개)
//
// 사용자용과 달리 PUBLISHED/HIDDEN/DELETED 전체를 본다. 작성자 식별은 닉네임 스냅샷만.
// 모더레이션은 숨김/복원만 제공한다(하드 삭제 없음 — 서버 정책과 일치).

export type SharingPostStatus = 'PUBLISHED' | 'HIDDEN' | 'DELETED';

export interface AdminSharingPost {
  id: number;
  memberId: number;
  nicknameSnapshot: string | null;
  titleSnapshot: string;
  category: string;
  status: SharingPostStatus;
  bodyPreview: string | null;
  body: string | null; // 상세 조회 시에만 채워짐(목록은 null)
  verseLabel: string | null;
  qtDate: string | null;
  commentsEnabled: boolean;
  likeCount: number;
  commentCount: number;
  hiddenAt: string | null;
  sourceNoteUnsharedAt: string | null;
  createdAt: string;
}

export interface SharingPostListParams extends PageParams {
  status?: SharingPostStatus;
  q?: string;
}

interface SpringPage<T> {
  content?: T[];
  number?: number;
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
}

function normalizePage<T>(page: SpringPage<T>, fallback: PageParams): Page<T> {
  return {
    content: Array.isArray(page.content) ? page.content : [],
    page: page.page ?? page.number ?? fallback.page ?? 0,
    size: page.size ?? fallback.size ?? 20,
    totalElements: page.totalElements ?? 0,
    totalPages: page.totalPages ?? 0,
  };
}

export async function listSharingPosts(params: SharingPostListParams = {}) {
  const page = await unwrap<SpringPage<AdminSharingPost>>(
    apiClient.get<ApiResponse<SpringPage<AdminSharingPost>>>('/admin/sharing-posts', {
      params,
    }),
  );
  return normalizePage(page, params);
}

export function getSharingPost(id: number) {
  return unwrap<AdminSharingPost>(
    apiClient.get<ApiResponse<AdminSharingPost>>(`/admin/sharing-posts/${id}`),
  );
}

export function hideSharingPost(id: number) {
  return unwrap<AdminSharingPost>(
    apiClient.patch<ApiResponse<AdminSharingPost>>(`/admin/sharing-posts/${id}/hide`),
  );
}

export function restoreSharingPost(id: number) {
  return unwrap<AdminSharingPost>(
    apiClient.patch<ApiResponse<AdminSharingPost>>(`/admin/sharing-posts/${id}/restore`),
  );
}
