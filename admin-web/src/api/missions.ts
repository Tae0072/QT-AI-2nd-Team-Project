import { apiClient, unwrap } from './client';
import type { ApiResponse } from './types';

// ===== AD-16 미션 관리 (F-13) =====
// 연결 API (권한: CONTENT_CREATOR / OPERATOR / SUPER_ADMIN)
//   GET   /api/v1/admin/missions                 목록
//   GET   /api/v1/admin/missions/{id}            상세
//   POST  /api/v1/admin/missions                 생성
//   PATCH /api/v1/admin/missions/{id}            수정
//   PATCH /api/v1/admin/missions/{id}/status     상태(ACTIVE/HIDDEN) 변경

export type MissionMetricType =
  | 'MEDITATION_SAVED_DAYS'
  | 'NOTE_SAVED_COUNT'
  | 'STREAK_DAYS';
export type MissionPeriodType = 'DAILY' | 'WEEKLY' | 'MONTHLY';
export type MissionStatus = 'ACTIVE' | 'HIDDEN';

export interface AdminMission {
  id: number;
  code: string;
  title: string;
  metricType: MissionMetricType;
  periodType: MissionPeriodType;
  targetCount: number;
  status: MissionStatus;
  createdAt: string;
  updatedAt: string | null;
}

export interface MissionCreateRequest {
  code: string;
  title: string;
  metricType: MissionMetricType;
  periodType: MissionPeriodType;
  targetCount: number;
}

export interface MissionUpdateRequest {
  title?: string;
  metricType?: MissionMetricType;
  periodType?: MissionPeriodType;
  targetCount?: number;
}

export function listMissions() {
  return unwrap<AdminMission[]>(
    apiClient.get<ApiResponse<AdminMission[]>>('/admin/missions'),
  );
}

export function createMission(payload: MissionCreateRequest) {
  return unwrap<AdminMission>(
    apiClient.post<ApiResponse<AdminMission>>('/admin/missions', payload),
  );
}

export function updateMission(id: number, payload: MissionUpdateRequest) {
  return unwrap<AdminMission>(
    apiClient.patch<ApiResponse<AdminMission>>(`/admin/missions/${id}`, payload),
  );
}

export function changeMissionStatus(id: number, status: MissionStatus) {
  return unwrap<AdminMission>(
    apiClient.patch<ApiResponse<AdminMission>>(`/admin/missions/${id}/status`, null, {
      params: { value: status },
    }),
  );
}
