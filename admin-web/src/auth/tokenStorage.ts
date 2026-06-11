// 관리자 토큰을 브라우저에 저장/조회/삭제하는 도우미.
// access(짧은 수명) + refresh(긴 수명, access 재발급용)를 함께 보관한다.
// 나중에 HttpOnly 쿠키 방식으로 바꿀 때 이 파일만 교체하면 된다.
const TOKEN_KEY = 'qtai_admin_token';
const REFRESH_KEY = 'qtai_admin_refresh_token';

// 저장된 access 토큰 읽기 (없으면 null)
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

// access 토큰 저장 (로그인 / 재발급)
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

// refresh 토큰 읽기 (없으면 null) — 자동 갱신에 사용
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY);
}

// refresh 토큰 저장 (로그인 / 회전 시)
export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_KEY, token);
}

// 세션 종료 시 access·refresh 둘 다 제거 (로그아웃 / 갱신 실패)
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
}
