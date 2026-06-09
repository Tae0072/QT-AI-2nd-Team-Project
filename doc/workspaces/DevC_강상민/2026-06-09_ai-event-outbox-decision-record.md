# AI Event Outbox Decision Record

## Status

Accepted for design. Implementation requires a separate approval PR.

## Context

ai-service MSA 분리 작업은 module, inbound API, outbound HTTP client/mock, DB ownership/migration/usecase, runtime smoke, cutover 문서, gateway route skeleton까지 준비했다. 이어서 event-driven 전환 설계와 event contract fixture가 작성되었다.

다음 단계에서 AI generation, validation, publish 후처리를 비동기화하려면 event 발행과 중복 처리 책임을 먼저 고정해야 한다. 특히 AI 생성 job과 asset 승인 흐름은 DB 상태 변경과 외부 write 연계가 함께 발생할 수 있어, event 유실과 중복 처리 기준이 결정되지 않으면 후속 구현이 흔들린다.

## Decision Drivers

- ai-service가 AI 소유 DB 테이블을 이미 가진다.
- event payload는 식별자, 상태, trace, idempotency 값만 가져야 한다.
- prompt, provider raw response, 본문, 인증 값은 event payload와 log에 남기지 않는다.
- 외부 write endpoint는 기존 `Idempotency-Key` 계약을 유지해야 한다.
- Kafka 구현은 아직 시작하지 않는다.
- provider `/api/v1/system/**` endpoint는 늦게 열릴 수 있다.

## Decision

ai-service event 발행은 broker-agnostic outbox pattern을 기준으로 설계한다. outbox와 processed event 저장소는 ai-service 소유 DB에 둔다.

후보 테이블명은 다음으로 고정한다.

| 테이블 후보 | 책임 |
| --- | --- |
| `ai_event_outbox` | ai-service가 발행해야 할 event를 저장한다. |
| `ai_processed_events` | handler별 event 처리 완료 이력을 저장해 중복 처리를 차단한다. |

이번 결정은 테이블명과 책임을 고정하는 문서 결정이다. 실제 migration, entity, repository, relay worker는 별도 PR에서 다룬다.

## Detailed Decisions

### 1. 저장소 소유권

`ai_event_outbox`와 `ai_processed_events`는 ai-service 소유 DB에 둔다. AI generation job, generated asset, validation log, validation reference job과 같은 AI 소유 상태 변경에서 발생하는 event이므로 저장 책임도 ai-service가 가진다.

다른 provider 서비스는 ai-service outbox 테이블을 직접 조회하거나 쓰지 않는다.

### 2. Producer Transaction

ai-service producer는 domain state 변경과 outbox append를 같은 DB transaction 안에서 처리한다.

예시는 다음과 같다.

| 흐름 | 같은 transaction 안에서 처리할 후보 |
| --- | --- |
| generation job 생성 | `ai_generation_jobs` 저장 + `AiGenerationJobRequested` outbox append |
| asset 승인 | `ai_generated_assets` review 상태 변경 + `AiAssetApproved` outbox append |
| validation 완료 | `ai_validation_logs` 저장 + `AiValidationCompleted` outbox append |

HTTP 응답 thread에서 broker로 직접 publish하지 않는다.

### 3. Relay 책임

outbox relay는 `ai_event_outbox`에 저장된 event를 broker 또는 후속 dispatch 경로로 전달하는 책임만 가진다.

relay가 가져야 할 최소 책임은 다음과 같다.

- pending event 조회
- publish 시도
- 성공 시 published 상태 기록
- 실패 시 retry count와 error code/message 기록
- trace id 전파

relay 구현은 이번 PR에서 하지 않는다.

### 4. Handler Idempotency

consumer 또는 handler는 `eventId + handlerName` 기준으로 중복 처리를 차단한다.

`eventId`만으로 중복을 막으면 서로 다른 handler가 같은 event를 처리해야 하는 경우를 표현하기 어렵다. 따라서 handler 단위 처리 이력을 `ai_processed_events`에 기록하는 모델을 기준으로 한다.

### 5. 외부 Write 연계

Study publish/hide, Audit write와 같은 외부 write endpoint는 기존 `Idempotency-Key` 계약을 유지한다.

event가 외부 write를 유발하는 경우 `idempotencyKey`는 event payload 또는 handler 내부 deterministic key로 전달한다. 같은 event를 재처리해도 같은 external write key를 사용해야 한다.

### 6. Asset 승인과 Publish 상태 분리

`AiAssetApproved`는 관리자 승인 상태를 의미한다. Study publish 성공을 의미하지 않는다.

승인과 publish 결과는 다음처럼 분리한다.

| 상태 | 의미 |
| --- | --- |
| asset approved | 관리자가 AI asset을 승인했다. |
| publish requested | 승인 asset의 Study 반영을 요청했다. |
| publish completed | Study provider 반영이 성공했다. |
| publish failed | Study provider 반영이 실패했다. |

이 분리는 rollback과 재시도를 위해 필요하다.

### 7. Retry 책임

retry 책임은 두 계층으로 나눈다.

| 계층 | 책임 |
| --- | --- |
| outbox relay retry | event publish 실패 재시도 |
| handler retry | event 수신 후 provider write 또는 내부 처리 실패 재시도 |

두 계층 모두 실패 로그에 `eventId`, event name, handler name, error code/message를 남긴다.

### 8. Payload와 Log 제한

event payload와 log에는 다음 값만 허용한다.

- 식별자
- 상태
- trace id
- idempotency key
- event schema version
- aggregate type/id

다음 값은 저장하지 않는다.

- prompt 전문
- provider raw response 전문
- 본문 전문
- 인증 값
- DB 접속 값
- 사용자 민감 정보

## Considered Options

| 옵션 | 판단 |
| --- | --- |
| broker 직접 publish | DB commit과 publish 사이 유실 위험이 있어 채택하지 않는다. |
| provider별 outbox | AI event의 소유권이 흩어져 현재 ai-service 분리 단계와 맞지 않는다. |
| ai-service DB outbox | AI 상태 변경과 event 발행 의도를 같은 transaction으로 묶을 수 있어 채택한다. |
| eventId 단독 deduplication | handler별 처리 이력을 표현하기 어려워 채택하지 않는다. |

## Consequences

### Positive

- AI 상태 변경과 event 발행 의도를 한 transaction으로 묶을 수 있다.
- broker 종류와 구현 시점을 뒤로 미룰 수 있다.
- handler별 중복 처리 기준이 명확해진다.
- Study publish와 asset approval 상태를 분리해 장애 대응이 쉬워진다.

### Negative

- ai-service DB에 추가 테이블이 필요하다.
- relay worker와 processed event cleanup 정책이 필요하다.
- event 상태 관리와 재처리 운영 절차가 추가된다.

### Risks

| 위험 | 완화 |
| --- | --- |
| outbox backlog 증가 | relay lag metric과 오래된 pending event 알림을 설계한다. |
| processed event 테이블 증가 | retention 정책을 별도 결정한다. |
| handler 재처리 중 외부 write 중복 | deterministic `Idempotency-Key`를 유지한다. |
| event schema 변경 충돌 | `schemaVersion`과 fixture 기반 계약 테스트를 유지한다. |

## Implementation Notes

후속 구현 PR에서 검토할 최소 컬럼 후보는 다음과 같다. 이번 문서는 migration을 만들지 않는다.

| 테이블 | 후보 컬럼 |
| --- | --- |
| `ai_event_outbox` | `event_id`, `event_name`, `aggregate_type`, `aggregate_id`, `schema_version`, `payload_json`, `status`, `retry_count`, `last_error_code`, `last_error_message`, `trace_id`, `created_at`, `published_at` |
| `ai_processed_events` | `event_id`, `handler_name`, `aggregate_type`, `aggregate_id`, `status`, `processed_at`, `last_error_code`, `last_error_message` |

## Related Documents

- `doc/workspaces/DevC_강상민/2026-06-09_ai-event-driven-transition-design.md`
- `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-event-contract-fixtures.md`
- `qtai-server/ai-service/src/test/resources/contracts/ai-events/ai-event-contract-fixtures.json`

## Not In This Decision

- Kafka dependency
- topic creation
- consumer/producer implementation
- DB migration
- JPA entity/repository
- relay worker
- provider live integration
- gateway route enable
- monolith AI deletion
