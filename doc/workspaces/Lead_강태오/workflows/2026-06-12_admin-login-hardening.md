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

## 보안 트레이드오프 / 후속
- **refresh 재사용 탐지 대응**: 제시된 refresh가 저장본과 불일치(옛/재사용/탈취 의심)면 저장된 현재 토큰까지 삭제해 활성 세션을 강제 종료한다(재로그인 필요). 탈취 토큰의 피해 창을 줄인다.
- **계정 단위 잠금 DoS 트레이드오프**: username 단위 잠금은 공격자가 특정 관리자 아이디를 의도적으로 잠가 DoS를 유발할 수 있다. 운영에서 임계치(현재 5회/15분)·필요 시 IP 병행/관리자 잠금 해제 수단을 검토한다(후속). 운영 관리자 계정 발급·정책은 제안서 `2026-06-12_decision-proposal_admin-account-provisioning.md` 참고.

## 검증
- `./gradlew :admin-server:test` **BUILD SUCCESSFUL**. `AdminAuthServiceTest`(성공/잠금429/미존재/오비번/해시null/비활성/refresh성공·회전/재사용거부+현재토큰무효화/무효토큰) + `AdminLoginAttemptGuardTest`·`AdminRefreshTokenStoreTest`(컴포넌트 직접 단위) + `AdminAuthControllerTest` + ArchUnit/Modulith 통과.

## Git/PR
- 브랜치 `feature/admin-login-hardening` → PR 대상 `dev`. 관련: #539, #549, #546, 문서레포 #20.
- 후속(③): 운영 관리자 계정 발급·비밀번호 정책 — 설계 제안서로 진행 예정.
