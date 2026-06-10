# 2026-06-10 결정 — 서비스 간 시스템 인증(공유 HS256 단명 SYSTEM_BATCH 토큰)

## 배경 (왜 필요한가)
MSA 전환 후 서비스 간 호출은 RestClient(동기)로 한다. 사용자 요청에서 비롯된 호출은 사용자 JWT를 전달(`ServiceCallAuthForwarder`)하면 되지만, **배치·스케줄러(SYSTEM_BATCH) 호출**(예: ai 해설 생성 워커가 bible 본문을 읽는 호출)은 **전달할 사용자 JWT가 없다**. 그렇다고 인증을 비우면 수신 서비스의 보호 경로를 통과할 수 없다.

## 결정 (2026-06-10 Lead)
- 사용자 인증과 **분리된** "서비스 간 시스템 인증" 메커니즘을 둔다.
- 사용자 토큰: service-user가 발급하는 **RS256**(개인키 발급 / 공개키 검증). **변경 없음**.
- 시스템 토큰: **공유 시크릿 기반 HS256 단명 토큰**.
  - 키: `security.jwt.system-secret` (env로만 주입, 로그·커밋 금지).
  - claim: `sub=0`, `role=SYSTEM_BATCH`, `type=system`, 단명 만료(기본 60초).
  - 발급 `SystemTokenProvider`, 검증 `SystemTokenValidator` (둘 다 lib-common, `@ConditionalOnProperty(security.jwt.system-secret)`).
- 공통 `JwtAuthenticationFilter`: `Authorization: Bearer` 토큰을 먼저 RS256(사용자)로 검증하고, **실패 시 시스템 토큰(HS256)으로 폴백** 검증한다. 시스템 토큰이 유효하면 `memberId=0` + `ROLE_SYSTEM_BATCH`로 인증을 설정한다. **둘 다 실패해야 401**. RS256 사용자 경로 동작은 그대로 유지.

## 왜 RS256이 아니라 HS256(공유 시크릿)인가 (입문자용)
- RS256은 "발급자만 개인키를 갖고, 검증자는 공개키만"이라 **사용자 토큰**처럼 발급처가 하나(service-user)일 때 적합하다.
- 시스템 토큰은 **여러 서비스가 서로 발급·검증**해야 하므로, 모두가 같은 비밀(공유 시크릿)을 갖고 **HS256(대칭)**으로 발급·검증하는 게 단순하고 운영이 쉽다. 단명(60초)으로 탈취 위험을 낮춘다.
- 사용자 토큰(RS256)과 시스템 토큰(HS256)을 **분리**해, 시스템 시크릿이 새도 사용자 토큰 위조로 이어지지 않게 한다.

## 범위
- 본 PR: **메커니즘(Provider/Validator/필터 폴백) + 테스트 + SSoT(CLAUDE.md §5) + 본 결정 문서**.
- 후속 PR: 첫 소비자 — ai 배치가 bible를 호출하는 RestClient 어댑터에서 `SystemTokenProvider`로 토큰을 발급해 헤더에 실어 보냄. 서비스별 `SECURITY_JWT_SYSTEM_SECRET` env 주입(설정).

## SSoT·거버넌스
- 코드보다 먼저 CLAUDE.md §5에 메커니즘을 명문화(가드/리뷰가 SSoT 미갱신을 막으므로).
- 기존 금지(§8 "plain secret/token/private key 예시")는 유지 — 시크릿은 env만, 저장소·로그 금지.
- 공통 필터는 전 서비스가 공유하므로 lib-common/user/bible/note/ai/admin-server 전 모듈 회귀 빌드로 기존 JWT 동작 무손상 확인.
