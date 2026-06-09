# 2026-06-09 bible HTTP 클라이언트 Circuit Breaker(Inc3d) — 결과 보고

## 요약
Lead 승인 후 bible HTTP 클라이언트에 Resilience4j Circuit Breaker를 도입했다. 지속 장애 시 fast-fail로 다운스트림 격리. 재시도(일시 blip)와 CB(지속 장애)의 2단 회복탄력성. 기본 inprocess라 동작 무변경.

## 산출물

| 파일 | 설명 |
|------|------|
| `build.gradle.kts` | `resilience4j-circuitbreaker:2.2.0` 추가 |
| `external/bible/BibleHttpClientConfiguration.java` | CircuitBreaker 빈(설정값·4xx 미기록·상태전이 로그) + 클라이언트 주입 |
| `external/bible/BibleServiceClient.java` | `executeWithResilience` — CB로 재시도 래핑, OPEN 시 fast-fail |
| `external/bible/BibleClientProperties.java` | `cb-*` 설정(failure-rate/window/min-calls/wait) |
| `src/main/resources/application.yml` | CB 토글 |
| `external/bible/BibleServiceClientTest.java` | CB OPEN·단락 / 4xx CLOSED 유지 |

## 변경 성격
- **장애 격리(CB)**: 지속 일시장애(재시도 소진 = EXTERNAL_API_FAILURE)가 임계 초과 시 OPEN → 다운스트림 호출 없이 fast-fail(`BusinessException`). **4xx 역매핑은 CB 실패로 기록 안 함**(클라이언트 결정적 오류는 격리 대상 아님).
- **2단 회복탄력성**: 재시도(일시 blip 흡수) + CB(지속 장애 차단). 게이트웨이와 동일 Resilience4j.
- **관측성**: CB 상태 전이 INFO 로그.
- 기본 inprocess라 동작 무변경(소비자·bible·코어 무변경). resilience4j는 게이트웨이가 이미 사용(금지 아님).

## 검증
- `gradlew :compileJava :test --tests "com.qtai.external.bible.*"` — **0 failures (16건)**: 클라이언트 10(+CB 2) + Configuration 3 + 어댑터 3

## 미해결
- Inc3d 컷오버(승인): mode=http + 토큰 동기화 + 소비자 계약 테스트 → Inc4(DB 분리) → Inc5(모놀리식 제거).
