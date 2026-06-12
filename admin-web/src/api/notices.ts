import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-06 시스템 공지 =====
// 연결 API (권한: ADMIN + OPERATOR/SUPER_ADMIN)
//   GET   /api/v1/admin/notices              목록(정렬 createdAt,desc 고정)
//   GET   /api/v1/admin/notices/{id}         상세(전체 body 포함)
//   POST  /api/v1/admin/notices              등록(201, 상세 반환)
//   PATCH /api/v1/admin/notices/{id}         수정(200, DRAFT만 / status 보내면 400)
//   POST  /api/v1/admin/notices/{id}/publish 발행(200, 알림 fan-out 결과 반환)
//   POST  /api/v1/admin/notices/{id}/hide    숨김(204)
//
// 정책(DevC_강상민 2026-06-10_admin-notices-api_report): 본문은 plain text만(`<`,`>` 거부),
// 발행 시 활성 회원에게 notifications.type=NOTICE fan-out.

export type NoticeStatus = 'DRAFT' | 'PUBLISHED' | 'HIDDEN';

// 목록 Item(미리보기). 상세 조회는 NoticeDetail이 전체 body를 반환한다.
export interface Notice {
  id: number;
  title: string;
  bodyPreview: string;
  status: NoticeStatus;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
}

// 등록/수정 응답(상세) — 전체 body 포함.
export interface NoticeDetail {
  id: number;
  title: string;
  body: string;
  status: NoticeStatus;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface NoticeRequest {
  title: string;
  body: string;
}

// 발행 응답 — 알림 fan-out 결과 포함.
export interface NoticePublishResult {
  requestedCount: number;
  createdCount: number;
  failedCount: number;
}
export interface NoticePublishResponse {
  id: number;
  status: NoticeStatus;
  publishedAt: string | null;
  notificationResult: NoticePublishResult;
}

export function listNotices(params: PageParams = {}) {
  return unwrap<Page<Notice>>(
    apiClient.get<ApiResponse<Page<Notice>>>('/admin/notices', { params }),
  );
}

export function getNotice(id: number) {
  return unwrap<NoticeDetail>(
    apiClient.get<ApiResponse<NoticeDetail>>(`/admin/notices/${id}`),
  );
}

export function createNotice(body: NoticeRequest) {
  return unwrap<NoticeDetail>(
    apiClient.post<ApiResponse<NoticeDetail>>('/admin/notices', body),
  );
}

export function updateNotice(id: number, body: NoticeRequest) {
  return unwrap<NoticeDetail>(
    apiClient.patch<ApiResponse<NoticeDetail>>(`/admin/notices/${id}`, body),
  );
}

export function publishNotice(id: number) {
  return unwrap<NoticePublishResponse>(
    apiClient.post<ApiResponse<NoticePublishResponse>>(`/admin/notices/${id}/publish`),
  );
}

// 숨김은 204(No Content) — envelope 없음. 본문을 읽지 않는다.
export async function hideNotice(id: number): Promise<void> {
  await apiClient.post(`/admin/notices/${id}/hide`);
}
