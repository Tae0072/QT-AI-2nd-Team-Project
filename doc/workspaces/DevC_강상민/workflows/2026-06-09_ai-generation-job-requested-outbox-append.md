# Workflow - 2026-06-09 ai-generation-job-requested-outbox-append

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-generation-job-requested-outbox-append` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | generation job 생성과 outbox event append를 같은 transaction 안에서 고정해야 worker/event 전환 흐름을 이어갈 수 있다. |
| 기준 문서 | `2026-06-09_ai-event-outbox-decision-record.md`, `2026-06-09_ai-event-outbox-skeleton.md`, `2026-06-09_ai-generation-worker-design.md` |
| 해당 경로 | `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`CreateAiGenerationJobUseCase`와 `RegenerateAiAssetUseCase`에서 generation job을 생성할 때 `AiGenerationJobRequested` outbox event를 같은 transaction 안에 저장한다. 이번 작업은 requested event append만 연결하며 worker 실행, scheduler, broker 연동은 다루지 않는다.

## 범위

- `AiService`에 `AiEventOutboxRepository`를 주입한다.
- `saveQueuedJob(...)` 성공 직후 requested outbox event를 append한다.
- system generation job 생성과 admin regenerate 생성 모두 동일 event name을 사용한다.
- event payload에는 job/target/request 식별자와 상태 값만 저장한다.
- `ai-event-contract-fixtures.json`의 `AiGenerationJobRequested` required payload를 generic target 기반으로 동기화한다.
- workflow 문서와 report 문서를 작성한다.

## 제외 범위

- worker 실행 또는 scheduler 구현
- Kafka, topic, producer, consumer, relay worker 구현
- provider live endpoint 호출
- LLM executor 구현
- `AiGenerationJobStarted/Completed/Failed` 동작 변경
- DB migration 변경
- gateway route 활성화

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiService.java` | job 생성 transaction 안에서 requested outbox append |
| Modify | `qtai-server/ai-service/src/test/resources/contracts/ai-events/ai-event-contract-fixtures.json` | requested event required payload 동기화 |
| Test | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiServiceSystemUseCasePersistenceTest.java` | system job 생성 requested outbox 검증 |
| Test | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiServiceAssetReviewPersistenceTest.java` | admin regenerate requested outbox 검증 |
| Test | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/event/AiEventContractFixtureTest.java` | fixture 계약 회귀 검증 |
| Create | workflow/report 문서 | 작업 계획과 실행 결과 기록 |

## 구현 순서

1. `dev` 최신화 후 작업 브랜치를 준비한다.
2. workflow 문서를 저장한다.
3. `AiService` 생성자에 `AiEventOutboxRepository`를 추가한다.
4. job 저장 성공 후 `AiGenerationJobRequested` event payload를 생성해 outbox에 저장한다.
5. system 생성과 admin regenerate의 `requestedBy`, `requestSource` 값을 분리한다.
6. fixture required fields와 example payload를 production payload 의미와 맞춘다.
7. system/admin persistence 테스트에 outbox 단언을 추가한다.
8. report 문서를 작성한다.
9. compile/test/diff/금지어 검증 후 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiServiceSystemUseCasePersistenceTest` | system job 생성 시 `AiGenerationJobRequested` outbox와 payload 저장 |
| `AiServiceAssetReviewPersistenceTest` | admin regenerate 시 `AiGenerationJobRequested` outbox와 payload 저장 |
| `AiEventContractFixtureTest` | requested event fixture required field 계약 통과 |
| `AiServiceEventOutboxPersistenceTest` | outbox persistence 회귀 확인 |

## 수용 기준

- [ ] system generation job 생성 시 job과 requested outbox가 함께 저장된다.
- [ ] admin regenerate job 생성 시 requested outbox가 함께 저장된다.
- [ ] duplicate/invalid job 생성 실패 시 requested outbox가 추가되지 않는다.
- [ ] payload에는 `jobId`, `jobType`, `targetType`, `targetId`, `promptVersionId`, `requestedBy`, `requestSource`, `requestedAt`이 포함된다.
- [ ] `targetType=QT_PASSAGE` payload에는 `passageId=targetId` alias가 포함된다.
- [ ] prompt, provider raw response, 본문, 인증 값, DB 접속 값은 payload에 저장하지 않는다.
- [ ] Kafka/topic/worker/scheduler/relay 구현은 추가하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- `AiService` transaction, outbox payload, fixture, 테스트가 같은 계약을 공유한다.
- payload 필드 변경과 fixture 변경을 같은 맥락에서 검증해야 한다.
- 변경 범위가 ai-service 내부 usecase와 테스트에 집중되어 직접 실행이 안전하다.

### 위임 가능한 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow, implementation, 테스트, report를 직접 작성하고 검증한다.

## 검증 계획

- `cd qtai-server`
- `.\gradlew.bat :ai-service:compileJava`
- `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiServiceSystemUseCasePersistenceTest --tests com.qtai.domain.ai.internal.AiServiceAssetReviewPersistenceTest --tests com.qtai.domain.ai.event.AiEventContractFixtureTest --tests com.qtai.domain.ai.internal.AiServiceEventOutboxPersistenceTest`
- `cd ..`
- `git diff --check`
- `rg -n "DeepSeek|LlmClient|external\\.llm|Kafka|topic" "qtai-server\ai-service\src\main\java"`
- `rg -n "개역개정|ESV|NIV|성서유니온|두란노|plain secret|private key" "qtai-server\ai-service" "doc\workspaces\DevC_강상민"`

## 후속 작업으로 남길 항목

- generation worker scheduler opt-in
- production LLM executor 구현
- outbox relay worker와 broker 연결
- provider input event ingestion 전환 검토
