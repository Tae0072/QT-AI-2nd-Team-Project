# 2026-06-12 관리자 웹 인증 — 카카오 → 자체 아이디/비밀번호 로그인

## 배경 / 결정
- 결정 제안서 `2026-06-11_decision-proposal_admin-id-login.md` 승인 → 구현.
- 방향(확정): ① 로그인 엔드포인트는 **admin-server(8090)**, ② dev 시드 계정 **admin / admin1234**(BCrypt), ③ 카카오는 **관리자 웹 한정 제거**(사용자 앱 `/api/v1/auth/kakao`는 유지), ④ dev-bypass(X-Dev 헤더) 제거하고 실토큰 로그인으로 일원화.
- 선행: dev-bypass 복원 PR #536 머지 후 최신 dev에서 `feature/admin-id-login` 분기.

## 변경 — 백엔드 (admin-server)
- 스키마: `V36__add_admin_username_password.sql` — `admin_users`에 `username`(UNIQUE), `password_hash` 추가(기존 행 호환 위해 NULL 허용).
- 엔티티/리포지토리: `AdminUser`에 `username`/`passwordHash` 필드 + `assignCredentials()`; `AdminUserRepository.findByUsername()`.
- 인증: `AdminAuthUseCase`(api) + `AdminAuthService`(internal) — `findByUsername` → **BCrypt 검증** → 활성 확인 → member 닉네임/상태 조회(`member.api.GetMemberUseCase`) → ADMIN RS256 발급. 미존재/비밀번호 불일치는 동일 `ADMIN_LOGIN_FAILED`(401)로 응답해 계정 enumeration 차단.
- 발급기: admin-server `JwtProvider`에 이미 `issueAccessToken/issueRefreshToken/validateRefreshToken` 존재 → 그대로 재사용(신규 발급기 불필요).
- 엔드포인트: `AdminAuthController` — `POST /api/v1/admin/auth/login`, `POST /api/v1/admin/auth/refresh`.
- 보안: `SecurityConfig`에 두 경로 `permitAll`(`/api/v1/admin/**` hasRole보다 먼저). `PasswordEncoderConfig`(BCrypt) 빈 신설.
- 에러코드: `ADMIN_LOGIN_FAILED("AD0004", 401)` 추가.
- 시드/프로파일: `R__seed_dev_admin_account.sql`에 `username='admin'` + BCrypt 해시(평문 admin1234) 추가. `application-dev.yml`의 `qtai.security.dev-bypass: false`로 전환(정식 JWT 체인 활성 → 실토큰 로그인 일원화).
- CORS: dev-bypass=false로 정식 SecurityConfig가 켜지면서 CORS 허용 오리진 기본값(`localhost:3000`)이 적용돼, admin-web(5173)에서 브라우저 요청이 `403 "Invalid CORS request"`로 거부됐다(서버→서버 curl은 Origin 헤더가 없어 통과 → 브라우저만 실패). `application-dev.yml`에 `cors.allowed-origins: http://localhost:5173,http://localhost:3000` 추가로 해소(브라우저 컨텍스트 fetch 200 확인).

## 변경 — 프런트 (admin-web)
- `adminAuth.ts`: `loginAdminWithKakao` → `loginAdminWithPassword(username, password)` (`POST /admin/auth/login`).
- `LoginPage.tsx`: 카카오 버튼·개발용 토큰 입력 제거 → **아이디/비밀번호 폼**.
- `kakao.ts` 삭제. `index.html` 카카오 JS SDK `<script>` 제거. `config/env.ts`에서 `KAKAO_JS_KEY`/`IS_DEV`/`DEV_ADMIN_MEMBER_ID` 제거.
- `client.ts`: dev-bypass(X-Dev 헤더/IS_DEV 분기) 제거 → 항상 Bearer 실토큰. refresh 경로 `/auth/refresh` → `/admin/auth/refresh`.
- `vite.config.ts`: 인증 프록시(8081) 분리 제거 → 모든 `/api` → admin-server(8090). `.env.example`에서 카카오/AUTH_PROXY 제거.
- 사용자 앱(flutter) 카카오 로그인은 변경 없음.

## 보안 고려
- 비밀번호는 BCrypt 해시만 저장(평문·해시·토큰 로그 금지, CLAUDE.md §8/§9). 로그인 실패는 username 존재 여부 노출 없이 401 단일 처리.
- dev 시드 해시는 dev-seed(db/dev-seed, local·dev 프로파일 한정)에만 존재 → 운영 미적용. 운영 계정/비번 발급은 후속.

## 검증 (요청대로 2~3회)
1. **백엔드 빌드/테스트**: `./gradlew :admin-server:test` → **BUILD SUCCESSFUL (1m35s)**. 신규 `AdminAuthServiceTest`(로그인 성공/미존재/오비번/해시없음/비활성/refresh 성공/무효 refresh 7케이스) + 기존 `AdminServerSecurityTest`(@SpringBootTest, V36 H2 적용) + ArchUnit/Spring Modulith 경계 모두 통과.
2. **도메인 경계**: admin→member 호출은 `member.api.GetMemberUseCase`(api 레벨)만 사용 → 경계 위반 없음(Modulith/ArchUnit 통과로 확인).
3. **프런트 타입체크**: `npm run typecheck` → 통과(카카오/dev-bypass 잔여 참조 0건 grep 확인).

## 후속 / 메모
- dev-bypass의 BE 잔재(`DevSecurityConfig`, `DevUserIdHeaderFilter`)는 `dev-bypass=false`로 **비활성**만 함(삭제는 후속 정리 PR). 운영 무영향.
- 운영 관리자 계정 발급 화면/비밀번호 정책(실패 잠금·초기 비번 강제변경)은 후속 항목.
- 문서 레포 반영 필요: `02_ERD`(admin_users 컬럼), `04_API`(/admin/auth/login·refresh), `CLAUDE.md` OAuth 경로 — 별도 문서 레포 PR.

## Git/PR
- 브랜치 `feature/admin-id-login`(규칙상 `feat/`는 금지 → `feature/`) → PR 대상 `dev`. 기능 PR이라 파일 수 많음(본문에 사유 명시). 관련: `reports/2026-06-12_admin-id-login_report.md`.
