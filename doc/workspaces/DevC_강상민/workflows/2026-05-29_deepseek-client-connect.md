# Workflow - 2026-05-29 deepseek-client-connect

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/deepseek-client-connect` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | AI 구현 순서 스케줄표의 PR 1: DeepSeek 클라이언트 연결, AI 호출 설정 정리, AI 호출 mock 테스트를 먼저 진행 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/23_도메인_용어사전.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/src/main/java/com/qtai/external/llm/**`, `qtai-server/src/test/java/com/qtai/external/llm/**`, `qtai-server/src/main/resources/application.yml`, `qtai-server/src/test/resources/application-test.yml` |

## 작업 목표

`DeepSeekLlmClient`의 placeholder 구현을 실제 DeepSeek OpenAI-compatible HTTP 호출 구현으로 교체한다. 이 작업은 서버가 외부 LLM과 통신할 수 있는 최소 기반을 만드는 PR이며, 사용자 API나 DB 저장 흐름은 건드리지 않는다.

이번 PR은 AI 구현 순서 스케줄표의 1~3번을 한 번에 처리한다. 즉, DeepSeek 클라이언트 연결, API key/model/base URL/timeout 환경변수 설정, 실제 API 호출 없는 mock 기반 성공/실패 테스트까지가 완료 기준이다.

## 범위

- `DeepSeekLlmClient.complete(...)`가 실제 HTTP 요청을 보낸다.
- HTTP client는 기존 Kakao client 관례에 맞춰 `RestTemplate`을 사용한다.
- 요청은 OpenAI-compatible chat completions 형태로 구성한다.
- API key, base URL, model, connect timeout, read timeout은 환경변수 기반 설정으로 둔다.
- `LlmCompletionRequest`는 `model`, `systemPrompt`, `prompt`, `maxTokens`, `temperature`를 받는다.
- `model`이 null 또는 blank이면 설정 기본 모델을 사용한다.
- `LlmCompletionResponse`는 `content`, `promptTokens`, `completionTokens`, `totalTokens`, `model`을 반환한다.
- 응답은 첫 번째 choice의 message content와 usage token 값만 파싱한다.
- 외부 API 장애, 인증 실패, rate limit, timeout, 빈 응답은 공통 예외 규칙으로 감싼다.
- 테스트는 mock `RestTemplate`을 주입해 실제 DeepSeek API를 호출하지 않는다.

## 제외 범위

- F-15 Q&A API 구현은 하지 않는다.
- `AiController` placeholder는 수정하지 않는다.
- `ai_qa_requests` 테이블, entity, repository는 만들지 않는다.
- `ai_generation_jobs` 실행 processor는 만들지 않는다.
- `ai_generated_assets`, `ai_validation_logs` 저장 흐름과 연결하지 않는다.
- 관리자/시스템 API 트리거와 연결하지 않는다.
- 배치, scheduler, worker는 만들지 않는다.
- Redis rate limit, 재시도 backoff, circuit breaker는 구현하지 않는다.
- 실제 DeepSeek API를 호출하는 통합 테스트는 만들지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/external/llm/DeepSeekLlmClient.java` | DeepSeek HTTP 호출, 요청 body 구성, 응답 파싱, 외부 오류 매핑 |
| Modify | `qtai-server/src/main/java/com/qtai/external/llm/dto/LlmCompletionRequest.java` | provider 중립 LLM 요청 DTO 필드 정리 |
| Modify | `qtai-server/src/main/java/com/qtai/external/llm/dto/LlmCompletionResponse.java` | provider 중립 LLM 응답 DTO 필드 정리 |
| Modify | `qtai-server/src/main/resources/application.yml` | `external.llm.deepseek.*` 설정 추가 |
| Modify | `qtai-server/src/test/resources/application-test.yml` | 테스트용 더미 DeepSeek 설정 추가. 실제 secret 금지 |
| Create | `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | mock RestTemplate 기반 성공/실패 테스트 |

## 설정 계약

| 설정 키 | 기본값 | 설명 |
| --- | --- | --- |
| `external.llm.deepseek.api-key` | `${DEEPSEEK_API_KEY:}` | DeepSeek API key. 기본값은 빈 문자열이며, 실제 값은 환경변수로만 주입 |
| `external.llm.deepseek.base-url` | `${DEEPSEEK_BASE_URL:https://api.deepseek.com}` | DeepSeek OpenAI-compatible base URL |
| `external.llm.deepseek.model` | `${DEEPSEEK_MODEL:deepseek-v4-flash}` | 기본 모델. 요청 DTO의 model이 있으면 요청 DTO 값을 우선 |
| `external.llm.deepseek.connect-timeout-ms` | `${DEEPSEEK_CONNECT_TIMEOUT_MS:3000}` | 연결 timeout |
| `external.llm.deepseek.read-timeout-ms` | `${DEEPSEEK_READ_TIMEOUT_MS:30000}` | 읽기 timeout |

## 구현 순서

1. `DeepSeekLlmClientTest`를 먼저 만든다. 정상 응답, 빈 응답, 4xx, 429, 5xx, timeout 케이스를 mock `RestTemplate`으로 고정한다.
2. 테스트용 생성자를 설계한다. 운영 생성자는 설정값으로 `RestTemplate`을 만들고, 테스트 생성자는 mock `RestTemplate`과 설정값을 직접 주입받는다.
3. `LlmCompletionRequest`를 `model`, `systemPrompt`, `prompt`, `maxTokens`, `temperature` 필드로 정리한다.
4. `LlmCompletionResponse`를 `content`, `promptTokens`, `completionTokens`, `totalTokens`, `model` 필드로 정리한다.
5. `application.yml`에 `external.llm.deepseek.*` 설정을 추가한다. 실제 API key 값은 쓰지 않는다.
6. `application-test.yml`에 테스트용 더미 설정을 추가한다. 실제 secret처럼 보이는 값은 쓰지 않는다.
7. `DeepSeekLlmClient.complete(...)`에서 입력 request가 null이거나 prompt가 blank이면 `BusinessException(ErrorCode.INVALID_INPUT)`을 던진다.
8. API key가 blank이면 외부 호출 전에 `BusinessException(ErrorCode.INTERNAL_ERROR, "DeepSeek API key is not configured")`를 던진다.
9. 요청 body는 `model`, `messages`, `max_tokens`, `temperature`, `stream=false`를 포함한다.
10. `systemPrompt`가 blank가 아니면 `messages[0]`에 `role=system`으로 넣고, 사용자 prompt는 `role=user`로 넣는다.
11. `Authorization: Bearer {apiKey}`와 `Content-Type: application/json` 헤더를 설정한다.
12. 요청 URL은 `{baseUrl}/chat/completions`로 조합하되, base URL 끝의 `/` 중복을 방지한다.
13. 응답 body가 null이거나 `choices[0].message.content`가 blank이면 `BusinessException(ErrorCode.INTERNAL_ERROR)`로 변환한다.
14. 응답 usage가 없으면 token 값은 null로 반환한다.
15. `HttpClientErrorException`, `HttpServerErrorException`, `ResourceAccessException`, `RestClientException`을 잡아 `BusinessException(ErrorCode.INTERNAL_ERROR)`로 변환한다.
16. 예외 메시지와 로그에는 API key, Authorization header, prompt, raw response body를 포함하지 않는다.
17. mock 테스트를 실행해 요청 URL, 헤더, body, 응답 파싱, 예외 매핑을 검증한다.
18. AI 도메인 테스트와 전체 build를 실행해 DTO 변경의 영향 범위를 확인한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | 정상 응답에서 content, model, promptTokens, completionTokens, totalTokens가 매핑된다 |
| `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | request model이 blank이면 설정 기본 모델 `deepseek-v4-flash`가 body에 들어간다 |
| `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | systemPrompt가 있으면 system message가 user message보다 먼저 들어간다 |
| `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | prompt가 blank이면 `INVALID_INPUT`으로 실패하고 외부 호출하지 않는다 |
| `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | API key가 blank이면 `INTERNAL_ERROR`로 실패하고 외부 호출하지 않는다 |
| `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | null body, 빈 choices, 빈 content는 `INTERNAL_ERROR`로 실패한다 |
| `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | 401, 402, 422, 429, 500, 503은 `INTERNAL_ERROR`로 변환된다 |
| `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | timeout은 `INTERNAL_ERROR`로 변환된다 |
| `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | exception message에 API key, prompt, raw response body가 포함되지 않는다 |

## 수용 기준

- [ ] `DeepSeekLlmClient.complete(...)`가 정상 응답 content를 반환한다.
- [ ] API key는 환경변수 기반으로만 주입된다.
- [ ] 실제 secret, token, password, private key 예시는 커밋하지 않는다.
- [ ] request DTO의 model이 없으면 기본 모델 `deepseek-v4-flash`를 사용한다.
- [ ] 외부 오류는 공통 `BusinessException`으로 변환된다.
- [ ] 예외 메시지와 로그에 API key, prompt, provider raw response가 남지 않는다.
- [ ] 테스트는 실제 DeepSeek API를 호출하지 않는다.
- [ ] Q&A, generation job 실행, asset/log 저장 흐름은 변경하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `external.llm`의 단일 client와 DTO, 설정, 단위 테스트에 집중되어 있다.
- DTO 변경과 client 구현, 테스트 기대값이 강하게 연결되어 있어 한 흐름에서 순서대로 확인하는 편이 안전하다.
- 병렬화하면 request/response 필드 이름과 예외 매핑 기준이 엇갈릴 가능성이 있다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 테스트 작성, DTO 정리, client 구현, 설정 추가, 검증 명령 실행을 직접 순서대로 수행한다.

## 검증 계획

- `./gradlew -p qtai-server test --tests "*DeepSeekLlmClientTest"`
- `./gradlew -p qtai-server test --tests "*Ai*"`
- `./gradlew -p qtai-server build`
- `gitleaks detect --source . --redact --exit-code 1`
- `git diff --check`

`gitleaks`가 로컬에 설치되어 있지 않으면 실행 불가 사유를 report와 최종 응답에 명확히 남긴다.

## 후속 작업으로 남길 항목

- PR 2: 기존 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs` 구조 점검
- PR 3: generation job 실행 흐름과 `LlmClient` 연결
- PR 4: 자동 검증 최소 구현
- PR 5: timeout, 429, 5xx, 검증 실패에 대한 실패/재시도 정책 정리
- Q&A(F-15)는 DeepSeek client, generation job, validation log 흐름이 안정화된 뒤 별도 workflow로 작성한다.
