# 2026-06-12 관리자 로그인 보안 강화 (시도 제한 + refresh 회전/무효화)

## 배경
- 아이디/비밀번호 로그인 도입(PR #539) 자동리뷰 WARN 대응: ① 로그인 브루트포스 방어 부재, ② refresh 토큰 회전·무효화 부재(탈취 시 만료까지 유효). Redis 기반 풀 구현.

## 변경 (admin-server, admin 도메인 소유)
- `AdminLoginAttemptGuard`(Redis): username 단위 실패 카운터. `5`회 연속 실패 시 `15분` 잠금(`assertNotLocked`/`recordFailure`/`reset`). 잠금 시 `429 ADMIN_LOGIN_RATE_LIMITED`.
- `AdminRefreshTokenStore`(Redis): memberId 단위 **현재 유효 refresh token 1개**만 보관. 갱신 시 새 토큰 저장→기존 자동 무효화(회전). 제시 토큰이 저장본과 불일치(옛/재사용)면 거부.
- `AdminAuthService`:
  - login: `assertNotLocked` 선검사 → 실패 시 `recordFailure` → 성공 시 `reset` + (issueFor에서) refresh 저장. 미존재·오비번을 단일 분기로 합쳐 enumeration 차단 강화.
  - refresh: `validateRefreshToken` → `refreshTokenStore.matches` 검증(회전/재사용 거부) → 새 토큰 발급·저장.
- `ErrorCode.ADMIN_LOGIN_RATE_LIMITED(AD0005, 429)` 추가.
- 도메인 경계: 기존 `RefreshTokenStore`는 member.internal이라 직접 사용 불가 → admin 소유로 동등 구현. `StringRedisTemplate`(공통 인프라)만 사용, 타 도메인 import 없음.

## 검증
- `./gradlew :admin-server:test` **BUILD SUCCESSFUL**. `AdminAuthServiceTest` 9케이스(성공/잠금429/미존재/오비번/해시null/비활성/refresh성공·회전/재사용거부/무효토큰) + `AdminAuthControllerTest` + ArchUnit/Modulith 통과. 신규 Redis 빈 컨텍스트 로드 확인.

## Git/PR
- 브랜치 `feature/admin-login-hardening` → PR 대상 `dev`. 관련: #539, #549, #546, 문서레포 #20.
- 후속(③): 운영 관리자 계정 발급·비밀번호 정책 — 설계 제안서로 진행 예정.
