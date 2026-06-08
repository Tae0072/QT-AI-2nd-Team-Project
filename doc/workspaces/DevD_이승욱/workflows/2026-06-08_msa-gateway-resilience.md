# 2026-06-08 MSA — 게이트웨이 하드닝 (타임아웃·Circuit Breaker + 라우트 회귀 테스트)

## 목표
`service-gateway`(#350) 스캐폴드의 리뷰 후속 — 라우트 타임아웃/Circuit Breaker로 다운스트림 장애를 격리하고, 라우트 적재 회귀 안전망을 추가한다.

## 배경
- #350 게이트웨이 스캐폴드 리뷰(BLOCK 아님, 후속 권장): **(a)** 라우트 타임아웃/Circuit Breaker 부재, **(b)** `GatewayApplicationTests`가 `contextLoads`만 수행해 라우트 적재 회귀 안전망이 얕음.

## 작업 내용
1. **(a) 장애 격리**: `httpclient.connect-timeout 2000ms` + `response-timeout 5s` + 라우트 `CircuitBreaker` 필터(Resilience4j, `monolithCb`) + `/__fallback` 폴백(503, 모놀리식 표준 에러 envelope `success/error{code,message}` 동일 형태).
2. **(b) 라우트 회귀 안전망**: `GatewayRouteTest` — `RouteLocator`로 `monolith` 라우트(CircuitBreaker 필터 포함)가 실제 빌드·적재되는지 검증(`contextLoads`보다 깊음).

## 범위
- 브랜치: `feature/msa-gateway-resilience` (base: `dev`)
- 변경: `service-gateway` build/yml + 폴백 컨트롤러 1 + 라우트 테스트 1

## 검증
- `:service-gateway:test` — **BUILD SUCCESSFUL** (contextLoads + 라우트 적재 회귀, CircuitBreaker 필터·Resilience4j 포함 빌드 성공)

## 미해결 / 후속
- PR 머지 대기
- 후속: lib-common web-free 분리 → 게이트웨이 JWT 인증 필터(JwtTokenVerifier) · bible 추출(서비스별 라우트 분기)

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
