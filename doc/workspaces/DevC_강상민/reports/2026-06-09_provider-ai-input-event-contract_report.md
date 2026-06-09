# provider-ai-input-event-contract Report

## 1. 작업 개요

- 작업 브랜치: `docs/provider-ai-input-event-contract`
- 작업 유형: 문서 전용
- 목적: provider가 `ProviderAiInputPrepared` 이벤트 producer가 될 수 있는 조건과 payload 계약을 합의 가능한 문서로 고정
- 선행 조건:
  - `ai-event-outbox-decision-record` 문서가 `dev`에 반영되어 있음
  - `ai-event-contract-fixtures.json`이 `dev`에 반영되어 있음

## 2. 생성 문서

- `doc/workspaces/DevC_강상민/workflows/2026-06-09_provider-ai-input-event-contract.md`
- `doc/workspaces/DevC_강상민/2026-06-09_provider-ai-input-event-contract.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-09_provider-ai-input-event-contract_report.md`

## 3. 반영 내용

- 이벤트 이름을 `ProviderAiInputPrepared`로 고정했다.
- producer 후보를 `today-qt`, consumer 후보를 `ai-service ingestion`으로 명시했다.
- 공통 envelope 필드를 고정했다.
  - `schemaVersion`
  - `eventId`
  - `eventName`
  - `aggregateType`
  - `aggregateId`
  - `traceId`
  - `traceparent`
  - `occurredAt`
  - `payload`
- payload 필드를 고정했다.
  - `sourceService`
  - `passageId`
  - `referenceId`
  - `contextBlockType`
- `contextBlockType`은 `ALLOWED_METADATA_CONTEXT_BLOCK`만 허용하도록 기록했다.
- `aggregateType`은 `provider_ai_input`으로 고정했다.
- `aggregateId`는 `provider-input-{passageId}` 형식을 권장값으로 기록했다.
- 이벤트 payload와 로그에 넣으면 안 되는 값을 명확히 분리했다.
  - 본문 원문
  - Bible verse text
  - prompt
  - provider raw response
  - 인증 값
  - DB 접속 값
- provider AI input event 계약 승인 전까지 QT/Bible 참조 조회는 기존 HTTP client 계약을 유지한다는 기준을 명시했다.
- `ProviderAiInputPrepared`는 `/api/v1/system/**` provider endpoint를 대체하지 않고, provider readiness 이후 검토할 후속 이벤트 계약임을 명시했다.

## 4. 제외 범위

- Kafka 의존성 추가
- topic 생성
- consumer/producer 구현
- event ingestion 구현
- outbox relay 구현
- provider Controller 구현
- DB migration 추가
- 기존 HTTP client 제거
- 기존 fixture/test 수정

## 5. 검증 결과

- `git diff --check`: 통과
- 선행 산출물 존재 확인:
  - `doc/workspaces/DevC_강상민/2026-06-09_ai-event-outbox-decision-record.md`: 존재
  - `qtai-server/ai-service/src/test/resources/contracts/ai-events/ai-event-contract-fixtures.json`: 존재
- placeholder 문구 검색: 매칭 없음
- 금지 데이터/민감 예시 문구 검색: 매칭 없음

## 6. 후속 작업

- provider와 `ProviderAiInputPrepared` payload 승인 여부를 합의한다.
- provider가 허용된 context block을 event로 제공하기로 합의하면 `provider-ai-input-event-fixture-sync`로 fixture/test를 동기화한다.
- event ingestion 구현은 설계 승인 이후 별도 작업으로 진행한다.
- Kafka, topic, producer, consumer 구현은 이번 결정과 별도 승인 후 진행한다.
