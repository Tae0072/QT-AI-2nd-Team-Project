# Provider AI Input Event Contract

## 1. 목적

이 문서는 provider가 AI 입력 준비 완료를 event로 알려줄 수 있는 조건과 `ProviderAiInputPrepared` payload 계약을 정리한다.

현재 ai-service는 provider `/api/v1/system/**` endpoint를 HTTP client로 호출하는 구조를 유지한다. provider AI input event는 이 HTTP 계약을 즉시 대체하지 않는다. provider와 ai-service가 허용된 context block event 제공에 합의한 뒤에만 event ingestion 구조를 재검토한다.

## 2. 계약 상태

| 항목 | 기준 |
| --- | --- |
| 계약 상태 | Proposed |
| event name | `ProviderAiInputPrepared` |
| producer 후보 | `today-qt` |
| consumer 후보 | `ai-service ingestion` |
| 적용 시점 | provider 합의 후 별도 fixture/test sync PR |
| 현재 QT/Bible 조회 | HTTP client 계약 유지 |
| 기존 provider endpoint | `/api/v1/system/**` 유지 |

## 3. 핵심 결정

- `ProviderAiInputPrepared`는 provider readiness 후속 계약이다.
- 이 event는 기존 QT/Bible HTTP read endpoint를 대체하지 않는다.
- event payload는 허용된 metadata/context block 식별 정보만 담는다.
- 본문 원문, Bible verse text, prompt, provider raw response, 인증 값, DB 접속 값은 payload와 log에 넣지 않는다.
- provider AI input event 계약 승인 전까지 QT/Bible 참조 조회는 기존 HTTP client 계약을 유지한다.
- provider가 허용된 context block을 event로 제공하기로 합의한 뒤에만 ai-service event ingestion 구조를 재검토한다.

## 4. Event Envelope

| 필드 | 필수 | 기준 |
| --- | --- | --- |
| `schemaVersion` | 예 | event schema version |
| `eventId` | 예 | event 고유 UUID |
| `eventName` | 예 | `ProviderAiInputPrepared` |
| `aggregateType` | 예 | `provider_ai_input` |
| `aggregateId` | 예 | `provider-input-{passageId}` 형식 권장 |
| `traceId` | 예 | provider trace id |
| `traceparent` | 예 | W3C traceparent |
| `occurredAt` | 예 | event 발생 시각 |
| `payload` | 예 | 아래 payload 계약 |

## 5. Payload Contract

| 필드 | 필수 | 기준 |
| --- | --- | --- |
| `sourceService` | 예 | provider service name. 1차 후보는 `today-qt` |
| `passageId` | 예 | QT passage id |
| `referenceId` | 예 | provider 내부 입력 reference id |
| `contextBlockType` | 예 | `ALLOWED_METADATA_CONTEXT_BLOCK` |

`contextBlockType=ALLOWED_METADATA_CONTEXT_BLOCK`는 event payload가 본문 원문 전체가 아니라 AI 생성/검증에 사용할 수 있는 허용된 metadata/context block 식별 정보만 담는다는 뜻이다.

## 6. Example Shape

```json
{
  "schemaVersion": "0.1.0",
  "eventId": "00000000-0000-0000-0000-000000000109",
  "eventName": "ProviderAiInputPrepared",
  "aggregateType": "provider_ai_input",
  "aggregateId": "provider-input-35",
  "traceId": "trace-provider-35",
  "traceparent": "00-99999999999999999999999999999999-aaaaaaaaaaaaaaaa-01",
  "occurredAt": "2026-06-09T00:12:00Z",
  "payload": {
    "sourceService": "today-qt",
    "passageId": 35,
    "referenceId": "qt-passage-35",
    "contextBlockType": "ALLOWED_METADATA_CONTEXT_BLOCK"
  }
}
```

## 7. 금지 필드

event payload와 log에는 다음 값을 넣지 않는다.

- prompt 전문
- provider raw response 전문
- 본문 원문
- Bible verse text
- 인증 값
- DB 접속 값
- 사용자 민감 정보

context 본문이 필요한 경우 event payload가 아니라 기존 HTTP read 계약 또는 ai-service 소유 DB 조회를 통해 필요한 시점에 가져온다.

## 8. HTTP 유지 기준

| 조회 | 현재 기준 |
| --- | --- |
| QT context 조회 | `QtContextClient` HTTP 계약 유지 |
| 오늘 QT status 조회 | `QtContextClient` HTTP 계약 유지 |
| Bible verse 조회 | `BibleVerseClient` HTTP 계약 유지 |

provider AI input event 계약이 승인되어도 HTTP read 계약은 즉시 제거하지 않는다. event ingestion으로 전환하려면 별도 설계, fixture sync, smoke readiness, cutover 계획이 필요하다.

## 9. Provider 합의 조건

provider 담당자와 다음 항목이 합의되어야 event ingestion 후속 작업을 시작할 수 있다.

| 항목 | 합의 기준 |
| --- | --- |
| producer 책임 | `today-qt`가 `ProviderAiInputPrepared` 발행 주체인지 확정 |
| context 범위 | `ALLOWED_METADATA_CONTEXT_BLOCK`가 담는 metadata 범위 확정 |
| retry 책임 | provider 발행 실패와 ai-service 수신 실패의 책임 경계 확정 |
| schema versioning | `schemaVersion` 변경 정책 확정 |
| trace | `traceparent`, `traceId` 전파 기준 확정 |
| 중복 처리 | `eventId` 재전송 가능성과 멱등 처리 기준 확정 |

## 10. 기존 Provider Endpoint와의 관계

`ProviderAiInputPrepared`는 기존 `/api/v1/system/**` provider endpoint를 대체하지 않는다.

| 기존 계약 | 유지 여부 |
| --- | --- |
| `/api/v1/system/qt/passages/{passageId}/context` | 유지 |
| `/api/v1/system/qt/passages/today/status` | 유지 |
| `/api/v1/system/bible/verses/**` | 유지 |

event는 provider가 AI 입력 준비 완료를 알려주는 보조 계약이다. ai-service가 생성/검증 직전에 최신 context를 확인해야 하는 경우 HTTP read 계약을 계속 사용할 수 있다.

## 11. 후속 작업

| 작업 | 조건 |
| --- | --- |
| `provider-ai-input-event-fixture-sync` | provider가 계약을 승인한 뒤 fixture/test 확장 |
| `ai-generation-worker-design` | AI generation workflow를 worker 경계로 분리 |
| `ai-event-ingestion-skeleton` | ingestion 구현 승인 이후 |

## 12. 이번 문서에서 하지 않는 것

- Kafka dependency 추가
- topic 생성
- consumer/producer 구현
- event ingestion 구현
- outbox relay 구현
- provider Controller 구현
- DB migration 적용
- HTTP client/mock 제거
- 기존 event fixture/test 수정
