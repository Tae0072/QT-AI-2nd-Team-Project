# Workflow - 2026-06-09 ai-generation-worker-runtime-smoke-readiness

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-generation-worker-runtime-smoke-readiness` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `test/ai-generation-worker-runtime-smoke-readiness` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | generation worker skeleton, scheduler skeleton, executor contract가 준비되어 실제 LLM 연결 전 worker runtime smoke를 고정해야 한다. |
| 기준 문서 | `2026-06-09_ai-generation-worker-design.md`, `2026-06-09_ai-generation-worker-executor-contract_report.md` |
| 대상 경로 | `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

ai-service 독립 컨텍스트에서 generation worker가 H2와 fake executor만으로 실행 가능한지 확인하는 opt-in runtime smoke를 추가한다. queued job 생성부터 `runOnce()`, `runBatch()`, asset 저장, Started/Completed/Failed outbox 저장까지 검증한다.

이번 작업은 smoke readiness 전용이다. 실제 LLM, provider live endpoint, Kafka/relay, scheduler 운영 실행은 포함하지 않는다.

## 범위

- worker runtime smoke 테스트를 추가한다.
- smoke wrapper script를 추가한다.
- workflow와 report 문서를 작성한다.
- smoke는 `qtai.ai.persistence.enabled=true`, `qtai.ai.worker.generation.enabled=true`, `qtai.ai.worker.generation.scheduler.enabled=false`, `qtai.ai.client.mode=mock` 조건으로 실행한다.
- H2 DB와 fake executor만 사용한다.

## 제외 범위

- production `AiGenerationWorkerExecutor` 구현
- DeepSeek/LLM 호출
- provider live endpoint 호출
- Kafka, topic, producer, consumer, relay worker 구현
- scheduler runtime 활성화
- gateway route 변경
- 운영 DB migration 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiGenerationWorkerRuntimeSmokeReadinessTest.java` | worker runtime smoke |
| Create | `qtai-server/ai-service/scripts/generation-worker-runtime-smoke-readiness.ps1` | smoke 실행 wrapper |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-generation-worker-runtime-smoke-readiness.md` | 작업 기준 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-generation-worker-runtime-smoke-readiness_report.md` | 검증 결과 |

## 구현 순서

1. `dev` 최신화 후 선행 `AiGenerationWorkerExecutorContractTest` 존재를 확인한다.
2. `test/ai-generation-worker-runtime-smoke-readiness` 브랜치를 생성한다.
3. workflow 문서를 작성한다.
4. worker runtime smoke 테스트를 추가한다.
5. smoke wrapper script를 추가한다.
6. compile/test/script/guard 검증을 실행한다.
7. report 문서를 작성한다.
8. 지정 파일만 stage 후 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiGenerationWorkerRuntimeSmokeReadinessTest` | worker bean/fake executor 등록, scheduler 미등록, success/failure path, batch-size smoke |
| `AiGenerationWorkerSkeletonTest` | 기존 worker 상태 전이 회귀 |
| `AiGenerationWorkerExecutorContractTest` | executor 계약 회귀 |

## 수용 기준

- [ ] worker bean과 fake executor bean이 등록된다.
- [ ] scheduler bean은 등록되지 않는다.
- [ ] queued job을 저장하고 `runOnce()` 성공 path가 동작한다.
- [ ] 성공 시 `ai_generated_assets`와 Started/Completed outbox가 저장된다.
- [ ] `runBatch()`는 batch-size 범위만 처리한다.
- [ ] fake executor failure 시 job은 `FAILED`, asset은 미저장, Failed outbox가 저장된다.
- [ ] wrapper는 provider/LLM/network 호출 없이 targeted test만 실행한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- smoke test, fake executor, wrapper, report가 같은 실행 조건을 공유한다.
- 내부 repository/entity 접근이 필요한 테스트라 단일 흐름으로 검증하는 편이 안전하다.
- 변경 범위가 worker runtime smoke와 문서로 제한된다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, smoke test/script 추가, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiGenerationWorkerRuntimeSmokeReadinessTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerSkeletonTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerExecutorContractTest
powershell -ExecutionPolicy Bypass -File .\ai-service\scripts\generation-worker-runtime-smoke-readiness.ps1
cd ..
git diff --check
rg -n "DeepSeek|LlmClient|external\.llm|Kafka|topic" "qtai-server\ai-service\src\main\java"
$forbiddenPattern = ("개역" + "개정") + "|" + ("E" + "SV") + "|" + ("N" + "IV") + "|" + ("성서" + "유니온") + "|" + ("두" + "란노") + "|" + ("plain " + "secret") + "|" + ("private " + "key")
rg -n $forbiddenPattern "qtai-server\ai-service" "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-generation-worker-runtime-smoke-readiness.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-generation-worker-runtime-smoke-readiness_report.md"
```

## 다음 작업으로 넘길 항목

- production `AiGenerationWorkerExecutor` 구현
- generation worker 운영 smoke 확장
- outbox relay worker 설계 및 skeleton
