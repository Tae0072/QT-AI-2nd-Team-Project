# Workflow - 2026-06-09 ai-event-contract-fixtures

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `test/ai-event-contract-fixtures` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | `ai-event-driven-transition-design`에서 event-driven 후보가 정리되었으므로, 구현 전에 이벤트 payload fixture와 검증 기준을 테스트 가능한 형태로 고정한다. |
| 기준 문서 | `2026-06-09_ai-event-driven-transition-design.md`, `2026-06-09_ai-service-operational-handoff.md` |
| 해당 경로 | `qtai-server/ai-service/src/test/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

Kafka 구현 전 단계로 AI event contract fixture를 테스트 리소스에 고정한다. event name, 공통 필드, event별 payload 필수 필드, 금지 필드, QT/Bible HTTP 유지 결정 조건을 검증 테스트로 묶어 후속 설계와 구현이 같은 계약을 따르도록 한다.

## 범위

- `ai-service` 테스트 리소스에 event contract fixture JSON을 추가한다.
- AI event 후보 9종의 success fixture를 추가한다.
- 공통 필드와 event별 payload 필수 필드 catalog를 fixture에 포함한다.
- 금지 payload field 목록을 fixture에 포함한다.
- JUnit 테스트로 fixture parse, event name, 공통 필드, payload 필드, 금지 필드, idempotency key, QT/Bible HTTP 유지 결정을 검증한다.
- workflow 문서와 report 문서를 작성한다.

## 제외 범위

- Kafka 의존성 추가
- topic 생성
- consumer/producer 구현
- outbox/processed event entity 구현
- provider Controller 구현
- provider live endpoint 호출
- gateway route enable
- 운영 DB migration 적용
- monolith AI 코드 삭제
- 기존 HTTP client/mock 제거

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-event-contract-fixtures.md` | 작업 범위와 검증 계획 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-event-contract-fixtures_report.md` | 실행 결과와 검증 결과 |
| Create | `qtai-server/ai-service/src/test/resources/contracts/ai-events/ai-event-contract-fixtures.json` | AI event contract fixture catalog |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/event/AiEventContractFixtureTest.java` | fixture 계약 검증 테스트 |

## 구현 순서

1. `dev` 최신화 후 `test/ai-event-contract-fixtures` 브랜치를 생성한다.
2. 기존 event-driven 설계 문서와 ai-service 테스트 구조를 확인한다.
3. workflow 문서를 작성한다.
4. event contract fixture JSON을 추가한다.
5. fixture 검증 테스트를 추가한다.
6. report 문서를 작성한다.
7. `:ai-service:test` 범위의 신규 테스트를 실행한다.
8. 문서 placeholder, 금지 데이터, trailing whitespace를 검증한다.
9. 지정 파일만 stage하고 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/event/AiEventContractFixtureTest.java` | expected event 9종 존재 |
| `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/event/AiEventContractFixtureTest.java` | event 공통 필드와 event name 일치 |
| `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/event/AiEventContractFixtureTest.java` | event별 payload 필수 필드 존재 |
| `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/event/AiEventContractFixtureTest.java` | valid event payload에 prompt/raw response/본문/secret 계열 필드 없음 |
| `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/event/AiEventContractFixtureTest.java` | `AiAssetPublishRequested`는 `idempotencyKey` 필수 |
| `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/event/AiEventContractFixtureTest.java` | QT/Bible 참조 조회는 provider input event 승인 전까지 HTTP 유지 |

## 수용 기준

- [ ] fixture JSON이 `ai-service` 테스트 리소스에 포함된다.
- [ ] fixture에 9개 event 후보가 포함된다.
- [ ] 테스트가 fixture를 실제로 로드하고 JSON pointer가 아니라 구조 전체를 검증한다.
- [ ] valid event payload에 금지 필드가 없다.
- [ ] `schemaVersion`, `eventId`, `eventName`, `aggregateType`, `aggregateId`, `traceId`, `traceparent`, `occurredAt`, `payload` 공통 필드가 검증된다.
- [ ] Provider AI input event 확정 전 QT/Bible 조회는 HTTP client 계약 유지로 검증된다.
- [ ] Kafka 구현, topic 생성, consumer/producer 구현은 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- fixture와 테스트가 같은 JSON 구조에 강하게 연결되어 있어 한 흐름으로 작성하는 편이 안전하다.
- 변경 파일 수가 적고 편집 경로가 좁다.
- Kafka 구현 금지와 금지 payload field 검증 기준을 일관되게 유지해야 한다.

### 위임 가능한 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, fixture, 테스트, report를 직접 작성하고 검증한다.

## 검증 계획

- `cd qtai-server`
- `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.event.AiEventContractFixtureTest`
- `cd ..`
- `git diff --check`
- placeholder 검색으로 미완성 문구가 없는지 확인한다.
- 금지 데이터 검색으로 허용되지 않은 번역본명, 외부 본문 출처명, 민감 값 예시가 fixture와 문서에 없는지 확인한다.

## 후속 작업으로 남길 항목

- `ai-event-outbox-decision-record`
- `provider-ai-input-event-contract`
- `ai-generation-worker-design`
- 설계 승인 후 outbox/processed event skeleton 검토
