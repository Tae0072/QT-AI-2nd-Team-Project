# Report - 2026-06-09 ai-generation-real-executor-runtime-toggle

## 작업 요약

generation worker가 설정에 따라 테스트 전용 fake executor와 real skeleton executor를 분리해서 사용하는지 검증했다. production fake executor는 추가하지 않았고, 실제 DeepSeek/LLM 호출도 구현하지 않았다.

## 변경 내용

- `AiGenerationRealExecutorRuntimeToggleTest`를 추가했다.
- workflow 문서 `2026-06-09_ai-generation-real-executor-runtime-toggle.md`를 추가했다.
- production code는 변경하지 않았다.

## 테스트 위치 보정

계획 초안은 `com.qtai.ai` 테스트 패키지를 사용했지만, worker 실행 결과 검증에는 package-private AI persistence 모델 접근이 필요하다. 기존 worker runtime smoke 테스트와 같은 기준으로 `com.qtai.domain.ai.internal` 패키지에 테스트를 배치했다.

## 검증한 runtime toggle

| 조건 | 검증 결과 |
| --- | --- |
| 기본 `executor.mode` + test fake executor | fake executor 사용, real skeleton 미등록, worker 성공 경로 통과 |
| explicit `executor.mode=none` + test fake executor | fake executor 사용, real skeleton 미등록, worker 성공 경로 통과 |
| `executor.mode=deepseek` + deepseek 설정 완비 | real skeleton executor 등록, fake executor 미등록 |
| deepseek mode `runOnce()` | 실제 호출 없이 skeleton 미구현 예외가 worker 실패 경로로 기록 |
| deepseek mode 필수 설정 누락 | context fail-fast 유지 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 통과 |
| `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiGenerationRealExecutorRuntimeToggleTest --tests com.qtai.ai.AiGenerationRealExecutorConfigurationTest --tests com.qtai.domain.ai.internal.DeepSeekGenerationWorkerExecutorSkeletonTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerRuntimeSmokeReadinessTest` | 통과 |
| `git diff --check` | 통과 |
| LLM 구현 marker 검색 | 매칭 없음 |
| workflow placeholder 검색 | 매칭 없음 |
| 변경 범위 금지 데이터/민감 예시 문구 검색 | 통과 |

## 수용 기준 확인

- 기본 mode와 explicit `none` mode에서 `DeepSeekGenerationWorkerExecutor`가 등록되지 않는다.
- 기본/none mode에서 테스트 전용 fake executor로 worker 성공 경로가 동작한다.
- `mode=deepseek`에서 fake executor 없이 `DeepSeekGenerationWorkerExecutor`가 등록된다.
- deepseek mode의 `runOnce()`는 skeleton 미구현 예외를 worker 실패 경로로 기록한다.
- deepseek 필수 설정 누락 시 context fail-fast가 유지된다.
- production code 변경 없이 테스트만으로 전환 조건을 검증했다.

## 제외 범위 확인

- production fake executor 추가 없음
- 실제 DeepSeek/LLM HTTP 호출 없음
- prompt/context 조립 없음
- provider live 조회 없음
- Kafka, topic, relay, producer, consumer 구현 없음
- scheduler 운영 활성화 없음
- gateway route, 운영 DB migration, monolith AI 코드 변경 없음

## 후속 작업

- 실제 DeepSeek HTTP adapter 구현
- prompt/context contract 설계
- executor 운영 smoke 확장
