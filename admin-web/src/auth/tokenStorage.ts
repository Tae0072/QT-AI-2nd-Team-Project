// 관리자 토큰을 브라우저에 저장/조회/삭제하는 도우미.
// access(짧은 수명) + refresh(긴 수명, access 재발급용)를 함께 보관한다.
//
// ⚠️ 보안 메모 (코드리뷰 P4 — 의도적 임시 설계): 토큰을 localStorage에 둔다.
//   · 위험: XSS 발생 시 스크립트가 localStorage 토큰을 읽어 탈취 가능.
//   · 현재 단순성을 위해 localStorage 사용(의도적, dev/시연 단계).
//   · 운영 전 전환 계획: HttpOnly 쿠키(+CSRF 대응)로 이전 → JS가 토큰 접근 못 해
//     XSS 탈취 위험 제거. 서버(CORS credentials·쿠키 발급) 변경 필요 → 이승욱·강태오와
//     함께 결정(P2 refresh 흐름과 묶으면 변경 1회).
//   · 완료 조건: **운영 배포 전**. 전환 시점 = **데모 후(운영 전)** 결정(2026-06-11).
//     단 데모가 공개/실데이터 노출이면 데모 전으로 앞당긴다.
//   · 전환 시 이 파일(저장 계층)만 교체하면 호출부는 그대로다.
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
