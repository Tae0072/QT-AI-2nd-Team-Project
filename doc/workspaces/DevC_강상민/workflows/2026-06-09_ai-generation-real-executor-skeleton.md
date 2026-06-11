# Workflow - 2026-06-09 ai-generation-real-executor-skeleton

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-generation-real-executor-skeleton` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `feature/ai-generation-real-executor-skeleton` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | generation worker runtime smoke 이후 실제 generation executor를 붙일 구성 경계를 먼저 고정한다. |
| 기준 문서 | `2026-06-09_ai-generation-worker-design.md`, `2026-06-09_ai-generation-worker-executor-contract.md` |
| 대상 경로 | `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

ai-service에 production generation executor를 연결할 수 있는 설정과 bean 경계를 추가한다. 이번 작업은 skeleton 전용이며 실제 DeepSeek/LLM HTTP 호출, prompt 조립, provider live 조회, scheduler 운영 활성화는 구현하지 않는다.

## 범위

- `qtai.ai.worker.generation.executor.mode=none|deepseek` 설정을 추가한다.
- 기본값 `none`에서는 production executor bean을 등록하지 않는다.
- `mode=deepseek`일 때만 `DeepSeekGenerationWorkerExecutor` skeleton bean을 등록한다.
- `mode=deepseek`에서 base-url, api-key, model, timeout 설정을 fail-fast로 검증한다.
- skeleton executor는 `execute(...)` 호출 시 실제 호출 없이 안전한 미구현 예외를 던진다.
- worker context가 skeleton executor로 생성될 수 있는지 검증한다.

## 제외 범위

- 실제 DeepSeek/LLM HTTP adapter 구현
- prompt/context 조립
- QT/Bible provider live 조회
- Kafka, topic, relay, producer, consumer 구현
- scheduler 운영 활성화
- gateway route, 운영 DB migration, monolith AI 코드 변경
- monolith `external.llm.*`, `ExplanationGenerationJobHandler` 복사

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceApplication.java` | executor configuration import |
| Modify | `qtai-server/ai-service/src/main/resources/application.yml` | executor mode와 deepseek placeholder 설정 |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceGenerationExecutorConfiguration.java` | mode 기반 skeleton executor bean 등록 |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/DeepSeekGenerationWorkerExecutor.java` | 실제 호출 없는 executor skeleton |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/ai/AiGenerationRealExecutorConfigurationTest.java` | executor configuration 조건 검증 |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/DeepSeekGenerationWorkerExecutorSkeletonTest.java` | skeleton executor 계약 검증 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-generation-real-executor-skeleton.md` | 작업 기준 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-generation-real-executor-skeleton_report.md` | 검증 결과 |

## 구현 순서

1. `dev` 최신화 후 `feature/ai-generation-real-executor-skeleton` 브랜치를 만든다.
2. 선행 runtime smoke readiness 테스트가 `dev`에 있는지 확인한다.
3. workflow 문서를 작성한다.
4. `application.yml`에 executor 설정 placeholder를 추가한다.
5. `AiServiceGenerationExecutorConfiguration`을 추가하고 `AiServiceApplication`에서 import한다.
6. `DeepSeekGenerationWorkerExecutor` skeleton을 추가한다.
7. configuration 조건 테스트와 executor skeleton 테스트를 추가한다.
8. compile/test/guard 검증을 실행한다.
9. report 문서를 작성한다.
10. 지정 파일만 stage 후 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiGenerationRealExecutorConfigurationTest` | 기본값 미등록, deepseek 완비 등록, 필수 설정 누락 fail-fast, worker service 생성 |
| `DeepSeekGenerationWorkerExecutorSkeletonTest` | constructor guard, execute 미구현 예외, 예외 메시지 민감 값 미노출 |
| `AiGenerationWorkerEnabledContextTest` | 기존 worker enabled fail-fast 회귀 |
| `AiGenerationWorkerRuntimeSmokeReadinessTest` | fake executor runtime smoke 회귀 |

## 수용 기준

- [ ] 기본 mode에서 production `AiGenerationWorkerExecutor` bean이 등록되지 않는다.
- [ ] `mode=deepseek`와 필수 설정 완비 시 skeleton executor가 등록된다.
- [ ] `mode=deepseek`에서 base-url, api-key, model 누락과 timeout 비정상 값이 fail-fast 된다.
- [ ] worker+persistence enabled 상태에서 skeleton executor로 `AiGenerationWorkerService`가 생성된다.
- [ ] skeleton executor는 실제 HTTP 호출 없이 안전한 미구현 예외만 던진다.
- [ ] monolith LLM 구현, Kafka, provider live 호출, scheduler 운영 활성화가 추가되지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- configuration, executor skeleton, worker context 테스트가 같은 bean 조건을 공유한다.
- 작은 범위의 code/test/doc 변경이며 병렬화 이득보다 조건 불일치 위험이 크다.
- 직접 실행이 fail-fast 메시지와 테스트 기대값을 일관되게 맞추기 쉽다.

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
.\gradlew.bat :ai-service:test --tests com.qtai.ai.AiGenerationRealExecutorConfigurationTest --tests com.qtai.domain.ai.internal.DeepSeekGenerationWorkerExecutorSkeletonTest --tests com.qtai.ai.AiGenerationWorkerEnabledContextTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerRuntimeSmokeReadinessTest
cd ..
git diff --check
rg -n "chat/completions|LlmCompletionRequest|LlmCompletionResponse|ExplanationGenerationJobHandler" "qtai-server\ai-service\src\main\java"
rg -n "RestTemplate|WebClient" "qtai-server\ai-service\src\main\java\com\qtai\ai\AiServiceGenerationExecutorConfiguration.java" "qtai-server\ai-service\src\main\java\com\qtai\domain\ai\internal\DeepSeekGenerationWorkerExecutor.java"
$forbiddenPattern = ("개역" + "개정") + "|" + ("E" + "SV") + "|" + ("N" + "IV") + "|" + ("성서" + "유니온") + "|" + ("두" + "란노") + "|" + ("plain " + "secret") + "|" + ("private " + "key")
rg -n $forbiddenPattern "qtai-server\ai-service" "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-generation-real-executor-skeleton.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-generation-real-executor-skeleton_report.md"
```

## 다음 작업으로 남길 항목

- 실제 DeepSeek HTTP adapter 구현
- generation prompt/context contract 설계
- provider context 조회 연결
- executor 운영 smoke 확장
