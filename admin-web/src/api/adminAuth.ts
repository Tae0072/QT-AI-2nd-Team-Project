import { apiClient, unwrap } from './client';
import type { ApiResponse } from './types';
import type { AdminRole } from '../constants/roles';

// ===== 관리자 자체 아이디/비밀번호 인증 =====
// 흐름: username/password → POST /api/v1/admin/auth/login → ADMIN access/refresh 발급.
// 2026-06-11 결정: 카카오 제거, admin-server(8090)가 직접 자격 검증(BCrypt) 후 발급.
// 응답: 카카오 시절과 동일하게 access/refresh + admin 요약(닉네임·세부역할 등).

export interface AdminLoginResponse {
  accessToken: string;
  refreshToken: string;
  admin: {
    memberId: number;
    nickname: string;
    role: string; // 항상 ADMIN
    adminRole: AdminRole; // 단일 역할 문자열
    status: string;
  };
}

// 아이디/비밀번호로 로그인해 ADMIN 토큰을 발급받는다.
// 자격 불일치는 서버가 401(ADMIN_LOGIN_FAILED)로 거절한다.
export function loginAdminWithPassword(username: string, password: string) {
  return unwrap<AdminLoginResponse>(
    apiClient.post<ApiResponse<AdminLoginResponse>>('/admin/auth/login', {
      username,
      password,
    }),
  );
}
