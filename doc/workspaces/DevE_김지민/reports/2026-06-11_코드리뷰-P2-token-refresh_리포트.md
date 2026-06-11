# Report — 2026-06-11 코드리뷰 P2 admin-web 토큰 자동 갱신

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김지민 (admin-web FE) |
| 브랜치 | `feature/admin-web-token-refresh` (base `origin/dev`) |
| PR 링크 | (PR 생성 후 작성) |
| 출처 TODO | `2026-06-10_코드리뷰_TODO_김지민.md` TODO 2 (P2) |
| 관련 F-ID | F-04 |
| 계약서 | `DevD_이승욱/contracts/2026-06-10_admin-kakao-auth-api-contract.md` §6 (2026-06-11 개정) |
| 워크플로우 | [workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md](../workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md) |

## 변경 내용

access 토큰 만료 시 공용 `POST /api/v1/auth/refresh`로 **자동 갱신 + 원요청 재시도**를 구현. 기존엔 401이면 토큰만 비워 30분마다 강제 재로그인이었음.

- `tokenStorage.ts`: `getRefreshToken`/`setRefreshToken` 추가. `clearToken`은 access+refresh **둘 다** 제거(세션 종료).
- `client.ts`:
  - 응답 인터셉터에 **single-flight refresh** — 동시 다발 401에 refresh는 1회만(공유 Promise), `_retry` 플래그로 무한루프 방지, `/auth/refresh`·`/admin/auth/`는 재시도 제외. 재발급 호출은 인터셉터 없는 순수 axios로 해 재귀 차단.
  - `setAuthExpiredHandler(fn)` export — 세션 만료 시 호출할 콜백 슬롯.
- `AuthContext.tsx`: `login(access, refresh?)`로 확장(둘 다 저장). `useEffect`로 `setAuthExpiredHandler(clearSession)` 등록.
- `LoginPage.tsx`: 카카오 로그인 시 `login(res.accessToken, res.refreshToken)`. (dev 토큰은 access만)
- `vite.config.ts`: **`/api/v1/auth` → service-user(8081)** 프록시 규칙 추가(refresh도 JWT 발급자 소관). P1의 `/api/v1/admin/auth`→8081과 동일 취지.

### 설계 결정 — 리다이렉트 방식 = B(AuthContext 콜백)

refresh 최종 실패(만료·무효·M0006 탈퇴·M0007 정지) 시 로그인 화면 이동을, `window.location` 하드 리로드(A) 대신 **B(콜백)** 로 구현:
- `client.ts`는 React/라우팅 비결합 유지 → 콜백만 호출. `AuthContext`가 `clearSession`을 등록 → 토큰 정리 → `ProtectedRoute`가 `/login`으로(**새로고침 없음**).
- 근거: 프로젝트가 ProtectedRoute SPA 네비게이션을 쓰고, AuthContext가 인증 상태 단일 소유자. 기존 401→clearToken 시 AuthContext 상태가 안 비던 **desync도 해소**.

## 검증 결과

- `npm run typecheck`(에러 0) + `npm run build`(✓) 통과.
- **실데이터 E2E 미수행** — service-user(8081)+admin-server(8090) 풀스택 + 관리자 토큰 필요(P1~P3과 동일 블로커). 동작 흐름은 계약(§6) 기준 구현.

## CI / 자동 리뷰 / 조율

- 자가 리뷰: 금지패턴·secret 위반 없음, typecheck+build 통과. 브랜치명 `feature/` 준수.
- ⚠️ **vite 프록시 `/api/v1/auth`→8081 추가**: P1의 인증 분리(결정③ 공존)와 동일 취지의 dev 전용 라우팅. 이승욱/강태오에 "refresh 경로도 8081로 분리" 공유(P1과 묶이는 건). 게이트웨이 뜨면 통합.

## 남은 리스크 / 후속

- ✅ **refresh 토큰 회전(rotation) — 백엔드 코드로 확인 완료**: `service-user/AuthService.refresh()`가 새 토큰 쌍을 발급(rotation)하고 응답 `LoginResponse`에 **새 `refreshToken`을 body로 반환**, 기존 refresh는 `RefreshTokenStore`(Redis)에서 교체·무효화(단일 세션). FE는 `data.refreshToken`을 매번 저장하므로 **그대로 정상 — 추가 수정 불필요**. (참고: 같은 계정을 **다중 탭**에서 동시 갱신하면 rotation+단일세션 특성상 한쪽이 INVALID_REFRESH_TOKEN으로 로그아웃될 수 있음 — admin 도구 특성상 영향 미미, 필요 시 후속.)
- 실데이터 스모크: access 만료→자동 갱신→화면 유지 / refresh 만료→로그인 이동을 풀스택에서 확인.
- (선택) 로그아웃 시 서버 `POST /auth/logout` 호출로 refresh 무효화(현재는 클라이언트 토큰만 제거).
- 로드맵 다음: P5c → P5b → P4. (P5a 찬양 숨김 보류)
