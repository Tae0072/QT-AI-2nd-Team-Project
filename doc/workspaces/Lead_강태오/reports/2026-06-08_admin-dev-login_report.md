# [Report] admin-web dev 관리자 로그인 (2026-06-08)

## 요약

dev-bypass 환경에서 admin-web이 관리자 API를 호출할 수 있도록 ① dev 필터의 `ROLE_ADMIN` 주입과 ② `admin_users` `SUPER_ADMIN` 시드를 추가했다. 모두 dev 프로파일 한정, prod 무영향.

## 결과 (코드 9개 파일, +98/-3)

- 백엔드: `DevUserIdHeaderFilter`(X-Dev-Roles), `DevAdminSeedRunner`(신규)+테스트, `DevMemberSeedRunner`(@Order), `DevUserIdHeaderFilterTest`(테스트 추가).
- 프론트: `client.ts`(dev 헤더), `LoginPage`(dev 로그인 버튼), `config/env.ts`, `.env.example`.

## 검증

- gradle: 관련 단위 테스트 + ArchUnit 경계(`DomainBoundaryArchTest`) 통과 — BUILD SUCCESSFUL.
- admin-web: `typecheck` 통과.

## 한계 / 후속

- 런타임 E2E(서버 기동 후 `/api/v1/admin/me` 200)는 수동 확인 권장.
- dev 회원 id=1 관례 의존(설정으로 변경 가능).
- 정식 카카오 웹 로그인(F단계)은 별도 PR.
