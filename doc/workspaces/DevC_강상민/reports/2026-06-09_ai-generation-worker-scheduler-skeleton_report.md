# Report - 2026-06-09 ai-generation-worker-scheduler-skeleton

## 작업 요약

ai-service generation worker를 주기 실행할 수 있는 scheduler skeleton을 추가했다. 기본값은 비활성 상태이며, persistence, generation worker, scheduler flag가 모두 켜진 경우에만 scheduler bean이 등록된다.

이번 변경은 `AiGenerationWorkerService.runBatch()` 호출 준비까지만 포함한다. 실제 LLM executor, Kafka, relay worker, provider live 호출, 운영 scheduler 활성화는 수행하지 않았다.

## 변경 내용

- `AiServiceWorkerSchedulerConfiguration`를 추가해 scheduler opt-in 조건과 `@EnableScheduling` 범위를 분리했다.
- `AiGenerationWorkerScheduler`를 추가해 scheduled tick마다 `runBatch()`를 호출하게 했다.
- scheduler 실행 중 예외는 밖으로 전파하지 않고 민감 값 없는 error type 로그만 남기게 했다.
- `application.yml`에 scheduler enabled/fixed-delay 설정을 추가했다.
- disabled/enabled context test와 scheduler unit test를 추가했다.

## 설정

| property | 기본값 | 설명 |
| --- | --- | --- |
| `qtai.ai.worker.generation.scheduler.enabled` | `false` | generation worker scheduler 활성화 여부 |
| `qtai.ai.worker.generation.scheduler.fixed-delay-ms` | `30000` | scheduled batch 실행 간격 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 통과 |
| `.\gradlew.bat :ai-service:test --tests com.qtai.ai.AiGenerationWorkerSchedulerDisabledContextTest --tests com.qtai.ai.AiGenerationWorkerSchedulerEnabledContextTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerSchedulerTest --tests com.qtai.ai.AiGenerationWorkerEnabledContextTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerSkeletonTest` | 통과 |
| `git diff --check` | 통과 |
| ai-service main source의 LLM/Kafka 관련 구현 검색 | 매칭 없음 |
| 변경 범위 금지 데이터/민감 예시 문구 검색 | 통과 |

## 수용 기준 확인

- 기본 property에서 scheduler bean이 등록되지 않는다.
- scheduler flag만 켠 상태에서는 persistence/worker 조건이 충족되지 않아 scheduler bean이 등록되지 않는다.
- persistence, generation worker, scheduler flag가 모두 켜지고 executor bean이 있으면 scheduler bean이 등록된다.
- executor bean이 없으면 worker enabled context가 fail-fast 한다.
- scheduler tick은 `runBatch()`를 호출한다.
- `runBatch()` 예외는 scheduler 밖으로 전파되지 않는다.

## 제외 범위 확인

- production LLM executor를 구현하지 않았다.
- Kafka, topic, producer, consumer, relay worker를 추가하지 않았다.
- provider live endpoint를 호출하지 않았다.
- gateway route를 변경하지 않았다.
- 운영 DB migration을 변경하지 않았다.
- monolith AI 코드를 삭제하지 않았다.
- 분산락 의존성을 추가하지 않았다.

## 후속 작업

- production `AiGenerationWorkerExecutor` 구현
- scheduler 운영 활성화 전 runtime smoke 보강
- outbox relay worker 설계 승인 후 구현
