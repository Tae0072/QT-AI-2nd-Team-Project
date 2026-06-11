# Workflow - 2026-06-09 ai-generation-worker-scheduler-skeleton

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-generation-worker-scheduler-skeleton` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `feature/ai-generation-worker-scheduler-skeleton` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | ai-service generation worker skeleton 이후, worker를 수동 호출이 아닌 opt-in 주기 실행 구조로 준비해야 한다. |
| 기준 문서 | `2026-06-09_ai-generation-worker-design.md`, `2026-06-09_ai-event-outbox-decision-record.md` |
| 대상 경로 | `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

ai-service generation worker를 주기 실행할 scheduler skeleton을 추가한다. 기본 실행과 CI에서는 scheduler가 비활성 상태를 유지하고, `qtai.ai.worker.generation.scheduler.enabled=true`를 명시했을 때만 `AiGenerationWorkerService.runBatch()`를 호출할 수 있게 한다.

이번 작업은 scheduler 연결 준비만 다룬다. 실제 LLM executor, Kafka, relay worker, provider live 호출, 운영 scheduler 활성화는 포함하지 않는다.

## 범위

- `AiServiceWorkerSchedulerConfiguration`를 추가하고 `AiServiceApplication`에서 import한다.
- scheduler 설정을 `application.yml`에 추가한다.
- `AiGenerationWorkerScheduler`를 추가해 `runBatch()`를 `@Scheduled`로 호출한다.
- scheduler는 예외를 밖으로 전파하지 않고 민감 값 없는 안전 로그만 남긴다.
- scheduler bean은 persistence, generation worker, scheduler flag가 모두 켜진 경우에만 등록한다.
- scheduler disabled/enabled 조건과 tick 동작을 테스트한다.

## 제외 범위

- production LLM executor 구현
- Kafka, topic, producer, consumer, relay worker 구현
- provider live endpoint 호출
- gateway route 변경
- 운영 DB migration 변경
- monolith AI 코드 삭제
- Redis, ShedLock 같은 분산락 의존성 추가

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceWorkerSchedulerConfiguration.java` | scheduler opt-in configuration |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiGenerationWorkerScheduler.java` | scheduled worker batch 실행 |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceApplication.java` | scheduler configuration import |
| Modify | `qtai-server/ai-service/src/main/resources/application.yml` | scheduler enabled/fixed-delay 기본값 |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/ai/AiGenerationWorkerSchedulerDisabledContextTest.java` | 기본 비활성 조건 검증 |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/ai/AiGenerationWorkerSchedulerEnabledContextTest.java` | enabled 조건과 fail-fast 검증 |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiGenerationWorkerSchedulerTest.java` | scheduler tick 단위 검증 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-generation-worker-scheduler-skeleton.md` | 작업 기준 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-generation-worker-scheduler-skeleton_report.md` | 검증 결과 |

## 구현 순서

1. `dev` 최신화 후 `feature/ai-generation-worker-scheduler-skeleton` 브랜치를 만든다.
2. workflow 문서를 작성한다.
3. `application.yml`에 scheduler opt-in property를 추가한다.
4. `AiServiceWorkerSchedulerConfiguration`와 `AiGenerationWorkerScheduler`를 추가한다.
5. `AiServiceApplication`에 scheduler configuration을 import한다.
6. disabled/enabled context test와 scheduler unit test를 추가한다.
7. 관련 Gradle 테스트와 diff 검증을 실행한다.
8. report 문서를 작성한다.
9. 지정 파일만 stage 후 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiGenerationWorkerSchedulerDisabledContextTest` | 기본값과 scheduler-only enabled 상태에서 scheduler bean 미등록 |
| `AiGenerationWorkerSchedulerEnabledContextTest` | persistence + worker + scheduler enabled 조건에서 scheduler bean 등록, executor 누락 시 fail-fast |
| `AiGenerationWorkerSchedulerTest` | scheduled tick이 `runBatch()` 호출, `runBatch()` 예외 미전파 |
| `AiGenerationWorkerEnabledContextTest` | 기존 worker enabled 조건 회귀 |
| `AiGenerationWorkerSkeletonTest` | 기존 worker 상태 전이 회귀 |

## 수용 기준

- [ ] 기본 property에서 scheduler가 등록되지 않는다.
- [ ] scheduler flag만 켜도 persistence/worker가 꺼져 있으면 scheduler가 등록되지 않는다.
- [ ] persistence, generation worker, scheduler flag가 모두 켜졌을 때 scheduler가 등록된다.
- [ ] executor bean이 없으면 worker enabled context가 기존처럼 fail-fast 한다.
- [ ] scheduler tick은 `runBatch()`를 호출한다.
- [ ] `runBatch()` 예외는 scheduler 밖으로 전파되지 않는다.
- [ ] 실제 LLM, Kafka, provider live 호출, 운영 route 변경이 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- scheduler 조건, worker bean, scheduling 테스트가 같은 실행 경계를 공유한다.
- 설정과 테스트가 강하게 연결되어 병렬 편집보다 직접 실행이 충돌 위험이 낮다.
- 변경 범위가 ai-service worker scheduler와 문서 2개로 제한된다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 구현, 테스트, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test --tests com.qtai.ai.AiGenerationWorkerSchedulerDisabledContextTest --tests com.qtai.ai.AiGenerationWorkerSchedulerEnabledContextTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerSchedulerTest --tests com.qtai.ai.AiGenerationWorkerEnabledContextTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerSkeletonTest
cd ..
git diff --check
rg -n "DeepSeek|LlmClient|external\.llm|Kafka|topic" "qtai-server\ai-service\src\main\java"
$forbiddenPattern = ("개역" + "개정") + "|" + ("E" + "SV") + "|" + ("N" + "IV") + "|" + ("성서" + "유니온") + "|" + ("두" + "란노") + "|" + ("plain " + "secret") + "|" + ("private " + "key")
rg -n $forbiddenPattern "qtai-server\ai-service" "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-generation-worker-scheduler-skeleton.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-generation-worker-scheduler-skeleton_report.md"
```

## 다음 작업으로 넘길 항목

- production `AiGenerationWorkerExecutor` 구현
- scheduler 운영 활성화 전 runtime smoke 보강
- outbox relay worker와 broker 연계 설계 승인 후 구현
