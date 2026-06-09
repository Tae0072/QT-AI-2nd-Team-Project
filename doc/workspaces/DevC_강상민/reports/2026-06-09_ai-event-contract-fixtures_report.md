# ai-event-contract-fixtures Report

## 1. 작업 개요

`test/ai-event-contract-fixtures` 브랜치에서 AI event contract fixture와 검증 테스트를 추가했다.

이번 작업은 Kafka 구현 전 계약 고정 단계다. 메시징 의존성 추가, topic 생성, consumer/producer 구현, provider live 호출, gateway route enable, 운영 DB migration 적용, monolith AI 삭제는 수행하지 않았다.

## 2. 생성 파일

| 경로 | 내용 |
| --- | --- |
| `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-event-contract-fixtures.md` | 작업 범위와 검증 계획 |
| `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-event-contract-fixtures_report.md` | 수행 결과와 검증 결과 |
| `qtai-server/ai-service/src/test/resources/contracts/ai-events/ai-event-contract-fixtures.json` | AI event contract fixture catalog |
| `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/event/AiEventContractFixtureTest.java` | fixture 계약 검증 테스트 |

## 3. 반영 내용

- AI event 후보 9종 fixture를 추가했다.
- `schemaVersion`, `eventId`, `eventName`, `aggregateType`, `aggregateId`, `traceId`, `traceparent`, `occurredAt`, `payload` 공통 필드를 고정했다.
- event별 payload 필수 필드를 catalog로 고정했다.
- valid event payload에 prompt, raw response, 본문, 인증 값 계열 필드가 들어가지 않도록 테스트했다.
- `AiAssetPublishRequested`의 `idempotencyKey` 필수 조건을 검증했다.
- Provider AI input event 확정 전까지 QT/Bible 참조 조회는 HTTP client 계약을 유지한다는 결정을 fixture로 고정했다.

## 4. 제외한 작업

- Kafka 구현
- topic 생성
- consumer/producer 작성
- outbox/processed event entity 구현
- provider Controller 구현
- provider live endpoint 호출
- gateway route enable
- 운영 DB migration 적용
- monolith AI 코드 삭제
- 기존 HTTP client/mock 제거

## 5. 검증 결과

| 검증 | 결과 |
| --- | --- |
| `cd qtai-server; .\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.event.AiEventContractFixtureTest` | 통과 |
| `git diff --check` | 통과 |
| placeholder 검색 | 통과 |
| 금지 데이터/민감 값 검색 | 통과 |

## 6. 후속 작업

1. `ai-event-outbox-decision-record`
2. `provider-ai-input-event-contract`
3. `ai-generation-worker-design`
