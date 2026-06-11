import { apiClient, unwrap } from './client';
import type { ApiResponse } from './types';
import type { AdminRole } from '../constants/roles';

// ===== 관리자 본인 정보 =====
// 로그인 토큰의 ADMIN 여부는 백엔드 SecurityFilterChain이 1차 검증하고,
// 이 API가 admin_users.admin_role 세부 권한을 반환한다.
export interface AdminMe {
  adminUserId: number;
  memberId: number;
  adminRole: AdminRole;
}

export function getAdminMe() {
  return unwrap<AdminMe>(apiClient.get<ApiResponse<AdminMe>>('/admin/me'));
}
