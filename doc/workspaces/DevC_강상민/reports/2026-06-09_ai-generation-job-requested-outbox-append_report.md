# ai-generation-job-requested-outbox-append Report

## 작업 요약

- `feature/ai-generation-job-requested-outbox-append` 브랜치에서 generation job 생성 시 `AiGenerationJobRequested` outbox event를 append하도록 연결했다.
- system generation job 생성과 admin regenerate job 생성 모두 job 저장 transaction 안에서 requested event를 저장한다.
- worker 실행, scheduler, Kafka/topic/relay, provider live 호출은 구현하지 않았다.

## 변경 내용

- `AiService`에 `AiEventOutboxRepository`를 주입했다.
- `saveQueuedJob(...)` 성공 직후 `AiGenerationJobRequested` event를 저장하도록 했다.
- requested event payload는 식별자/상태 값만 포함한다.
  - `jobId`, `jobType`, `targetType`, `targetId`, `promptVersionId`, `requestedBy`, `requestSource`, `requestedAt`
  - `QT_PASSAGE` target은 fixture 호환용 `passageId` alias 포함
- `ai-event-contract-fixtures.json`의 `AiGenerationJobRequested` required field를 production payload 의미와 맞췄다.
- system/admin persistence 테스트에 requested outbox payload 단언을 추가했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 성공 |
| `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiServiceSystemUseCasePersistenceTest --tests com.qtai.domain.ai.internal.AiServiceAssetReviewPersistenceTest --tests com.qtai.domain.ai.event.AiEventContractFixtureTest --tests com.qtai.domain.ai.internal.AiServiceEventOutboxPersistenceTest` | 성공 |
| `git diff --check` | 성공 |
| `rg -n "DeepSeek\|LlmClient\|external\\.llm\|Kafka\|topic" "qtai-server\ai-service\src\main\java"` | 매칭 없음 |
| 변경 코드/fixture/report 금지 데이터 검색 | 매칭 없음 |

## 검증 참고

- 계획의 전체 `doc/workspaces/DevC_강상민` 금지어 검색은 기존 문서의 정책 문구와 검증 명령 예시를 매칭했다.
- 이번 변경 코드, event fixture, report에는 금지 데이터와 민감 예시 값이 추가되지 않았음을 별도 검색으로 확인했다.

## 수용 기준 확인

- system generation job 생성 시 job과 requested outbox가 함께 저장됨을 확인했다.
- admin regenerate job 생성 시 requested outbox가 함께 저장됨을 확인했다.
- duplicate job 생성 실패 시 requested outbox가 추가되지 않음을 확인했다.
- payload의 `requestedBy`, `requestSource`, `targetType`, `targetId`, `passageId` 정책을 테스트로 고정했다.
- fixture 계약 테스트가 production payload 의미와 함께 통과했다.

## 제외 범위 준수

- worker 실행과 scheduler는 구현하지 않았다.
- Kafka, topic, producer, consumer, relay worker는 구현하지 않았다.
- provider live endpoint 호출은 추가하지 않았다.
- LLM executor 구현은 추가하지 않았다.
- DB migration은 변경하지 않았다.

## 후속 작업

- generation worker scheduler opt-in
- production LLM executor 구현
- outbox relay worker와 broker 연결
- provider input event ingestion 전환 검토
