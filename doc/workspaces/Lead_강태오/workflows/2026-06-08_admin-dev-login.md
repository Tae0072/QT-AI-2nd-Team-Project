# [Workflow] admin-web dev 관리자 로그인 (2026-06-08)

## 목적

dev 환경(dev-bypass)에서 admin-web이 실제 관리자 API(`/api/v1/admin/**`)를 호출할 수 있도록 관리자 인증 경로를 마련한다. (작업 일정 B단계)

## 문제

관리자 API는 ① SecurityContext의 `ROLE_ADMIN` 권한 + ② `admin_users` 활성 행(`AdminService` 2차 검증)이 **모두** 필요한데, 현행 dev는 `DevUserIdHeaderFilter`가 `ROLE_USER`만 부여하고 `admin_users` 시드가 없어 관리자 화면에서 403.

## 변경 (전부 `@Profile("dev")` + dev-bypass 토글 한정 — prod 무영향)

1. `DevUserIdHeaderFilter` — `X-Dev-Roles` 헤더 지원(`ADMIN`→`ROLE_ADMIN`). 헤더 없으면 기존대로 `ROLE_USER`.
2. `DevAdminSeedRunner`(admin.internal, 신규) — `qtai.dev.admin-member-id`(기본 1)를 `SUPER_ADMIN` 활성 관리자로 멱등 시드. FK/경합 예외 흡수.
3. `DevMemberSeedRunner` `@Order(10)` / `DevAdminSeedRunner` `@Order(20)` — 회원→관리자 시드 순서 보장(`admin_users.member_id` FK).
4. admin-web — dev에서 `X-Dev-User-Id` + `X-Dev-Roles:ADMIN` 자동 첨부(`client.ts`), 토큰 없이 "DEV 관리자로 로그인" 버튼(`LoginPage`), `config/env.ts`·`.env.example`.

## 경계/안전

- `DevAdminSeedRunner`는 admin 도메인 내부(`AdminUserRepository`)만 사용 — 도메인 간 import 없음(ArchUnit 통과).
- prod 무영향(`@Profile("dev")` + `@ConditionalOnProperty(dev-bypass)`).

## 검증

- qtai-server: `compileJava`/`compileTestJava` + `DevUserIdHeaderFilterTest`·`DevAdminSeedRunnerTest`·`AdminServiceTest`·`AdminUserTest`·`DomainBoundaryArchTest`·`DevMemberSeedRunnerTest` → BUILD SUCCESSFUL.
- admin-web: `tsc --noEmit`(typecheck) 통과.
- 런타임 수동 확인(후속): dev 서버 기동 후 admin-web "DEV 관리자로 로그인" → `/api/v1/admin/me` 200.

## 전제/주의

- dev 회원 = id 1 관례. 다르면 `qtai.dev.admin-member-id` + `VITE_DEV_ADMIN_MEMBER_ID`를 동일 값으로 설정.
- `members.role`은 현행 코드 인가 경로에서 미검증(`admin_users` + authority 기준)이라 별도 변경하지 않음.
- 정식 카카오 웹 로그인(F단계)은 별도.
