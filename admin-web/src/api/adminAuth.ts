import { apiClient, unwrap } from './client';
import type { ApiResponse } from './types';
import type { AdminRole } from '../constants/roles';

// ===== 관리자 카카오 인증 =====
// 흐름: 카카오 access token → POST /api/v1/admin/auth/kakao → ADMIN 토큰 발급.
// 응답 계약: 2026-06-10 admin-kakao-auth-contract (이승욱 service-user) §3.
// 합의 사항: ① 응답 키 admin 블록 / ② adminRole 단일 문자열 / ③ refreshToken body / ⑤ access 30분·refresh 14일.

export interface AdminLoginResponse {
  accessToken: string;
  // 합의 ③: 앱·웹 모두 body 로 전달(HttpOnly 쿠키 미사용).
  refreshToken: string;
  // 합의 ①: 사용자 로그인은 member, 관리자는 admin 블록으로 구분.
  admin: {
    memberId: number;
    nickname: string;
    role: string; // 항상 ADMIN
    adminRole: AdminRole; // 합의 ②: 단일 역할 문자열
    status: string;
  };
}

// 카카오 access token 을 서버로 전달해 ADMIN 토큰을 발급받는다.
// 비관리자면 서버가 403 ADMIN_USER_NOT_FOUND 로 거절한다(합의 ④: ErrorCode 그대로 표시).
export function loginAdminWithKakao(kakaoAccessToken: string) {
  return unwrap<AdminLoginResponse>(
    apiClient.post<ApiResponse<AdminLoginResponse>>('/admin/auth/kakao', {
      kakaoAccessToken,
    }),
  );
}
