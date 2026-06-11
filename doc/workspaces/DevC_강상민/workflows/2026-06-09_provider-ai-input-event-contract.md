# Workflow - 2026-06-09 provider-ai-input-event-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/provider-ai-input-event-contract` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | `ProviderAiInputPrepared`가 event fixture에 최소 형태로 고정되었고, provider가 AI 입력 이벤트 producer가 될 수 있는 조건을 합의용 계약 문서로 정리해야 한다. |
| 기준 문서 | `2026-06-09_ai-event-driven-transition-design.md`, `2026-06-09_ai-event-outbox-decision-record.md`, `2026-06-08_ai-provider-endpoint-readiness-checklist.md` |
| 해당 경로 | `doc/workspaces/DevC_강상민/**` |

## 작업 목표

provider가 `ProviderAiInputPrepared` event producer가 될 수 있는 조건과 payload 계약을 문서로 고정한다. 이번 작업은 provider 합의용 계약 문서 전용이며, Kafka, topic, consumer/producer, event ingestion, provider Controller, HTTP client 제거는 수행하지 않는다.

## 범위

- `ProviderAiInputPrepared` event name과 producer/consumer 후보를 고정한다.
- event envelope 필수 필드와 payload 필수 필드를 문서화한다.
- `contextBlockType=ALLOWED_METADATA_CONTEXT_BLOCK` 기준을 명시한다.
- payload와 log의 금지 데이터를 명시한다.
- provider AI input event 계약 승인 전까지 QT/Bible 참조 조회는 HTTP client 계약을 유지한다고 명시한다.
- 기존 `/api/v1/system/**` provider endpoint를 대체하지 않는 후속 계약임을 명시한다.
- workflow 문서, 계약 문서, report 문서를 작성한다.

## 제외 범위

- Kafka 의존성 추가
- topic 생성
- consumer/producer 구현
- event ingestion 구현
- outbox relay 구현
- provider Controller 구현
- DB migration 적용
- HTTP client/mock 제거
- 기존 event fixture/test 수정

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_provider-ai-input-event-contract.md` | 작업 범위와 검증 계획 |
| Create | `doc/workspaces/DevC_강상민/2026-06-09_provider-ai-input-event-contract.md` | provider AI input event 계약 본문 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_provider-ai-input-event-contract_report.md` | 수행 결과와 검증 결과 |

## 구현 순서

1. `dev`를 최신화한다.
2. outbox 결정 기록과 event fixture가 dev에 있는지 확인한다.
3. `docs/provider-ai-input-event-contract` 브랜치를 생성한다.
4. workflow 문서를 작성한다.
5. provider AI input event 계약 문서를 작성한다.
6. report 문서를 작성한다.
7. 문서 placeholder, 금지 데이터, trailing whitespace를 검증한다.
8. 문서 3개만 stage하고 커밋한다.

## 수용 기준

- [ ] 문서 3개가 생성된다.
- [ ] event name이 `ProviderAiInputPrepared`로 고정되어 있다.
- [ ] producer 후보가 `today-qt`, consumer 후보가 `ai-service ingestion`으로 기록되어 있다.
- [ ] envelope 필수 필드와 payload 필수 필드가 기록되어 있다.
- [ ] `contextBlockType=ALLOWED_METADATA_CONTEXT_BLOCK`가 기록되어 있다.
- [ ] payload/log 금지 데이터가 명시되어 있다.
- [ ] 계약 승인 전까지 QT/Bible 참조 조회는 HTTP client 계약 유지로 기록되어 있다.
- [ ] 기존 `/api/v1/system/**` provider endpoint를 대체하지 않는다고 기록되어 있다.
- [ ] Kafka, topic, consumer, producer, event ingestion 구현이 제외되어 있다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 문서 3개가 같은 provider 합의 계약을 공유한다.
- 변경 범위가 문서 전용이며 병렬화 이점이 작다.
- 금지 데이터와 HTTP 유지 조건을 문서 전체에 일관되게 반영해야 한다.

### 위임 가능한 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, 계약 문서, report를 직접 작성하고 검증한다.

## 검증 계획

- 문서 전용 변경이므로 Gradle 테스트는 실행하지 않는다.
- `git diff --check`
- 전제 산출물 존재 확인:
  - `doc/workspaces/DevC_강상민/2026-06-09_ai-event-outbox-decision-record.md`
  - `qtai-server/ai-service/src/test/resources/contracts/ai-events/ai-event-contract-fixtures.json`
- placeholder 검색으로 미완성 표현이 남아 있지 않은지 확인한다.
- 금지 데이터 검색으로 허용되지 않은 번역본명, 외부 본문 출처명, 민감 값 예시가 문서에 없는지 확인한다.

## 후속 작업으로 남길 항목

- `ai-generation-worker-design`
- provider 합의 후 `provider-ai-input-event-fixture-sync`
