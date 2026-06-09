# ai-generation-worker-skeleton Report

## 작업 요약

- `feature/ai-generation-worker-skeleton` 브랜치에서 ai-service generation worker skeleton을 추가했다.
- worker는 기본값에서 비활성 상태이며 `qtai.ai.worker.generation.enabled=true`와 `qtai.ai.persistence.enabled=true`가 모두 켜질 때만 등록된다.
- production executor 구현은 추가하지 않았다. worker enabled 상태에서 `AiGenerationWorkerExecutor` bean이 없으면 context가 fail-fast 된다.
- worker는 DB polling으로 `QUEUED` job을 claim하고 상태 전이와 outbox append만 수행한다.

## 변경 내용

- `AiServiceWorkerConfiguration`과 generation worker 설정 binding을 추가했다.
- `application.yml`에 generation worker 설정을 추가했다.
  - `qtai.ai.worker.generation.enabled=${QTAI_AI_GENERATION_WORKER_ENABLED:false}`
  - `qtai.ai.worker.generation.batch-size=${QTAI_AI_GENERATION_WORKER_BATCH_SIZE:1}`
- `AiGenerationWorkerService`를 추가했다.
  - `runOnce()`
  - `runBatch()`
  - `QUEUED -> RUNNING -> SUCCEEDED/FAILED`
  - `AiGenerationJobStarted`, `AiGenerationJobCompleted`, `AiGenerationJobFailed` outbox 저장
- `AiGenerationWorkerExecutor`와 중첩 입력/결과 record 계약을 추가했다.
- context toggle 및 worker persistence skeleton 테스트를 추가했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 성공 |
| `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiGenerationWorkerSkeletonTest --tests com.qtai.ai.AiGenerationWorkerDisabledContextTest --tests com.qtai.ai.AiGenerationWorkerEnabledContextTest --tests com.qtai.domain.ai.internal.AiServiceEventOutboxPersistenceTest` | 성공 |

## 수용 기준 확인

- 기본 설정에서 worker bean이 등록되지 않음을 확인했다.
- worker enabled + persistence enabled + fake executor 조건에서 worker bean이 등록됨을 확인했다.
- worker enabled 상태에서 executor bean이 없으면 context가 실패함을 확인했다.
- queued job이 없을 때 `runOnce()`가 `false`를 반환함을 확인했다.
- success path에서 job이 `SUCCEEDED`가 되고 asset과 Started/Completed outbox가 저장됨을 확인했다.
- failure path에서 job이 `FAILED`가 되고 Failed outbox가 저장되며 executor 예외가 외부로 전파되지 않음을 확인했다.
- 이미 `RUNNING` 상태인 job은 `findByIdAndStatus(QUEUED)` 기준으로 skip됨을 확인했다.
- `runBatch()`가 설정된 batch size까지만 처리함을 확인했다.

## 제외 범위 준수

- DeepSeek/LLM production executor 구현은 추가하지 않았다.
- Kafka, topic, consumer, producer, scheduler, relay worker 구현은 추가하지 않았다.
- provider live endpoint 호출은 추가하지 않았다.
- provider QT/Bible 조회 전환은 하지 않았다.
- gateway route 활성화와 monolith AI 삭제는 하지 않았다.
- `CreateAiGenerationJobUseCase`의 `AiGenerationJobRequested` outbox append 연결은 후속 작업으로 남겼다.

## 후속 작업

- `ai-generation-job-requested-outbox-append`
- production LLM executor 설계 및 opt-in 구현
- generation worker scheduler opt-in
- outbox relay worker와 broker 연결
