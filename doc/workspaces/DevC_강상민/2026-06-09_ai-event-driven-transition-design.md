# AI Event-driven 전환 설계

## 1. 목적

이 문서는 ai-service MSA 분리 이후 긴 AI workflow를 어떤 기준으로 event-driven 구조로 전환할지 정리한다.

현재 ai-service는 module skeleton, inbound API, outbound HTTP client/mock, DB ownership/migration/usecase, runtime smoke, cutover checklist/runbook, gateway route skeleton, provider live smoke readiness까지 준비되어 있다. provider `/api/v1/system/**` endpoint는 아직 늦게 열릴 수 있으므로 이번 단계에서는 live 연결이나 메시징 구현을 시작하지 않는다.

이번 문서의 목적은 다음 작업의 경계를 고정하는 것이다.

- 즉시 응답이 필요한 호출은 HTTP로 유지한다.
- 긴 AI 처리, 후처리, 상태 변경 알림은 event-driven 전환 후보로 분리한다.
- Kafka, topic, consumer, producer 구현은 설계 승인 이후에만 검토한다.

## 2. 현재 전제

| 영역 | 현재 상태 |
| --- | --- |
| ai-service | 독립 모듈과 inbound API skeleton 준비 완료 |
| outbound provider 호출 | HTTP adapter/mock와 runtime toggle 준비 완료 |
| DB | AI 소유 7개 테이블 skeleton과 migration skeleton 준비 완료 |
| gateway | `ai-service-cutover` route skeleton 준비 완료, 기본 disabled |
| provider | `/api/v1/system/**` endpoint open 대기 가능성 있음 |
| cutover | readiness checklist, runbook, handoff 문서 준비 완료 |

## 3. 설계 결정 요약

| 결정 | 내용 |
| --- | --- |
| HTTP 유지 | 조회, 권한 검증, 관리자 즉시 응답, provider 상태 확인 |
| Event 후보 | AI 생성 job, 검증 완료, 승인 asset publish 후처리, audit/notification 후처리 |
| Provider 연동 | 기존 HTTP client/mock 구조 유지 |
| QT/Bible 조회 | Provider AI input event 계약 확정 전까지 HTTP client 계약 유지 |
| 메시징 구현 | 이번 단계에서 구현하지 않음 |
| 데이터 원칙 | 이벤트 payload에는 식별자와 상태만 담고 원문, prompt, provider raw response, 민감 값은 담지 않음 |
| 전환 방식 | 문서 설계 → 계약 fixture → outbox/processed event skeleton → worker skeleton → live 전환 순서 |

## 4. HTTP로 유지할 대상

| 대상 | 유지 이유 | 현재 계약 |
| --- | --- | --- |
| QT context 조회 | AI 생성/검증 직전에 필요한 동기 입력 | `QtContextClient` |
| 오늘 QT status 조회 | 상태 확인성 read API | `QtContextClient` |
| Bible verse 조회 | AI 입력 조립에 필요한 동기 read | `BibleVerseClient` |
| Admin/Auth 권한 검증 | 관리자 요청 처리 전 즉시 거부가 필요 | `AdminAuthClient` |
| 관리자 목록/상세 조회 | 화면 응답이 즉시 필요 | ai-service inbound API |
| provider health/status 확인 | cutover와 smoke에서 즉시 판단 필요 | provider smoke/readiness |

HTTP 유지 대상은 event로 바꾸지 않는다. 특히 권한 검증과 관리자 화면 응답은 비동기화하면 실패 시점이 늦어지고 사용자에게 잘못된 성공 응답을 줄 수 있다.

Provider AI input event 계약이 확정되기 전까지 QT/Bible 참조 조회는 HTTP client 계약을 유지한다. Provider가 허용된 context block을 event로 제공하기로 합의되면 해당 조회는 event ingestion 구조로 재검토한다.

## 5. Event-driven 전환 후보

| 후보 | 전환 이유 | 우선순위 |
| --- | --- | --- |
| AI generation job requested | 요청 접수와 실제 생성 작업을 분리할 수 있음 | 높음 |
| AI generation job started/completed/failed | 긴 LLM 호출의 상태 추적이 필요 | 높음 |
| AI validation completed | 검증 결과 후처리와 관리자 review 흐름 연결 | 높음 |
| Approved asset publish 후처리 | Study publish, audit, read model 반영을 분리 가능 | 중간 |
| Audit log 기록 | 본 요청 성공 여부와 감사 로그 저장을 분리할 수 있음 | 중간 |
| Notification 후처리 | 사용자 알림은 본 요청 응답과 분리 가능 | 낮음 |
| Provider AI input prepared | provider가 AI 입력 준비 완료를 알려주는 구조 검토 가능 | 낮음 |

## 6. 이벤트 카탈로그 초안

이번 카탈로그는 topic 정의가 아니라 event name과 payload 원칙 초안이다.

| Event name | Producer 후보 | Consumer 후보 | 핵심 payload |
| --- | --- | --- | --- |
| `AiGenerationJobRequested` | ai-service inbound | AI worker | `eventId`, `jobId`, `passageId`, `requestedBy`, `traceId` |
| `AiGenerationJobStarted` | AI worker | ai-service status updater | `eventId`, `jobId`, `startedAt`, `traceId` |
| `AiGenerationJobCompleted` | AI worker | ai-service persistence, validation trigger | `eventId`, `jobId`, `assetId`, `resultType`, `traceId` |
| `AiGenerationJobFailed` | AI worker | ai-service persistence, monitoring | `eventId`, `jobId`, `failureCode`, `traceId` |
| `AiValidationCompleted` | validation worker | ai-service persistence, admin monitoring | `eventId`, `assetId`, `validationResult`, `traceId` |
| `AiAssetApproved` | admin inbound | publish coordinator | `eventId`, `assetId`, `adminUserId`, `traceId` |
| `AiAssetPublishRequested` | publish coordinator | Study/Audit adapter worker | `eventId`, `assetId`, `idempotencyKey`, `traceId` |
| `AiAssetPublishFailed` | Study/Audit adapter worker | monitoring | `eventId`, `assetId`, `failureCode`, `traceId` |
| `ProviderAiInputPrepared` | provider service | ai-service ingestion | `eventId`, `sourceService`, `passageId`, `referenceId`, `traceId` |

## 7. Payload 원칙

이벤트 payload에는 다음 값만 허용한다.

- 식별자: `eventId`, `jobId`, `assetId`, `passageId`, `adminUserId`
- 상태: `requested`, `started`, `completed`, `failed`, `validationResult`, `failureCode`
- 추적: `traceId`, `traceparent`, `occurredAt`, `schemaVersion`
- 멱등성: `idempotencyKey`, `aggregateType`, `aggregateId`

이벤트 payload에 넣지 않는 값은 다음과 같다.

- LLM prompt 전문
- provider raw response 전문
- 성경 본문 전문
- 관리자 인증 값
- DB 접속 값
- 외부 service-token 값
- 사용자 민감 개인정보

본문이나 prompt가 필요한 worker는 event payload가 아니라 ai-service 소유 DB 또는 provider HTTP read 계약을 통해 필요한 시점에 조회한다.

## 8. 멱등성 기준

| 항목 | 기준 |
| --- | --- |
| event identity | 모든 이벤트는 `eventId`를 가진다. |
| aggregate identity | 상태 변경 이벤트는 `aggregateType`, `aggregateId`를 가진다. |
| duplicate handling | consumer는 `eventId` 또는 `idempotencyKey` 기준으로 중복 처리를 차단한다. |
| write endpoint 연계 | Study publish/hide, Audit write는 기존 `Idempotency-Key` 계약을 유지한다. |
| retry | 동일 이벤트 재처리는 같은 결과가 되도록 설계한다. |

## 9. 순서 보장 기준

순서 보장은 전체 이벤트 전역 기준이 아니라 aggregate 단위로만 요구한다.

| Aggregate | 필요한 순서 |
| --- | --- |
| `ai_generation_job` | requested → started → completed 또는 failed |
| `ai_generated_asset` | generated → validation completed → approved/rejected/hidden |
| `validation_reference_job` | created → fetched → expired |
| `publish request` | requested → completed 또는 failed |

서로 다른 job 또는 asset 간의 전역 순서는 요구하지 않는다.

## 10. 실패 처리 기준

| 실패 유형 | 처리 원칙 |
| --- | --- |
| 일시적 provider 실패 | retry 후보로 기록하고 즉시 성공 처리하지 않는다. |
| 권한 실패 | event로 넘기지 않고 HTTP 요청 단계에서 차단한다. |
| payload mapping 실패 | `RESPONSE_MAPPING_FAILED` 또는 event validation 실패로 기록한다. |
| 중복 이벤트 | processed event 기준으로 무시한다. |
| 재시도 초과 | 실패 상태를 저장하고 monitoring 대상에 올린다. |
| publish 후처리 실패 | asset 승인 상태와 publish 상태를 분리해서 기록한다. |

## 11. 관측성 기준

event-driven 전환 시 모든 이벤트 처리 로그에는 다음 정보를 포함한다.

- `eventId`
- event name
- aggregate type
- aggregate id
- handler name
- trace id
- result status
- error code

민감 값과 원문 본문은 로그에 남기지 않는다.

## 12. 단계별 전환 계획

| Phase | 작업명 후보 | 목표 | 구현 여부 |
| --- | --- | --- | --- |
| 0 | `ai-event-driven-transition-design` | HTTP/event 경계 설계 | 이번 문서 |
| 1 | `ai-event-contract-fixtures` | 이벤트 payload fixture와 schema 초안 작성 | 문서/테스트 fixture |
| 2 | `ai-event-outbox-skeleton` | ai-service outbox/processed event skeleton 검토 | 구현 전 별도 승인 |
| 3 | `ai-generation-worker-design` | 긴 AI 생성 workflow worker 경계 설계 | 문서 |
| 4 | `ai-generation-worker-skeleton` | opt-in worker skeleton 작성 | 설계 승인 후 |
| 5 | `provider-ai-input-event-contract` | provider가 발행할 AI 입력 이벤트 계약 검토 | provider 합의 후 |
| 6 | `ai-event-live-smoke-readiness` | 실제 broker 연결 전 opt-in smoke 준비 | 구현 승인 후 |

## 13. provider와의 합의 필요 항목

| 질문 | 결정 필요 이유 |
| --- | --- |
| provider가 AI 입력 이벤트를 발행할지 여부 | provider가 단순 HTTP 제공자인지, 입력 준비 이벤트 producer인지 결정해야 한다. |
| 이벤트 payload에 허용할 context 범위 | 본문 원문 전체를 이벤트에 싣지 않기 위해 필요하다. |
| QT/Bible 참조 조회 전환 여부 | 허용된 context block event 제공 합의 전까지는 HTTP client 계약을 유지해야 한다. |
| provider event retry 책임 | provider 발행 실패와 ai-service 수신 실패의 책임 경계를 나눠야 한다. |
| event schema versioning | provider와 ai-service가 독립 배포될 수 있어야 한다. |

## 14. 지금 하지 않는 작업

- Kafka 의존성 추가
- topic, consumer, producer 구현
- provider HTTP client 제거
- provider live endpoint 호출
- gateway route 실제 enable
- 운영 DB migration 적용
- monolith AI 코드 삭제
- Study/Audit write smoke 실행

## 15. 다음 권장 작업

1. `ai-event-contract-fixtures`
   - 이벤트 이름, 필수 필드, 금지 필드, 실패 fixture를 문서와 테스트 리소스로 고정한다.
2. `ai-event-outbox-decision-record`
   - outbox/processed event 저장소를 ai-service 소유 DB에 둘지 ADR로 결정한다.
3. `provider-ai-input-event-contract`
   - provider가 AI 입력 이벤트 producer가 될지 합의한다.
4. `ai-generation-worker-design`
   - AI 생성 작업을 HTTP request thread 밖으로 빼는 worker 경계를 설계한다.

## 16. 완료 기준

- HTTP 유지 대상과 event-driven 후보가 분리되어 있다.
- 이벤트 payload에 허용되는 값과 금지되는 값이 명시되어 있다.
- 멱등성, 순서, 실패 처리, 관측성 기준이 정리되어 있다.
- 구현하지 않는 범위가 명확하다.
- 다음 작업 순서가 설계 문서에 기록되어 있다.
