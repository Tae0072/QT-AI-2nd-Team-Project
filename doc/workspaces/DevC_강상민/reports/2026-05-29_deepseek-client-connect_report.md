# 2026-05-29 deepseek-client-connect 작업 보고

## 개요

- 관련 F-ID: F-02, F-14
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-05-29_deepseek-client-connect.md`
- 브랜치: `feature/deepseek-client-connect`
- PR 대상: `dev`
- 실행 경로: workflow-spec-runner 직접 실행

## 작업 결과

`DeepSeekLlmClient`의 placeholder 구현을 실제 DeepSeek OpenAI-compatible chat completions 호출 구현으로 교체했다. 이번 작업은 AI 구현 순서 스케줄표의 PR 1 범위인 DeepSeek client 연결, 환경변수 기반 호출 설정, mock 기반 성공/실패 테스트까지를 처리했다.

Q&A(F-15), generation job processor, `ai_generated_assets`/`ai_validation_logs` 저장 연결, 배치/worker는 workflow 제외 범위에 따라 구현하지 않았다.

## 변경 내용

- `DeepSeekLlmClient.complete(...)`가 `RestTemplate`으로 `POST {baseUrl}/chat/completions`를 호출하도록 구현했다.
- 요청 body는 `model`, `messages`, `max_tokens`, `temperature`, `stream=false`로 구성한다.
- `systemPrompt`가 있으면 system message를 user message보다 먼저 넣는다.
- 응답은 `choices[0].message.content`와 `usage.prompt_tokens`, `usage.completion_tokens`, `usage.total_tokens`, `model`만 파싱한다.
- `LlmCompletionRequest`를 `model`, `systemPrompt`, `prompt`, `maxTokens`, `temperature` 필드로 정리했다.
- `LlmCompletionResponse`를 `content`, `promptTokens`, `completionTokens`, `totalTokens`, `model` 필드로 정리했다.
- `external.llm.deepseek.*` 설정을 `application.yml`과 `application-test.yml`에 추가했다.
- `DeepSeekLlmClientTest`를 추가해 실제 DeepSeek API 호출 없이 mock `RestTemplate`으로 성공/실패 흐름을 검증했다.

## 설정 계약

| 설정 키 | 기본값 | 설명 |
| --- | --- | --- |
| `external.llm.deepseek.api-key` | `${DEEPSEEK_API_KEY:}` | DeepSeek API key. 실제 값은 환경변수로만 주입 |
| `external.llm.deepseek.base-url` | `${DEEPSEEK_BASE_URL:https://api.deepseek.com}` | DeepSeek base URL |
| `external.llm.deepseek.model` | `${DEEPSEEK_MODEL:deepseek-v4-flash}` | 기본 모델 |
| `external.llm.deepseek.connect-timeout-ms` | `${DEEPSEEK_CONNECT_TIMEOUT_MS:3000}` | 연결 timeout |
| `external.llm.deepseek.read-timeout-ms` | `${DEEPSEEK_READ_TIMEOUT_MS:30000}` | 읽기 timeout |

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/external/llm/DeepSeekLlmClient.java` | DeepSeek HTTP 호출, 응답 파싱, 외부 오류 변환 구현 |
| `qtai-server/src/main/java/com/qtai/external/llm/dto/LlmCompletionRequest.java` | LLM 요청 DTO 필드 정리 |
| `qtai-server/src/main/java/com/qtai/external/llm/dto/LlmCompletionResponse.java` | LLM 응답 DTO 필드 정리 |
| `qtai-server/src/main/resources/application.yml` | DeepSeek 환경변수 기반 설정 추가 |
| `qtai-server/src/test/resources/application-test.yml` | 테스트용 placeholder DeepSeek 설정 추가 |
| `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | mock 기반 client 단위 테스트 추가 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*DeepSeekLlmClientTest"` in `qtai-server` | 통과 |
| `.\gradlew.bat test --tests "*Ai*"` in `qtai-server` | 통과 |
| `.\gradlew.bat build` in `qtai-server` | 통과 |
| `git diff --check` | 통과. CRLF 변환 경고만 출력 |
| `gitleaks detect --source . --redact --exit-code 1` | 실행 불가. 로컬에 `gitleaks` 명령이 없음 |
| `rg -n "test-api-key\|test-deepseek\|sk-[A-Za-z0-9]\|private key\|raw provider body" ...` | 매칭 없음 |

## 수용 기준 확인

| 수용 기준 | 상태 |
| --- | --- |
| `DeepSeekLlmClient.complete(...)`가 정상 응답 content를 반환한다 | 충족 |
| API key는 환경변수 기반으로만 주입된다 | 충족 |
| 실제 secret, token, password, private key 예시는 커밋하지 않는다 | 충족 |
| request DTO의 model이 없으면 기본 모델 `deepseek-v4-flash`를 사용한다 | 충족 |
| 외부 오류는 공통 `BusinessException`으로 변환된다 | 충족 |
| 예외 메시지에 API key, prompt, provider payload가 남지 않는다 | 충족 |
| 테스트는 실제 DeepSeek API를 호출하지 않는다 | 충족 |
| Q&A, generation job 실행, asset/log 저장 흐름은 변경하지 않는다 | 충족 |

## 제외 범위

- F-15 Q&A API 구현
- `AiController` placeholder 수정
- `ai_qa_requests` 테이블/entity/repository 추가
- `ai_generation_jobs` 실행 processor 추가
- `ai_generated_assets`, `ai_validation_logs` 저장 흐름 연결
- 관리자/시스템 API 트리거 연결
- 배치, scheduler, worker 구현
- Redis rate limit, 재시도 backoff, circuit breaker 구현
- 실제 DeepSeek API를 호출하는 통합 테스트

## 후속 작업

- PR 2: 기존 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs` 구조 점검
- PR 3: generation job 실행 흐름과 `LlmClient` 연결
- PR 4: 자동 검증 최소 구현
- PR 5: timeout, 429, 5xx, 검증 실패에 대한 실패/재시도 정책 정리
- Q&A(F-15)는 DeepSeek client, generation job, validation log 흐름이 안정화된 뒤 별도 workflow로 작성
