# Workflow - 2026-06-09 ai-generation-real-executor-runtime-toggle

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-generation-real-executor-runtime-toggle` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `test/ai-generation-real-executor-runtime-toggle` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | real executor skeleton 이후 fake executor와 real skeleton executor가 설정으로 안전하게 전환되는지 검증한다. |
| 기준 문서 | `2026-06-09_ai-generation-real-executor-skeleton.md`, `2026-06-09_ai-generation-worker-runtime-smoke-readiness.md` |
| 대상 경로 | `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

generation worker가 테스트 전용 fake executor와 real skeleton executor를 설정에 따라 분리해서 사용하는지 검증한다. production fake executor는 만들지 않고, 실제 DeepSeek/LLM 호출도 구현하지 않는다.

## 범위

- runtime toggle 테스트를 추가한다.
- 기본 `executor.mode` 미지정 또는 `none` 상태에서는 테스트 전용 fake executor bean만 사용되는지 검증한다.
- `executor.mode=deepseek` 상태에서는 `DeepSeekGenerationWorkerExecutor`가 worker에 연결되는지 검증한다.
- fake mode 성공 경로와 deepseek skeleton 실패 경로를 H2 persistence로 확인한다.
- deepseek 필수 설정 누락 fail-fast 회귀를 확인한다.

## 제외 범위

- production fake executor 추가
- 실제 DeepSeek/LLM HTTP 호출
- prompt/context 조립
- provider live 조회
- Kafka, topic, relay, producer, consumer 구현
- scheduler 운영 활성화
- gateway route, 운영 DB migration, monolith AI 코드 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiGenerationRealExecutorRuntimeToggleTest.java` | fake/real skeleton executor runtime toggle 검증 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-generation-real-executor-runtime-toggle.md` | 작업 기준 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-generation-real-executor-runtime-toggle_report.md` | 검증 결과 |

## 구현 순서

1. `dev` 최신화 후 선행 real executor skeleton 파일이 있는지 확인한다.
2. `test/ai-generation-real-executor-runtime-toggle` 브랜치를 만든다.
3. workflow 문서를 작성한다.
4. `AiGenerationRealExecutorRuntimeToggleTest`를 추가한다.
5. fake mode 성공 경로, deepseek mode 실패 경로, deepseek fail-fast 경로를 검증한다.
6. compile/test/guard 검증을 실행한다.
7. report 문서를 작성한다.
8. 지정 파일만 stage 후 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiGenerationRealExecutorRuntimeToggleTest` | 기본/none mode fake executor 사용, deepseek mode skeleton executor 사용, persistence 결과 검증 |
| `AiGenerationRealExecutorConfigurationTest` | 기존 real executor 설정 fail-fast 회귀 |
| `DeepSeekGenerationWorkerExecutorSkeletonTest` | skeleton executor 미구현 예외 회귀 |
| `AiGenerationWorkerRuntimeSmokeReadinessTest` | fake executor runtime smoke 회귀 |

## 수용 기준

- [ ] 기본 mode와 explicit `none` mode에서 `DeepSeekGenerationWorkerExecutor`가 등록되지 않는다.
- [ ] 기본/none mode에서 테스트 전용 fake executor로 worker 성공 경로가 동작한다.
- [ ] `mode=deepseek`에서 fake executor 없이 `DeepSeekGenerationWorkerExecutor`가 등록된다.
- [ ] deepseek mode의 `runOnce()`는 skeleton 미구현 예외를 worker 실패 경로로 기록한다.
- [ ] deepseek 필수 설정 누락 시 context fail-fast가 유지된다.
- [ ] production code 변경 없이 테스트만으로 전환 조건을 검증한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- runtime toggle 테스트는 Spring context 조건, H2 persistence, worker 실행 결과가 한 흐름에 묶여 있다.
- production code 변경 없이 테스트와 문서만 추가하므로 병렬화 이득이 작다.
- 직접 실행이 fake/real bean 충돌 여부를 가장 명확하게 확인할 수 있다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 테스트 추가, 검증, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiGenerationRealExecutorRuntimeToggleTest --tests com.qtai.ai.AiGenerationRealExecutorConfigurationTest --tests com.qtai.domain.ai.internal.DeepSeekGenerationWorkerExecutorSkeletonTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerRuntimeSmokeReadinessTest
cd ..
git diff --check
rg -n "chat/completions|LlmCompletionRequest|LlmCompletionResponse|ExplanationGenerationJobHandler" "qtai-server\ai-service\src\main\java"
$placeholderPattern = ("TB" + "D") + "|" + ("TO" + "DO") + "|" + ("추후 " + "정리") + "|" + ("나중에 " + "정리") + "|" + ("미" + "정")
rg -n $placeholderPattern "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-generation-real-executor-runtime-toggle.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-generation-real-executor-runtime-toggle_report.md"
$forbiddenPattern = ("개역" + "개정") + "|" + ("E" + "SV") + "|" + ("N" + "IV") + "|" + ("성서" + "유니온") + "|" + ("두" + "란노") + "|" + ("plain " + "secret") + "|" + ("private " + "key")
rg -n $forbiddenPattern "qtai-server\ai-service" "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-generation-real-executor-runtime-toggle.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-generation-real-executor-runtime-toggle_report.md"
```

## 다음 작업으로 남길 항목

- 실제 DeepSeek HTTP adapter 구현
- prompt/context contract 설계
- executor 운영 smoke 확장
