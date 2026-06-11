import axios, { AxiosError } from 'axios';
import { API_BASE_URL, IS_DEV, DEV_ADMIN_MEMBER_ID } from '../config/env';
import { getToken, clearToken } from '../auth/tokenStorage';
import type { ApiResponse } from './types';

export class ApiClientError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
    public readonly code?: string,
  ) {
    super(message);
    this.name = 'ApiClientError';
  }
}

// ===== 모든 API 호출이 함께 쓰는 axios 인스턴스 =====
// - baseURL    : 모든 요청 앞에 붙는 공통 주소 (예: /api/v1)
// - 요청 가로채기 : 저장된 ADMIN 토큰을 Authorization 헤더에 자동으로 붙인다.
// - 응답 가로채기 : 에러를 사람이 읽기 좋은 메시지로 정리하고, 401이면 토큰을 비운다.
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// [요청 보내기 전] 토큰이 있으면 'Authorization: Bearer {token}' 자동 첨부
apiClient.interceptors.request.use((config) => {
  const token = getToken();
  // dev에선 정식 토큰 발급 경로가 없어 가짜 토큰('dev-bypass')을 저장하므로 Bearer로 보내지 않는다.
  // (가짜 토큰을 보내면 JwtAuthenticationFilter가 JWT 파싱 실패로 401을 낸다. dev 인증은 X-Dev-* 헤더가 담당.)
  if (token && !IS_DEV) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  // [DEV 전용] dev 서버(dev-bypass)는 정식 토큰 발급 경로가 없으므로,
  // 관리자 식별 헤더를 보낸다: X-Dev-User-Id(회원 id) + X-Dev-Roles(ADMIN → ROLE_ADMIN).
  // prod 빌드에서는 IS_DEV=false 라 절대 첨부되지 않는다.
  if (IS_DEV) {
    // axios v1: 커스텀 헤더는 .set()으로 넣어야 실제 요청에 확실히 직렬화된다.
    // (AxiosHeaders 인스턴스에 대괄호 대입은 요청에서 누락될 수 있음)
    config.headers.set('X-Dev-User-Id', DEV_ADMIN_MEMBER_ID);
    config.headers.set('X-Dev-Roles', 'ADMIN');
  }
  return config;
});

// [응답 받은 후] 에러 공통 처리
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status;
    const apiError = error.response?.data?.error;

    // 토큰 만료/인증 실패면 저장 토큰을 비워 로그인 화면으로 유도한다.
    if (status === 401) {
      clearToken();
    }

    const message =
      apiError?.message ?? error.message ?? '알 수 없는 오류가 발생했습니다.';
    return Promise.reject(new ApiClientError(message, status, apiError?.code));
  },
);

// 공통 봉투({ success, data, error })에서 data 만 꺼내는 도우미.
// 각 도메인 API 함수는 이 함수를 통해 실제 데이터만 돌려받는다.
export async function unwrap<T>(
  promise: Promise<{ data: ApiResponse<T> }>,
): Promise<T> {
  const res = await promise;
  if (!res.data.success || res.data.data === null) {
    throw new Error(res.data.error?.message ?? '응답이 올바르지 않습니다.');
  }
  return res.data.data;
}
