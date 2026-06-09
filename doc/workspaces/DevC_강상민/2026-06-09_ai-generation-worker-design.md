# AI Generation Worker 전환 설계

## 1. 목적

이 문서는 ai-service의 긴 AI generation workflow를 HTTP request thread 밖으로 분리할 때 필요한 worker 경계를 정의한다.

현재 ai-service는 inbound API, outbound HTTP client/mock, runtime toggle, DB ownership/migration/usecase, runtime smoke, cutover 문서, gateway skeleton, event contract fixture, outbox 결정 기록까지 준비되어 있다. 다음 단계에서 실제 worker를 구현하기 전에 어떤 책임을 generation worker 후보에 둘지 먼저 고정한다.

이번 문서는 설계 전용이다. Kafka, topic, consumer, producer, outbox table, worker 구현, DeepSeek 실행 이관은 포함하지 않는다.

## 2. 설계 상태

| 항목 | 기준 |
| --- | --- |
| 설계 상태 | Proposed for worker skeleton |
| worker input 후보 | `AiGenerationJobRequested` |
| worker output 후보 | `AiGenerationJobStarted`, `AiGenerationJobCompleted`, `AiGenerationJobFailed` |
| worker 소유 서비스 | `ai-service` |
| job 저장소 | `ai_generation_jobs` |
| asset 저장소 | `ai_generated_assets` |
| event 발행 방식 | broker-agnostic outbox pattern 후보 |
| provider 조회 | provider event 합의 전까지 기존 HTTP client 계약 유지 |

## 3. 핵심 결정

- job 생성 API는 즉시 응답 가능한 현재 경계를 유지한다.
- 실제 LLM generation은 worker 후보로 분리한다.
- `AiGenerationJobRequested`는 worker 시작 신호 후보이며, job 실행 자체를 의미하지 않는다.
- worker는 job 상태를 `QUEUED -> RUNNING -> SUCCEEDED/FAILED`로 전이한다.
- 성공 시 worker는 생성 결과를 `ai_generated_assets`에 저장한다.
- 성공/실패 결과는 outbox 후보 event로 기록한다.
- validation, approval, publish는 generation worker 책임에서 분리한다.
- `ProviderAiInputPrepared`가 승인되기 전까지 QT/Bible 참조 조회는 기존 HTTP client 계약을 유지한다.

## 4. Worker 책임

| 책임 | 기준 |
| --- | --- |
| job claim | `QUEUED` job을 실행 대상으로 잡고 `RUNNING`으로 전이 |
| 입력 조회 | QT/Bible 참조는 현재 HTTP client 계약으로 조회 |
| generation 실행 | LLM 호출 경계는 worker 내부 후보로 둔다 |
| 성공 저장 | 생성 결과를 `ai_generated_assets`에 저장 |
| 성공 event | `AiGenerationJobCompleted` outbox append 후보 |
| 실패 저장 | job을 `FAILED`로 전이하고 실패 코드를 기록 |
| 실패 event | `AiGenerationJobFailed` outbox append 후보 |
| trace 전파 | `traceId`, `traceparent`를 job 처리 로그와 event 후보에 연결 |

## 5. Worker가 하지 않는 일

| 제외 책임 | 이유 |
| --- | --- |
| Study publish/hide 직접 호출 | approval 이후 publish coordinator 후보 책임 |
| Audit write 직접 수행 | audit adapter worker 또는 coordinator 후보 책임 |
| AdminAuth 검증 | 관리자 inbound 요청 단계 책임 |
| provider event ingestion | `ProviderAiInputPrepared` 합의 이후 별도 ingestion 책임 |
| validation 최종 판단 | validation worker 후보 책임 |
| gateway route 전환 | cutover/gateway 작업 책임 |

generation worker는 생성 결과를 만들고 AI 소유 DB 상태를 전이하는 데 집중한다. 게시, 감사, 승인 후 후처리는 별도 worker 또는 coordinator 후보로 분리한다.

## 6. Event 흐름 후보

| 단계 | producer 후보 | consumer 후보 | 결과 |
| --- | --- | --- | --- |
| generation 요청 | ai-service inbound | generation worker | `AiGenerationJobRequested` |
| generation 시작 | generation worker | status updater 후보 | `AiGenerationJobStarted` |
| generation 성공 | generation worker | persistence/validation trigger 후보 | `AiGenerationJobCompleted` |
| generation 실패 | generation worker | monitoring 후보 | `AiGenerationJobFailed` |

이 흐름은 event 이름과 책임 경계 후보를 고정하는 설계이다. 실제 broker, topic, consumer 구현은 별도 승인 뒤 진행한다.

## 7. 상태 전이 기준

| 현재 상태 | 다음 상태 | 허용 주체 | 기준 |
| --- | --- | --- | --- |
| `QUEUED` | `RUNNING` | generation worker | job claim 성공 |
| `RUNNING` | `SUCCEEDED` | generation worker | asset 저장 성공 |
| `RUNNING` | `FAILED` | generation worker | generation, 입력 조회, 저장 중 실패 |
| `QUEUED` | `FAILED` | generation worker | 실행 전 검증 실패 |

terminal 상태인 `SUCCEEDED`, `FAILED`에서 다시 실행 상태로 되돌리는 흐름은 worker skeleton에서 허용하지 않는다.

## 8. 입력 조회 기준

| 입력 | 현재 기준 |
| --- | --- |
| QT context | `QtContextClient` HTTP 계약 유지 |
| Today QT status | `QtContextClient` HTTP 계약 유지 |
| Bible verse | `BibleVerseClient` HTTP 계약 유지 |
| Provider AI input event | 승인 전까지 참고 후보로만 유지 |

`ProviderAiInputPrepared`는 기존 provider endpoint를 대체하지 않는다. provider가 허용된 context block을 event로 제공하기로 합의한 뒤에만 generation worker 입력 구조를 event ingestion 기반으로 재검토한다.

## 9. Payload와 Log 제한

event payload와 worker log에는 식별자, 상태, trace, idempotency 값만 남긴다.

허용 후보:

- `eventId`
- `jobId`
- `assetId`
- `passageId`
- `traceId`
- `traceparent`
- `failureCode`
- `resultType`
- `schemaVersion`

저장하지 않는 값:

- provider raw response 전문
- prompt 전문
- 본문 원문
- 인증 값
- DB 접속 값
- 사용자 민감 정보

본문이나 prompt가 필요한 경우 event payload가 아니라 ai-service 소유 DB 또는 provider HTTP read 계약을 통해 필요한 시점에 조회한다.

## 10. 실패 처리 기준

| 실패 유형 | 처리 기준 |
| --- | --- |
| QT/Bible 조회 실패 | job `FAILED`, `AiGenerationJobFailed` 후보 기록 |
| LLM 호출 실패 | job `FAILED`, 실패 코드 기록 |
| asset 저장 실패 | transaction rollback, 재처리 후보로 남김 |
| event append 실패 | job 상태 변경과 같은 transaction 기준으로 실패 처리 |
| 중복 event | `eventId + handlerName` processed event 기준 후보로 차단 |

실패 로그에는 `eventId`, event name, handler name, job id, error code/message를 남긴다. 민감 값과 본문 원문은 남기지 않는다.

## 11. Validation, Approval, Publish 분리

| 흐름 | generation worker와의 관계 |
| --- | --- |
| validation | generation 성공 이후 별도 validation worker 후보 |
| approval | admin inbound 또는 approval coordinator 후보 |
| publish | `AiAssetApproved` 이후 publish coordinator 후보 |
| audit | publish/approval 결과에 대한 별도 audit adapter 후보 |

`AiAssetApproved`는 관리자 승인 상태만 의미한다. Study publish 성공을 의미하지 않는다.

## 12. 다음 구현 후보

| 작업 | 조건 |
| --- | --- |
| `ai-event-outbox-skeleton` | outbox table과 processed event skeleton 승인 |
| `ai-generation-worker-skeleton` | worker 경계 설계 승인 |
| `ai-generation-worker-fixture-sync` | worker skeleton에서 필요한 event fixture 확장 승인 |
| `ai-event-live-smoke-readiness` | broker 연결 방식 승인 |

## 13. 이번 문서에서 하지 않는 것

- Kafka 의존성 추가
- topic 생성
- consumer/producer 구현
- outbox table migration
- JPA entity/repository 구현
- relay worker 구현
- generation worker 구현
- DeepSeek flow 이관
- HTTP client 제거
- provider live 연결
- gateway route 활성화
