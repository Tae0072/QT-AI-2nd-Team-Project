# 2026-06-11 admin-web dev-bypass 재도입 (FE/BE 짝 복원)

## 배경
- PR #459(`revert/pr459-admin-web`) 롤백 이후 admin-web 로컬 로그인이 완전히 막힘.
- 증상: 개발용 토큰으로 로그인해도 모든 관리자 API가 401/403 → AuthContext가 토큰을 비우고 `/login`으로 튕김(무한 차단).
- 원인 분석 결과, 롤백으로 dev-bypass의 **FE 헤더 주입**과 **BE 역할 주입**이 빠지면서 인증 고리가 끊겼다.
  - FE `client.ts`: `IS_DEV`일 때 `X-Dev-User-Id` / `X-Dev-Roles` 헤더를 붙이던 블록 제거됨. 가짜 토큰이 그대로 `Bearer`로 나가 인증주체 미설정.
  - FE `env.ts`: `IS_DEV`, `DEV_ADMIN_MEMBER_ID` export 제거됨.
  - BE `DevUserIdHeaderFilter`: `X-Dev-Roles` 처리(`resolveAuthorities`)가 제거되고 `ROLE_USER` 하드코딩으로 회귀 → 관리자 API의 ROLE_ADMIN 1차 검사 통과 불가.
- 화면 측(`LoginPage`, `AuthContext`, `ProtectedRoute`, `kakao.ts`)은 롤백 영향 없음(현행 유지). 즉 dev 로그인 진입은 `LoginPage`의 `import.meta.env.DEV` 전용 "개발용 토큰" 입력란 + `client.ts` 헤더 주입 조합으로 동작하는 구조.

## 복원 근거(원본 출처)
- 정합성 있는 원본: 커밋 `924654ef`(=`aa3c2341`) "feat(security,admin): dev 관리자 로그인 경로 — ROLE_ADMIN 주입 + admin_users 시드 (dev 전용) (#345)".
- 최신 정상본 참고: 백업 브랜치 `backup/dev-before-rollback-504-20260611`(tip `5c6728d`, #509) — `client.ts` 헤더 블록과 `DevUserIdHeaderFilter.resolveAuthorities` 포함.
- 위 두 출처에서 가져와 현행 파일에 **최소 diff**로 재이식(무관한 리팩터링 없음).

## 변경 파일 (3개, dev 전용)
1. `admin-web/src/config/env.ts`
   - `IS_DEV = import.meta.env.DEV`
   - `DEV_ADMIN_MEMBER_ID = import.meta.env.VITE_DEV_ADMIN_MEMBER_ID ?? '1'`
   - 기존 `API_BASE_URL`, `KAKAO_JS_KEY`는 유지(병합).
2. `admin-web/src/api/client.ts`
   - import에 `IS_DEV, DEV_ADMIN_MEMBER_ID` 추가.
   - 요청 인터셉터: `if (token && !IS_DEV)`로 dev에선 가짜 토큰을 Bearer로 보내지 않음.
   - `if (IS_DEV)`일 때 `headers.set('X-Dev-User-Id', DEV_ADMIN_MEMBER_ID)`, `headers.set('X-Dev-Roles', 'ADMIN')`.
3. `qtai-server/admin-server/src/main/java/com/qtai/security/DevUserIdHeaderFilter.java`
   - `ROLES_HEADER = "X-Dev-Roles"` 상수 추가, import `ArrayList`, `Locale` 추가.
   - 인증 토큰 권한을 `resolveAuthorities(request)`로 생성: 기본 `ROLE_USER` + `X-Dev-Roles`(쉼표구분)를 `ROLE_*`로 승격. `ADMIN` → `ROLE_ADMIN`.

## 안전 가드(운영 영향 없음 확인)
- BE 활성 조건: `@Profile("dev")` + `@ConditionalOnProperty(qtai.security.dev-bypass=true)`.
  - `application-dev.yml` → `dev-bypass: true`(현행), `application-prod.yml` → `dev-bypass: false`(현행).
  - `DevSecurityConfig`는 active profile에 `prod` 포함 시 부트 실패(3중 가드) — 변경 없음.
- FE: `IS_DEV`(=vite `import.meta.env.DEV`)는 prod 빌드에서 false → X-Dev 헤더 미첨부, `LoginPage` 개발용 입력란 미노출.
- 헤더 짝 검증: FE 전송 `X-Dev-User-Id`/`X-Dev-Roles:ADMIN` ↔ BE 수신 동일 헤더명/`ROLE_ADMIN` 매핑 일치.
- import 짝 검증: `client.ts`가 `env.ts`의 `IS_DEV`,`DEV_ADMIN_MEMBER_ID`를 정확히 참조.
- 회원 시드: `DevMemberSeedRunner`(dev+bypass=true)로 dev 회원 시드 존재(기존). `DEV_ADMIN_MEMBER_ID` 기본 `'1'`.

## 검토 (요청대로 2~3회)
1. 짝/문법: TS import·헤더명·Java `resolveAuthorities` 반환형(`List<SimpleGrantedAuthority>`) ↔ `UsernamePasswordAuthenticationToken` 생성자 호환 확인.
2. 진입 흐름: `LoginPage`(dev) 개발용 토큰 입력 → `login()` 토큰 저장 → `AuthContext`가 `getAdminMe()` 호출 → `client.ts`가 X-Dev 헤더 첨부 → BE permitAll+필터로 200.
3. prod 안전: 위 가드들로 prod 경로에 어떤 헤더/입력란/필터도 노출되지 않음 재확인.

## 대안: dev-bypass를 끈 채 admin-server 기동
재이식을 원치 않을 때(정식 JWT 경로로 검증) 사용. 파일 수정 없이 실행 시 오버라이드 권장.
- bootRun 인자: `./gradlew -p qtai-server :admin-server:bootRun --args='--spring.profiles.active=dev --qtai.security.dev-bypass=false'`
- 또는 환경변수: `QTAI_SECURITY_DEV_BYPASS=false` 로 기동.
- 효과: `SecurityConfig`(정식 RS256 JWT 체인) 활성, `DevSecurityConfig`/`DevUserIdHeaderFilter`/`DevMemberSeedRunner` 비활성.
- 단, 이 경우 admin-web 로그인엔 **실제 ADMIN JWT**가 필요 → `VITE_KAKAO_JS_KEY` 주입 후 카카오 로그인 또는 개발용 토큰란에 진짜 RS256 ADMIN access 토큰을 붙여야 함. 카카오 키/토큰이 없으면 로컬 로그인은 여전히 불가.

## Git/PR (미수행 — Lead 확인 후 진행)
- 작업 트리만 수정. `dev`/`master` 직접 push 금지(규칙). 브랜치 예: `fix/admin-web-dev-bypass-restore` → PR 대상 `dev`.
- 팀 MSA 작업 중 PR 보류 관례 고려 — 머지 타이밍은 Lead 판단.
- 관련: 롤백 출처 `revert/pr459-admin-web`, 복원 참고 `revert/pr460-restore-admin-web`, 백업 `backup/dev-before-rollback-504-20260611`.
