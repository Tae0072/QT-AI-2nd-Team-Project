# 2026-06-09 bible-service 라우트 컷오버(Inc2b) — 결과 보고

## 요약
게이트웨이가 `/api/v1/bible/**`를 bible-service로 라우팅하도록 전환했다(실제 컷오버). 우회 2차 방어선 `X-Gateway-Token` 주입 + 라우트 순서/필터 회귀 테스트 + 토큰 통합 회귀 테스트(Inc2a 리뷰 후속) 포함. 도메인/코어 무변경.

## 산출물

| 파일 | 설명 |
|------|------|
| `service-gateway/.../application.yml` | bible 라우트(catch-all 이전) + CircuitBreaker(`bibleCb`) + `AddRequestHeader=X-Gateway-Token`(sentinel 기본값) |
| `service-gateway/.../GatewayRouteTest.java` | (+2) bible 라우트 필터 적재 + monolith보다 앞 순서 단언 |
| `bible-service/.../BibleGatewayTokenSliceTest.java` | (신규) 토큰 설정 시 일치 200 / 불일치·누락 401 통합 회귀 |

## 변경 성격
- **트래픽 컷오버**: bible 라우트를 monolith catch-all보다 **앞 순서**로 추가 → `/api/v1/bible/**`만 bible-service로, 나머지는 모놀리식 유지(Strangler).
- **2차 방어선 주입**: 게이트웨이가 `X-Gateway-Token`을 주입(env 동기값). 빈 값 바인딩 불가 제약 때문에 기본값은 sentinel(`unset`) — 서비스 토큰 미설정 시 무시, 양측 설정 시 활성.
- **회귀 안전망**: ① 라우트 순서(bible < monolith)를 테스트로 고정(순서 뒤집힘 = bible 트래픽 유실 방지) ② 토큰 불일치/누락 → 401을 컨트롤러 통합 경로에서 단언(리뷰 후속).
- 장애 격리: CircuitBreaker 폴백(503 표준 envelope).

## 검증
- `gradlew :service-gateway:test` — **0 failures (18건)**: GatewayRouteTest 3 + JwtAuthenticationFilterTest 13 + contextLoads/폴백 2
- `gradlew :bible-service:test` — **0 failures (15건)**: BibleGatewayTokenSliceTest 3 + 슬라이스 2 + 필터 7 + persistence 1 + 캐시 1 + contextLoads 1

## 미해결
- 배포 활성화 설정(양측 토큰 동기화 점검 필수) — workflow에 체크리스트.
- Inc3(소비자 어댑터) → Inc4(DB 분리) → Inc5(모놀리식 제거)
