# Report - 2026-06-09 ai-generation-real-executor-skeleton

## 작업 요약

ai-service generation worker에 실제 executor를 연결할 수 있는 skeleton 경계를 추가했다. 기본값은 `none`으로 production executor를 등록하지 않고, `qtai.ai.worker.generation.executor.mode=deepseek`일 때만 DeepSeek executor skeleton bean을 등록한다.

이번 작업은 실제 LLM/provider 호출을 구현하지 않고, 설정 fail-fast와 worker bean 연결 가능성만 검증한다.

## 변경 내용

- `AiServiceGenerationExecutorConfiguration`을 추가해 executor mode 기반 bean 등록을 분리했다.
- `DeepSeekGenerationWorkerExecutor` skeleton을 추가했다.
- `application.yml`에 executor mode와 deepseek 설정 placeholder를 추가했다.
- `AiServiceApplication`에서 executor configuration을 import했다.
- `AiGenerationRealExecutorConfigurationTest`와 `DeepSeekGenerationWorkerExecutorSkeletonTest`를 추가했다.
- workflow 문서 `2026-06-09_ai-generation-real-executor-skeleton.md`를 추가했다.

## 동작 기준

| 조건 | 결과 |
| --- | --- |
| `qtai.ai.worker.generation.executor.mode` 미지정 또는 `none` | production executor bean 미등록 |
| `mode=deepseek` + 필수 설정 완비 | `DeepSeekGenerationWorkerExecutor` bean 등록 |
| `mode=deepseek` + 필수 설정 누락 | context fail-fast |
| `mode=deepseek` + worker/persistence enabled | `AiGenerationWorkerService` 생성 가능 |
| skeleton executor `execute(...)` 호출 | 실제 호출 없이 안전한 미구현 예외 발생 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 통과 |
| `.\gradlew.bat :ai-service:test --tests com.qtai.ai.AiGenerationRealExecutorConfigurationTest --tests com.qtai.domain.ai.internal.DeepSeekGenerationWorkerExecutorSkeletonTest --tests com.qtai.ai.AiGenerationWorkerEnabledContextTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerRuntimeSmokeReadinessTest` | 통과 |
| `git diff --check` | 통과 |
| `rg -n "chat/completions|LlmCompletionRequest|LlmCompletionResponse|ExplanationGenerationJobHandler" "qtai-server\ai-service\src\main\java"` | 매칭 없음 |
| executor 신규 파일 대상 `RestTemplate|WebClient` 검색 | 매칭 없음 |
| 변경 범위 금지 데이터/민감 예시 문구 검색 | 통과 |

## 수용 기준 확인

- 기본 mode에서 production `AiGenerationWorkerExecutor` bean이 등록되지 않는다.
- `mode=deepseek`와 필수 설정 완비 시 skeleton executor가 등록된다.
- `mode=deepseek`에서 base-url, api-key, model 누락과 timeout 비정상 값이 fail-fast 된다.
- worker+persistence enabled 상태에서 skeleton executor로 `AiGenerationWorkerService`가 생성된다.
- skeleton executor는 실제 HTTP 호출 없이 안전한 미구현 예외만 던진다.
- monolith LLM 구현, Kafka, provider live 호출, scheduler 운영 활성화는 추가하지 않았다.

## 제외 범위 확인

- 실제 DeepSeek/LLM HTTP 호출 없음
- prompt/context 조립 없음
- provider live 조회 없음
- Kafka, topic, relay, producer, consumer 구현 없음
- scheduler 운영 활성화 없음
- gateway route, 운영 DB migration, monolith AI 코드 변경 없음

## 후속 작업

- 실제 DeepSeek HTTP adapter 구현
- generation prompt/context contract 설계
- provider context 조회 연결
- executor 운영 smoke 확장
