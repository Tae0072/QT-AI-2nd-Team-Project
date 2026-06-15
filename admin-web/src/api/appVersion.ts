import { apiClient, unwrap } from './client';
import type { ApiResponse } from './types';

// ===== AD-19 앱 버전 / 업데이트 관리 (appversion 도메인, 2026-06-14 Lead 승인) =====
// 연결 API (권한: OPERATOR / SUPER_ADMIN)
//   GET    /api/v1/admin/app-updates/state                  현재 버전 상태
//   POST   /api/v1/admin/app-updates/apply-content          콘텐츠 버전 즉시 게시(0.1.0→0.1.0.1)
//   GET    /api/v1/admin/app-updates/pending?status=        업데이트 예정 목록
//   POST   /api/v1/admin/app-updates/pending                업데이트 예정 등록
//   POST   /api/v1/admin/app-updates/pending/{id}/apply     적용(앱 출시 버전 업데이트)
//   DELETE /api/v1/admin/app-updates/pending/{id}           업데이트 예정 삭제

export type AppUpdateMode = 'NONE' | 'RECOMMENDED' | 'FORCED';
export type PendingUpdateStatus = 'PENDING' | 'APPLIED';

export interface AppVersionState {
  contentVersion: string;
  appVersion: string;
  minSupportedVersion: string;
  updateMode: AppUpdateMode;
  updateMessage: string | null;
  updatedAt: string | null;
}

export interface PendingAppUpdate {
  id: number;
  title: string;
  description: string | null;
  targetAppVersion: string;
  updateMode: AppUpdateMode;
  status: PendingUpdateStatus;
  createdAt: string;
  appliedAt: string | null;
}

export interface PendingCreateInput {
  title: string;
  description?: string;
  targetAppVersion: string;
  updateMode?: AppUpdateMode;
}

export function getAppVersionState() {
  return unwrap<AppVersionState>(
    apiClient.get<ApiResponse<AppVersionState>>('/admin/app-updates/state'),
  );
}

// 콘텐츠 버전 즉시 게시(백그라운드 데이터 갱신, 앱 재설치 불필요).
export function applyContentVersion() {
  return unwrap<AppVersionState>(
    apiClient.post<ApiResponse<AppVersionState>>('/admin/app-updates/apply-content'),
  );
}

export function listPendingUpdates(status?: PendingUpdateStatus) {
  const query = status ? `?status=${status}` : '';
  return unwrap<PendingAppUpdate[]>(
    apiClient.get<ApiResponse<PendingAppUpdate[]>>(`/admin/app-updates/pending${query}`),
  );
}

export function createPendingUpdate(payload: PendingCreateInput) {
  return unwrap<PendingAppUpdate>(
    apiClient.post<ApiResponse<PendingAppUpdate>>('/admin/app-updates/pending', payload),
  );
}

// 적용 → 앱 출시 버전 업데이트(재설치 필요한 변경 반영).
export function applyPendingUpdate(id: number) {
  return unwrap<AppVersionState>(
    apiClient.post<ApiResponse<AppVersionState>>(`/admin/app-updates/pending/${id}/apply`),
  );
}

export function deletePendingUpdate(id: number) {
  return apiClient.delete(`/admin/app-updates/pending/${id}`);
}
