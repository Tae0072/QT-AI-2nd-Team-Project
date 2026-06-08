# 2026-06-08 MSA 게이트웨이 하드닝 — 결과 보고

## 요약
`service-gateway`(#350)의 리뷰 후속으로 ① 라우트 타임아웃 + Resilience4j Circuit Breaker + 폴백, ② 라우트 적재 회귀 테스트를 추가했다. 본 PR은 코드 4파일 + 문서.

## 산출물

| 파일 | 설명 |
|------|------|
| `service-gateway/build.gradle.kts` | `spring-cloud-starter-circuitbreaker-reactor-resilience4j` 추가 |
| `service-gateway/.../application.yml` | httpclient connect/response 타임아웃 + 라우트 `CircuitBreaker` 필터(fallback `/__fallback`) |
| `service-gateway/.../GatewayFallbackController.java` | (신규) 503 폴백 — 표준 에러 envelope(`success/data/error/timestamp/traceId`) 동일 형태 |
| `service-gateway/.../GatewayRouteTest.java` | (신규) RouteDefinitionLocator로 monolith 라우트의 **Path 예측자 + CircuitBreaker 필터** 실제 단언 |
| `service-gateway/.../GatewayFallbackControllerTest.java` | (신규) WebTestClient로 `/__fallback` 503 + envelope 회귀 검증 |

## 변경 성격
- **장애 격리(a)**: 다운스트림 연결 2s/응답 5s 타임아웃, 회로 개방 시 503 표준 envelope 폴백(빈 응답/행 대신 명시적 실패).
- **회귀 안전망(b)**: 라우트 정의가 필터·예측자 포함 실제 빌드되는지 단언 → 설정 오타/필터 누락을 PR에서 차단.
- 게이트웨이는 여전히 lib-common 미의존(WebFlux/servlet 충돌 회피). 폴백 envelope는 Map으로 직접 구성.

### 리뷰 후속 반영(머지 전 보완)
- **(a) 폴백 응답 테스트 추가**: `GatewayFallbackControllerTest`가 WebTestClient로 `/__fallback` 503 + envelope(code·timestamp·traceId)을 검증 — 변경된 public 동작 회귀 보호.
- **(b) 필터 실검증**: 라우트 테스트를 `RouteDefinitionLocator` 기반으로 바꿔 **CircuitBreaker 필터·Path 예측자 정의**를 실제로 단언(기존 id-only 단언 보강).
- **(c) envelope 보강**: 폴백에 `data(null)/timestamp(Asia/Seoul)/traceId` 추가 → ApiResponse와 동일 스키마.

## 검증
- `gradlew :service-gateway:test` — **BUILD SUCCESSFUL** (contextLoads + 라우트 필터 단언 + 폴백 503 응답, 3개 테스트 클래스)
- 전체 `./gradlew test`는 CI

## 미해결
- PR 머지 대기
- 후속: lib-common web-free 분리 → 게이트웨이 JWT 인증 필터 · bible 추출(라우트 분기)
