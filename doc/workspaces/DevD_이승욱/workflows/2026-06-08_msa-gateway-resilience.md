# 2026-06-08 MSA — 게이트웨이 하드닝 (타임아웃·Circuit Breaker + 라우트 회귀 테스트)

## 목표
`service-gateway`(#350) 스캐폴드의 리뷰 후속 — 라우트 타임아웃/Circuit Breaker로 다운스트림 장애를 격리하고, 라우트 적재 회귀 안전망을 추가한다.

## 배경
- #350 게이트웨이 스캐폴드 리뷰(BLOCK 아님, 후속 권장): **(a)** 라우트 타임아웃/Circuit Breaker 부재, **(b)** `GatewayApplicationTests`가 `contextLoads`만 수행해 라우트 적재 회귀 안전망이 얕음.

## 작업 내용
1. **(a) 장애 격리**: `httpclient.connect-timeout 2000ms` + `response-timeout 5s` + 라우트 `CircuitBreaker` 필터(Resilience4j, `monolithCb`) + `/__fallback` 폴백(503, 표준 에러 envelope).
2. **(b) 라우트 회귀 안전망**: `GatewayRouteTest` — `RouteDefinitionLocator`로 `monolith` 라우트의 **Path 예측자 + CircuitBreaker 필터 정의**를 실제로 단언.

### 리뷰 후속 반영(2차, 머지 전)
3. **(a) 폴백 응답 테스트**: `GatewayFallbackControllerTest` — WebTestClient로 `/__fallback` → 503 + envelope(code·timestamp·traceId) 검증(변경된 public 동작 회귀 보호).
4. **(b) 필터 실검증**: 라우트 테스트를 id-only → `RouteDefinitionLocator` 기반 필터/예측자 정의 단언으로 보강.
5. **(c) envelope 보강**: 폴백에 `data(null)/timestamp(Asia/Seoul)/traceId` 추가 → ApiResponse 동일 스키마.

## 범위
- 브랜치: `feature/msa-gateway-resilience` (base: `dev`)
- 변경: `service-gateway` build/yml + 폴백 컨트롤러 1 + 테스트 3(contextLoads·라우트 정의·폴백 응답)

## 검증
- `:service-gateway:test` — **BUILD SUCCESSFUL** (3개 테스트 클래스: contextLoads + 라우트 필터 정의 단언 + 폴백 503 응답)

## 미해결 / 후속
- PR 머지 대기
- 후속: lib-common web-free 분리 → 게이트웨이 JWT 인증 필터(JwtTokenVerifier) · bible 추출(서비스별 라우트 분기)

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
