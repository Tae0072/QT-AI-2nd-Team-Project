# Workflow - 2026-06-09 ai-generation-worker-skeleton

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-generation-worker-skeleton` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | `ai-generation-worker-design` 이후 긴 AI generation 흐름을 worker 경계로 분리하기 위한 skeleton 구현 |
| 기준 문서 | `2026-06-09_ai-generation-worker-design.md`, `2026-06-09_ai-event-outbox-decision-record.md`, `2026-06-09_ai-event-outbox-skeleton.md` |
| 해당 경로 | `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

ai-service에 opt-in generation worker skeleton을 추가한다. worker는 실제 DeepSeek/LLM 호출 없이 테스트용 executor 계약만 받고, DB polling으로 `QUEUED` job을 claim한 뒤 상태 전이와 outbox event append를 검증 가능한 수준으로 제공한다.

## 범위

- `qtai.ai.worker.generation.enabled=false` 기본값을 유지한다.
- `qtai.ai.worker.generation.enabled=true`와 `qtai.ai.persistence.enabled=true`일 때만 worker bean을 등록한다.
- worker class는 `@Service`로 등록하지 않고 `AiServiceWorkerConfiguration`에서만 bean으로 생성한다.
- `runOnce()`와 `runBatch()`를 제공한다.
- `QUEUED -> RUNNING -> SUCCEEDED/FAILED` 상태 전이를 persistence transaction으로 검증한다.
- `AiGenerationJobStarted`, `AiGenerationJobCompleted`, `AiGenerationJobFailed` outbox event를 저장한다.
- event payload에는 job/asset/result/failure 식별자와 상태 값만 저장한다.
- workflow 문서와 report 문서를 작성한다.

## 제외 범위

- DeepSeek/LLM production executor 구현
- Kafka, topic, producer, consumer 구현
- scheduler, relay worker 구현
- provider live endpoint 호출
- provider QT/Bible 조회 전환
- gateway route 활성화
- monolith AI worker 삭제
- `CreateAiGenerationJobUseCase`의 `AiGenerationJobRequested` outbox append 연결

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceWorkerConfiguration.java` | opt-in worker bean 등록과 generation worker batch 설정 |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceApplication.java` | worker configuration import |
| Modify | `qtai-server/ai-service/src/main/resources/application.yml` | disabled-by-default worker 설정 |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiGenerationWorkerService.java` | claim, executor 호출, success/failure 상태 전이와 outbox append |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiGenerationWorkerExecutor.java` | production 구현 없는 executor 계약과 입력/결과 record |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/ai/AiGenerationWorkerDisabledContextTest.java` | 기본 worker 미등록 검증 |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/ai/AiGenerationWorkerEnabledContextTest.java` | opt-in 등록과 executor 누락 fail-fast 검증 |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiGenerationWorkerSkeletonTest.java` | 상태 전이와 outbox append 검증 |
| Create | workflow/report 문서 | 작업 계획과 실행 결과 기록 |

## 구현 순서

1. `dev` 최신화 후 `feature/ai-generation-worker-skeleton` 브랜치에서 시작한다.
2. workflow 문서를 저장한다.
3. worker 설정 property와 `AiServiceWorkerConfiguration`을 추가한다.
4. worker executor/job/result 계약과 `AiGenerationWorkerService`를 추가한다.
5. application 설정과 import를 연결한다.
6. context toggle 테스트를 추가한다.
7. worker persistence skeleton 테스트를 추가한다.
8. report 문서를 작성한다.
9. compile/test/diff/금지어 검증을 실행한다.
10. 지정 커밋 메시지로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiGenerationWorkerDisabledContextTest` | 기본 property에서 worker bean 미등록 |
| `AiGenerationWorkerEnabledContextTest` | worker enabled + persistence enabled + fake executor에서 worker bean 등록, executor 누락 시 context fail-fast |
| `AiGenerationWorkerSkeletonTest` | queued job 없음, success path, failure path, duplicate/concurrent claim skip |
| `AiServiceEventOutboxPersistenceTest` | 기존 outbox persistence 회귀 확인 |

## 수용 기준

- [ ] 기본 설정에서 worker bean이 등록되지 않는다.
- [ ] worker enabled + persistence enabled + executor bean 조건에서만 worker가 등록된다.
- [ ] worker enabled 상태에서 executor bean이 없으면 context가 실패한다.
- [ ] `runOnce()`가 queued job이 없으면 `false`를 반환한다.
- [ ] success path에서 job이 `SUCCEEDED`가 되고 asset과 Started/Completed outbox가 저장된다.
- [ ] failure path에서 job이 `FAILED`가 되고 Failed outbox가 저장되며 executor 예외가 외부로 전파되지 않는다.
- [ ] duplicate/concurrent claim은 `findByIdAndStatus(QUEUED)` 기준으로 처리하지 않는다.
- [ ] DeepSeek/LLM/Kafka/topic/scheduler/relay worker 구현이 추가되지 않는다.
- [ ] event payload에는 prompt, provider raw response, 본문, 인증 값, DB 접속 값이 저장되지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- worker 상태 전이, outbox payload, test fixture가 같은 transaction 경계를 공유한다.
- `@Service` scan 회피와 configuration 조건 검증이 implementation과 test에 동시에 영향을 준다.
- 변경 범위가 ai-service 내부에 집중되어 직접 실행이 충돌 가능성을 줄인다.

### 위임 가능한 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow, worker skeleton, 테스트, report를 직접 작성하고 검증한다.

## 검증 계획

- `cd qtai-server`
- `.\gradlew.bat :ai-service:compileJava`
- `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiGenerationWorkerSkeletonTest --tests com.qtai.ai.AiGenerationWorkerDisabledContextTest --tests com.qtai.ai.AiGenerationWorkerEnabledContextTest --tests com.qtai.domain.ai.internal.AiServiceEventOutboxPersistenceTest`
- `cd ..`
- `git diff --check`
- `rg -n "DeepSeek|LlmClient|external\\.llm|Kafka|topic" "qtai-server\ai-service\src\main\java"`
- workflow/report placeholder 검색
- 변경 파일 기준 금지 데이터와 민감 예시 문구 검색

## 후속 작업으로 남길 항목

- `AiGenerationJobRequested` outbox append 연결
- production LLM executor 구현
- generation worker scheduler opt-in
- outbox relay worker와 broker 연결
- provider input event ingestion 전환 검토
