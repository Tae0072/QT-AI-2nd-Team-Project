import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL, IS_DEV, DEV_ADMIN_MEMBER_ID } from '../config/env';
import {
  getToken,
  setToken,
  getRefreshToken,
  setRefreshToken,
  clearToken,
} from '../auth/tokenStorage';
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

// 세션 만료 시 호출할 콜백을 담는 슬롯.
// client.ts는 React/라우팅을 모르는 데이터 계층이라, 직접 화면을 못 바꾼다.
// 대신 AuthContext가 여기에 정리 함수(clearSession)를 등록(setAuthExpiredHandler)해두고,
// refresh가 최종 실패하면 이 콜백을 호출해 SPA가 /login으로 가게 한다.
let onAuthExpired: (() => void) | null = null;
export function setAuthExpiredHandler(fn: (() => void) | null): void {
  onAuthExpired = fn;
}

function endSession(): void {
  clearToken(); // access·refresh 모두 제거
  onAuthExpired?.(); // 등록돼 있으면 AuthContext 정리 → ProtectedRoute가 /login
}

// ===== 모든 API 호출이 함께 쓰는 axios 인스턴스 =====
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// [요청 전] 저장된 access 토큰을 Authorization 헤더에 자동 첨부
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

// ===== 토큰 자동 갱신 (single-flight) =====
// 공용 POST /api/v1/auth/refresh 재사용(계약서 §6). 재발급 access의 role=ADMIN은 서버가 유지.
interface RefreshResponse {
  accessToken: string;
  refreshToken?: string; // 회전(rotation) 시 새 refresh가 올 수 있음 → 있으면 갱신
}

// 인터셉터 없는 순수 axios로 호출해야 재귀(갱신 요청이 또 인터셉터를 타는 것)를 막는다.
async function requestNewAccessToken(): Promise<string> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) throw new Error('NO_REFRESH_TOKEN');
  const res = await axios.post<ApiResponse<RefreshResponse>>(
    `${API_BASE_URL}/auth/refresh`,
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } },
  );
  const data = res.data?.data;
  if (!res.data?.success || !data?.accessToken) throw new Error('REFRESH_FAILED');
  setToken(data.accessToken);
  if (data.refreshToken) setRefreshToken(data.refreshToken);
  return data.accessToken;
}

// single-flight: 동시 다발 401이 와도 refresh는 한 번만. 나머지는 같은 Promise를 기다린다.
let refreshing: Promise<string> | null = null;
function refreshOnce(): Promise<string> {
  refreshing ??= requestNewAccessToken().finally(() => {
    refreshing = null;
  });
  return refreshing;
}

// [응답 후] 401이면 refresh로 1회 자동 갱신 후 원요청 재시도. 그래도 안 되면 세션 종료.
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status;
    const apiError = error.response?.data?.error;
    const original = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined;

    // 인증 엔드포인트(로그인·갱신) 자체의 401은 재시도하지 않는다(무한루프 방지).
    const url = original?.url ?? '';
    const isAuthEndpoint = url.includes('/auth/refresh') || url.includes('/admin/auth/');

    if (status === 401 && original && !original._retry && !isAuthEndpoint && getRefreshToken()) {
      // access 만료로 추정 → refresh로 새 access 받아 같은 요청을 한 번 더.
      original._retry = true;
      try {
        const newToken = await refreshOnce();
        original.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(original);
      } catch {
        // refresh 자체가 실패(만료·무효·탈퇴 M0006·정지 M0007) → 재로그인 필요.
        endSession();
      }
    } else if (status === 401) {
      // 재시도 후에도 401 / refresh 토큰 없음 / 인증 엔드포인트 401 → 세션 종료.
      endSession();
    }

    const message =
      apiError?.message ?? error.message ?? '알 수 없는 오류가 발생했습니다.';
    return Promise.reject(new ApiClientError(message, status, apiError?.code));
  },
);

// 공통 봉투({ success, data, error })에서 data 만 꺼내는 도우미.
export async function unwrap<T>(
  promise: Promise<{ data: ApiResponse<T> }>,
): Promise<T> {
  const res = await promise;
  if (!res.data.success || res.data.data === null) {
    throw new Error(res.data.error?.message ?? '응답이 올바르지 않습니다.');
  }
  return res.data.data;
}
