// 관리자 토큰을 브라우저에 저장/조회/삭제하는 도우미.
// 지금은 '임시 토큰 입력' 방식이라 localStorage 에 보관한다.
// 나중에 카카오 웹 로그인 / 서버측 OAuth 로 바꿀 때 이 파일만 교체하면 된다.
const TOKEN_KEY = 'qtai_admin_token';

// 저장된 토큰 읽기 (없으면 null)
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

// 토큰 저장 (로그인)
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

// 토큰 삭제 (로그아웃 / 토큰 만료)
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}
