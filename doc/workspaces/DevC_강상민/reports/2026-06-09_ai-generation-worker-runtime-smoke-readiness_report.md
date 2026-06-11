# Report - 2026-06-09 ai-generation-worker-runtime-smoke-readiness

## 작업 요약

generation worker를 실제 LLM/provider 호출 없이 H2와 fake executor로 실행하는 runtime smoke readiness를 추가했다. smoke는 worker bean 등록, scheduler 비활성 상태, queued job 처리, asset 저장, outbox 저장, 실패 흐름을 확인한다.

## 변경 내용

- `AiGenerationWorkerRuntimeSmokeReadinessTest`를 추가했다.
- `generation-worker-runtime-smoke-readiness.ps1` wrapper를 추가했다.
- workflow 문서 `2026-06-09_ai-generation-worker-runtime-smoke-readiness.md`를 추가했다.
- report 문서 `2026-06-09_ai-generation-worker-runtime-smoke-readiness_report.md`를 추가했다.

## smoke 실행 조건

| property | 값 |
| --- | --- |
| `qtai.ai.persistence.enabled` | `true` |
| `qtai.ai.worker.generation.enabled` | `true` |
| `qtai.ai.worker.generation.scheduler.enabled` | `false` |
| `qtai.ai.worker.generation.batch-size` | `2` |
| `qtai.ai.client.mode` | `mock` |
| DB | H2 in-memory |
| executor | test fake executor |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 통과 |
| `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiGenerationWorkerRuntimeSmokeReadinessTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerSkeletonTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerExecutorContractTest` | 통과 |
| `powershell -ExecutionPolicy Bypass -File .\ai-service\scripts\generation-worker-runtime-smoke-readiness.ps1` | 통과 |
| `git diff --check` | 통과 |
| ai-service main source의 LLM/Kafka 관련 구현 검색 | 매칭 없음 |
| 변경 범위 금지 데이터/민감 예시 문구 검색 | 통과 |

## 수용 기준 확인

- worker bean과 fake executor bean이 등록된다.
- scheduler bean은 등록되지 않는다.
- queued job을 저장하고 `runOnce()` 성공 path가 동작한다.
- 성공 시 `ai_generated_assets`와 Started/Completed outbox가 저장된다.
- `runBatch()`는 batch-size 범위만 처리한다.
- fake executor failure 시 job은 `FAILED`, asset은 미저장, Failed outbox가 저장된다.
- wrapper는 provider/LLM/network 호출 없이 targeted test만 실행한다.

## 제외 범위 확인

- production `AiGenerationWorkerExecutor`를 구현하지 않았다.
- DeepSeek/LLM 호출을 수행하지 않았다.
- provider live endpoint를 호출하지 않았다.
- Kafka, topic, producer, consumer, relay worker를 추가하지 않았다.
- scheduler runtime을 활성화하지 않았다.
- gateway route와 운영 DB migration을 변경하지 않았다.

## 후속 작업

- production `AiGenerationWorkerExecutor` 구현
- generation worker 운영 smoke 확장
- outbox relay worker 설계 및 skeleton
