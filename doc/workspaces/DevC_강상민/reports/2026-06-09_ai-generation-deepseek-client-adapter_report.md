# ai-generation-deepseek-client-adapter report

## 작업 요약

- `ai-generation-deepseek-client-adapter` workflow 문서를 작성했다.
- `DeepSeekGenerationClient` interface와 request/response DTO 계약을 추가했다.
- `DeepSeekGenerationClientHttpAdapter`를 추가해 OpenAI-compatible `/chat/completions` 호출 계층을 분리했다.
- `AiServiceGenerationExecutorConfiguration`에서 `mode=deepseek`일 때 DeepSeek client bean과 executor skeleton bean이 함께 등록되도록 조정했다.
- `DeepSeekGenerationWorkerExecutor`가 HTTP 설정값을 직접 보유하지 않고 `DeepSeekGenerationClient`와 model에만 의존하도록 변경했다.
- MSA 작업 스케줄을 현재 상태 기준으로 업데이트했다.

## 변경 범위

| 구분 | 경로 | 내용 |
| --- | --- | --- |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/client/deepseek/DeepSeekGenerationClient.java` | DeepSeek client interface와 DTO |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/client/deepseek/DeepSeekGenerationClientHttpAdapter.java` | OpenAI-compatible HTTP adapter |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/client/deepseek/DeepSeekGenerationClientHttpAdapterTest.java` | adapter 계약 테스트 |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceGenerationExecutorConfiguration.java` | deepseek mode bean wiring |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/DeepSeekGenerationWorkerExecutor.java` | executor skeleton 의존성 분리 |
| Modify | `qtai-server/ai-service/src/test/java/com/qtai/ai/AiGenerationRealExecutorConfigurationTest.java` | client bean 등록 검증 |
| Modify | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/DeepSeekGenerationWorkerExecutorSkeletonTest.java` | skeleton constructor 계약 갱신 |
| Modify | `doc/workspaces/DevC_강상민/2026-06-09_ai-msa-work-schedule.md` | 완료/다음 작업 상태 업데이트 |

## 계약 결정

- DeepSeek adapter는 HTTP 요청/응답 변환과 실패 매핑만 담당한다.
- 요청은 `POST {baseUrl}/chat/completions`로 보내며 `Authorization: Bearer {apiKey}`를 사용한다.
- 응답은 assistant content, usage token count, model만 DTO로 변환한다.
- 실패 매핑은 `AiClientException`으로 통일한다.
  - `401 -> UNAUTHORIZED`
  - `403 -> FORBIDDEN`
  - `429 -> RATE_LIMITED`
  - `5xx -> DOWNSTREAM_ERROR`
  - resource access failure -> `TIMEOUT`
  - malformed response -> `RESPONSE_MAPPING_FAILED`
  - invalid request/config -> `VALIDATION_FAILED`

## 제외 확인

- real executor 구현 없음
- prompt/context 조립 없음
- provider live 조회 없음
- Kafka 의존성, topic, relay, consumer 구현 없음
- scheduler 운영 활성화 없음
- monolith `external.llm` 코드 복사 없음
- prompt 본문, provider raw response, 본문 원문, 인증 값, DB 접속 값 저장 없음

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 통과 |
| `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.client.deepseek.DeepSeekGenerationClientHttpAdapterTest --tests com.qtai.ai.AiGenerationRealExecutorConfigurationTest --tests com.qtai.domain.ai.internal.DeepSeekGenerationWorkerExecutorSkeletonTest --tests com.qtai.domain.ai.internal.AiGenerationRealExecutorRuntimeToggleTest` | 통과 |
| `git diff --check` | 통과 |
| placeholder 문구 검색 | 매칭 없음 |
| 금지 데이터/민감 예시 검색 | 매칭 없음 |
| monolith LLM DTO/import 검색 | 매칭 없음 |

## 후속 작업

다음 작업은 `ai-generation-real-executor-implementation`이다.
