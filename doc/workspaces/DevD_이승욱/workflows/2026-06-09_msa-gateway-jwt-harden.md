# 2026-06-09 게이트웨이 JWT 인증 필터 — 리뷰 WARN 보완(harden)

## 목표
게이트웨이 JWT 인증 필터 PR(#364, dev 머지 완료)의 리뷰 APPROVE 후 WARN(권장 수준) 항목을 후속 보완한다. 회귀 안전망·관측성·운영 친화성을 높이는 소규모 하드닝.

## 배경
- #364가 dev에 머지된 뒤 리뷰에서 5건 WARN(비차단). 그중 4건은 소규모라 별도 브랜치로 보완, 1건(refresh 블랙리스트)은 인증 서비스 설계가 얽혀 별도 트랙으로 분리.

## 작업 내용
1. **빈 토큰 사전 분기** — `Bearer ` 뒤 토큰이 빈 문자열이면 검증기 호출 전에 401로 분기(명시적 방어·불필요한 파싱 회피). 기존에도 빈 토큰은 검증기 예외로 401이었으나 경로를 명확화.
2. **검증 실패 로깅** — slf4j `log.debug`로 인증 실패 사유 기록(헤더 누락·빈 토큰·검증 예외 메시지 + path). **token 값은 남기지 않는다**(CLAUDE.md §9). 401은 미인증 프로브로 흔해 `debug` 레벨로 둬 로그 스팸 방지.
3. **환경변수 누락 가드** — `gateway.jwt.public-key`를 `${JWT_PUBLIC_KEY:}`(빈 기본값)로 두고, `GatewayJwtConfig`에서 blank면 **명확한 메시지**로 `IllegalStateException` 던짐(미설정 시 Spring placeholder 에러 대신 "JWT_PUBLIC_KEY를 주입하세요" 안내, fail-fast).
4. **claim 누락 테스트** — 필터 테스트에 유효 서명이나 `role`/`subject(memberId)` claim이 누락된 토큰 → 401 케이스 + 빈 Bearer 토큰 케이스 추가(10→13건).

(보류) **refresh 블랙리스트 다운스트림 확인** — 게이트웨이는 이미 refresh 타입 토큰의 인증 사용을 거부. 로그아웃/폐기(revocation) 블랙리스트는 인증 서비스·다운스트림 설계 주제라 별도 작업으로 분리.

## 범위
- 브랜치: `feature/msa-gateway-jwt-harden` (base: `dev`, #364 머지 후 dev에서 분기)
- 변경: `JwtAuthenticationFilter`(로깅·빈토큰 분기) / `GatewayJwtConfig`(가드) / application.yml(빈 기본값) / 필터 테스트(+3). 코어·다른 서비스 무변경.
- 관련: #364 리뷰 후속

## 검증
- `gradlew :service-gateway:test` — **BUILD SUCCESSFUL / 0 failures (16건)**
  - `JwtAuthenticationFilterTest` **13**(기존 10 + role누락/subject누락/빈Bearer 401)
  - `GatewayApplicationTests`/`FallbackControllerTest`/`GatewayRouteTest` 각 1 — 환경변수 가드(빈 기본값) 변경 후에도 컨텍스트 로드·라우트 유지(테스트 키 런타임 주입)
- 전체 `./gradlew test`(Docker/Redis)는 CI.

## 미해결 / 후속
- PR 머지 대기.
- 후속(별도 트랙): refresh 토큰 블랙리스트/폐기 검증 — 인증 서비스 + 다운스트림. 다운스트림에서 게이트웨이 주입 헤더 신뢰·소비. 이어서 bible 추출(Phase 1).

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
