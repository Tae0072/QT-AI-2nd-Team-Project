import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-12 회원 관리 (F-04 / F-10) =====
// 연결 API (권한: OPERATOR / SUPER_ADMIN)
//   GET   /api/v1/admin/members                  목록·검색
//   GET   /api/v1/admin/members/{id}             기본 정보
//   GET   /api/v1/admin/members/{id}/detail      상세(닉네임 변경 시각·신고/나눔 집계)
//   PATCH /api/v1/admin/members/{id}/status      정지/정지해제
//
// 개인정보 최소노출: kakaoId·email 원문은 응답에 없다(닉네임·상태·권한만).

export type MemberStatus = 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN';

export interface AdminMember {
  id: number;
  nickname: string;
  status: MemberStatus;
  role: string;
  nicknameChangedAt: string | null;
  withdrawnAt: string | null;
  createdAt: string;
}

export interface MemberListParams extends PageParams {
  status?: MemberStatus;
  q?: string;
}

export interface MemberStatusUpdateRequest {
  status: 'ACTIVE' | 'SUSPENDED';
}

// 회원 상세 — 닉네임 변경 시각 + 나눔/신고 집계
export interface AdminMemberDetail {
  id: number;
  nickname: string;
  status: MemberStatus;
  role: string;
  nicknameChangedAt: string | null;
  withdrawnAt: string | null;
  createdAt: string;
  sharingPostCount: number;
  reportsFiledCount: number;
  reportsReceivedCount: number;
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

export async function listMembers(params: MemberListParams = {}) {
  const page = await unwrap<SpringPage<AdminMember>>(
    apiClient.get<ApiResponse<SpringPage<AdminMember>>>('/admin/members', { params }),
  );
  return normalizePage(page, params);
}

export function getMember(id: number) {
  return unwrap<AdminMember>(
    apiClient.get<ApiResponse<AdminMember>>(`/admin/members/${id}`),
  );
}

export function getMemberDetail(id: number) {
  return unwrap<AdminMemberDetail>(
    apiClient.get<ApiResponse<AdminMemberDetail>>(`/admin/members/${id}/detail`),
  );
}

export function updateMemberStatus(id: number, payload: MemberStatusUpdateRequest) {
  return unwrap<AdminMember>(
    apiClient.patch<ApiResponse<AdminMember>>(`/admin/members/${id}/status`, payload),
  );
}

// ===== 회원 상세 추가 조회 (노트·공유글·댓글·좋아요·미션) =====
//   GET /api/v1/admin/members/{id}/notes|posts|comments|likes|missions

export interface AdminNoteItem {
  id: number;
  qtPassageId: number | null;
  category: string | null;
  status: string | null;
  visibility: string | null;
  title: string | null;
  createdAt: string;
}

export interface AdminMemberPostItem {
  id: number;
  status: string | null;
  title: string | null;
  category: string | null;
  createdAt: string;
}

export interface AdminMemberCommentItem {
  id: number;
  sharingPostId: number;
  body: string | null;
  deleted: boolean;
  createdAt: string;
}

export interface AdminMemberLikedPostItem {
  postId: number;
  title: string | null;
  status: string | null;
  likedAt: string;
}

export interface MissionProgress {
  missionDefinitionId: number;
  code: string;
  title: string;
  metricType: string;
  periodType: string;
  currentCount: number;
  targetCount: number;
  progressRate: number;
  completed: boolean;
  periodStartDate: string | null;
  periodEndDate: string | null;
  completedAt: string | null;
}

async function listSub<T>(id: number, sub: string, params: PageParams): Promise<Page<T>> {
  const page = await unwrap<SpringPage<T>>(
    apiClient.get<ApiResponse<SpringPage<T>>>(`/admin/members/${id}/${sub}`, { params }),
  );
  return normalizePage(page, params);
}

export function listMemberNotes(id: number, params: PageParams = {}) {
  return listSub<AdminNoteItem>(id, 'notes', params);
}

export function listMemberPosts(id: number, params: PageParams = {}) {
  return listSub<AdminMemberPostItem>(id, 'posts', params);
}

export function listMemberComments(id: number, params: PageParams = {}) {
  return listSub<AdminMemberCommentItem>(id, 'comments', params);
}

export function listMemberLikes(id: number, params: PageParams = {}) {
  return listSub<AdminMemberLikedPostItem>(id, 'likes', params);
}

export function getMemberMissions(id: number) {
  return unwrap<MissionProgress[]>(
    apiClient.get<ApiResponse<MissionProgress[]>>(`/admin/members/${id}/missions`),
  );
}
