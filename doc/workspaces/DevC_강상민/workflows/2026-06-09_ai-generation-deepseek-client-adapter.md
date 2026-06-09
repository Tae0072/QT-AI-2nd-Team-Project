# Workflow - 2026-06-09 ai-generation-deepseek-client-adapter

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-generation-deepseek-client-adapter` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | AI MSA 스케줄의 다음 작업이 `ai-generation-deepseek-client-adapter`로 지정됨 |
| 기준 문서 | `doc/workspaces/DevC_강상민/2026-06-09_ai-msa-work-schedule.md`, `doc/workspaces/DevC_강상민/2026-06-09_ai-generation-prompt-context-contract.md` |
| 대상 경로 | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/**`, `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`ai-service`에 DeepSeek OpenAI-compatible HTTP 호출 계층을 추가한다. 실제 real executor 구현은 하지 않고, executor가 나중에 사용할 수 있는 `DeepSeekGenerationClient` interface와 HTTP adapter, 계약 테스트만 준비한다.

이번 작업은 prompt/context 계약 이후 단계다. adapter는 HTTP 요청/응답 변환과 실패 매핑까지만 담당하고, prompt 조립과 generated asset 저장은 후속 `ai-generation-real-executor-implementation`에서 처리한다.

## 범위

- `DeepSeekGenerationClient` interface와 request/response DTO를 추가한다.
- `DeepSeekGenerationClientHttpAdapter`를 추가해 `/chat/completions` 요청을 보낼 수 있게 한다.
- `AiServiceGenerationExecutorConfiguration`에서 `mode=deepseek`일 때 DeepSeek client bean을 등록한다.
- `DeepSeekGenerationWorkerExecutor`는 HTTP 설정값을 직접 보유하지 않고 client와 model만 받도록 조정한다.
- adapter 계약 테스트와 runtime configuration 테스트를 보강한다.
- 작업 report와 MSA 스케줄 문서를 현재 상태로 업데이트한다.

## 제외 범위

- 실제 real executor 구현
- prompt/context 조립
- provider live 조회
- scheduler 운영 활성화
- Kafka, topic, relay, consumer 구현
- monolith `external.llm` 코드 복사
- prompt 본문, provider raw response, 본문 원문, 인증 값, DB 접속 값 저장

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/client/deepseek/DeepSeekGenerationClient.java` | DeepSeek client interface와 DTO 계약 |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/client/deepseek/DeepSeekGenerationClientHttpAdapter.java` | OpenAI-compatible HTTP adapter |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/client/deepseek/DeepSeekGenerationClientHttpAdapterTest.java` | request/response/error mapping 계약 테스트 |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceGenerationExecutorConfiguration.java` | deepseek mode bean wiring |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/DeepSeekGenerationWorkerExecutor.java` | executor가 client/model만 의존하도록 조정 |
| Modify | `qtai-server/ai-service/src/test/java/com/qtai/ai/AiGenerationRealExecutorConfigurationTest.java` | client bean 등록과 fail-fast 검증 |
| Modify | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/DeepSeekGenerationWorkerExecutorSkeletonTest.java` | executor 생성자 계약 조정 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-generation-deepseek-client-adapter_report.md` | 작업 결과 리포트 |
| Modify | `doc/workspaces/DevC_강상민/2026-06-09_ai-msa-work-schedule.md` | 이번 작업 완료와 다음 작업 갱신 |

## 구현 순서

1. `DeepSeekGenerationClient` interface와 request/response record를 추가한다.
2. HTTP adapter를 구현한다.
   - `POST {baseUrl}/chat/completions`
   - `Authorization: Bearer {apiKey}`
   - `Content-Type: application/json`
   - `messages`: optional system + required user
   - `stream=false`
3. HTTP 실패 매핑을 고정한다.
   - `401 -> UNAUTHORIZED`
   - `403 -> FORBIDDEN`
   - `429 -> RATE_LIMITED`
   - `5xx -> DOWNSTREAM_ERROR`
   - timeout/resource access -> `TIMEOUT`
   - malformed provider response -> `RESPONSE_MAPPING_FAILED`
   - invalid request/config -> `VALIDATION_FAILED`
4. executor skeleton이 HTTP 설정을 직접 갖지 않도록 client/model 의존으로 바꾼다.
5. configuration 테스트와 adapter 테스트를 작성한다.
6. report와 MSA 스케줄을 업데이트한다.
7. 검증 명령을 실행한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `DeepSeekGenerationClientHttpAdapterTest` | request body, auth header, success response parsing |
| `DeepSeekGenerationClientHttpAdapterTest` | 401/403/429/5xx/timeout/malformed response 매핑 |
| `AiGenerationRealExecutorConfigurationTest` | `mode=deepseek`에서 client와 executor bean 등록 |
| `DeepSeekGenerationWorkerExecutorSkeletonTest` | executor가 client/model 기반으로 생성되고 아직 안전한 미구현 예외를 던짐 |

## 수용 기준

- [ ] DeepSeek client interface와 HTTP adapter가 추가된다.
- [ ] adapter는 prompt/context 조립이나 generated asset 저장을 하지 않는다.
- [ ] executor skeleton은 HTTP 설정값 대신 client/model에만 의존한다.
- [ ] adapter 계약 테스트와 configuration 테스트가 통과한다.
- [ ] MSA 스케줄에서 이번 작업은 완료, 다음 작업은 `ai-generation-real-executor-implementation`으로 표시된다.
- [ ] 금지 데이터/민감 값 예시를 저장하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- client DTO, HTTP adapter, configuration wiring, executor constructor가 같은 계약을 공유한다.
- 테스트와 구현이 동시에 바뀌므로 한 흐름에서 직접 실행하는 편이 안전하다.
- 변경 경로가 `ai-service` 내부에 집중되어 병렬 편집 이점이 작다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 기준으로 직접 실행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.client.deepseek.DeepSeekGenerationClientHttpAdapterTest --tests com.qtai.ai.AiGenerationRealExecutorConfigurationTest --tests com.qtai.domain.ai.internal.DeepSeekGenerationWorkerExecutorSkeletonTest --tests com.qtai.ai.AiGenerationRealExecutorRuntimeToggleTest
cd ..
git diff --check
```

추가로 변경 범위에서 금지 데이터/민감 값 예시와 placeholder 문구가 없는지 검색한다.

## 후속 작업으로 남기는 항목

- `ai-generation-real-executor-implementation`
- `ai-generation-real-executor-runtime-smoke`
- `ai-event-outbox-relay-design`
