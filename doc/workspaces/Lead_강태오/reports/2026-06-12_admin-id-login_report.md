# 리포트 — 2026-06-12 관리자 웹 자체 아이디/비밀번호 로그인 (카카오 제거)

## 한 줄 요약
관리자 웹 로그인을 카카오 → 자체 아이디/비밀번호(BCrypt)로 전환. admin-server(8090)가 직접 발급하며, dev는 `admin / admin1234`로 로그인. 백엔드 전체 테스트·프런트 타입체크 통과.

## 결과
- ✅ 백엔드: `./gradlew :admin-server:test` BUILD SUCCESSFUL — 신규 인증 테스트 7케이스 + 보안 통합 + ArchUnit/Modulith + V36(H2) 통과.
- ✅ 프런트: `npm run typecheck` 통과, 카카오/dev-bypass 잔여 참조 0건.
- ✅ 도메인 경계 준수(admin→member는 api UseCase만).

## 엔드포인트 / 계약
- `POST /api/v1/admin/auth/login` — body `{ username, password }` → `{ accessToken, refreshToken, admin{ memberId, nickname, role, adminRole, status } }`.
- `POST /api/v1/admin/auth/refresh` — body `{ refreshToken }` → 동일 형태.
- 실패: 자격 불일치 401(`ADMIN_LOGIN_FAILED`), 비활성 403(`ADMIN_USER_DISABLED`).

## 변경 범위
- BE(admin-server): 마이그레이션 1, 엔티티/리포지토리, UseCase/Service/Controller/DTO, PasswordEncoder, SecurityConfig permitAll, ErrorCode, dev-seed, application-dev.yml(dev-bypass=false), 테스트 1.
- FE(admin-web): adminAuth/LoginPage/client/env/vite.config/index.html/.env.example 수정, kakao.ts 삭제.
- 사용자 앱 카카오 로그인은 변경 없음.

## 로컬 사용
1. `docker compose up -d mysql redis`
2. admin-server를 dev로 기동(`SERVER_PORT=8090`, `SPRING_PROFILES_ACTIVE=dev`) — Flyway가 admin/admin1234 시드.
3. admin-web `npm run dev` → `http://localhost:5173` → **admin / admin1234** 로그인.

## 후속
- 운영 관리자 계정 발급/비밀번호 정책(실패 잠금·초기 비번 변경) — 후속 PR.
- `DevSecurityConfig`/`DevUserIdHeaderFilter` 삭제 정리 — 후속(현재 dev-bypass=false로 비활성).
- 문서 레포(02 ERD / 04 API / CLAUDE.md OAuth) 반영 — 별도 PR.
- 상세: `workflows/2026-06-12_admin-id-login.md`.
