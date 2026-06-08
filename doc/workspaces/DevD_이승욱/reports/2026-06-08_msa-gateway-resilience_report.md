# 2026-06-08 MSA 게이트웨이 하드닝 — 결과 보고

## 요약
`service-gateway`(#350)의 리뷰 후속으로 ① 라우트 타임아웃 + Resilience4j Circuit Breaker + 폴백, ② 라우트 적재 회귀 테스트를 추가했다. 본 PR은 코드 4파일 + 문서.

## 산출물

| 파일 | 설명 |
|------|------|
| `service-gateway/build.gradle.kts` | `spring-cloud-starter-circuitbreaker-reactor-resilience4j` 추가 |
| `service-gateway/.../application.yml` | httpclient connect/response 타임아웃 + 라우트 `CircuitBreaker` 필터(fallback `/__fallback`) |
| `service-gateway/.../GatewayFallbackController.java` | (신규) 503 폴백 — 모놀리식 표준 에러 envelope 동일 형태 |
| `service-gateway/.../GatewayRouteTest.java` | (신규) RouteLocator로 monolith 라우트(CB 필터 포함) 적재 검증 |

## 변경 성격
- **장애 격리(a)**: 다운스트림 연결 2s/응답 5s 타임아웃, 회로 개방 시 503 표준 envelope 폴백(빈 응답/행 대신 명시적 실패).
- **회귀 안전망(b)**: 라우트가 필터·예측자 포함 실제 빌드되는지 단언 → 설정 오타/필터 누락을 PR에서 차단.
- 게이트웨이는 여전히 lib-common 미의존(WebFlux/servlet 충돌 회피). 폴백 envelope는 Map으로 직접 구성.

## 검증
- `gradlew :service-gateway:test` — **BUILD SUCCESSFUL** (contextLoads + GatewayRouteTest, CircuitBreaker/Resilience4j 포함)
- 전체 `./gradlew test`는 CI

## 미해결
- PR 머지 대기
- 후속: lib-common web-free 분리 → 게이트웨이 JWT 인증 필터 · bible 추출(라우트 분기)
