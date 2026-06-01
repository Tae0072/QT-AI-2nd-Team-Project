# Workflow - 2026-06-01 ai-failure-retry-policy-runtime

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-failure-retry-policy-runtime` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | AI 구현 순서 9번: 실패/재시도 정책 정리 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-28_ai-implementation-order.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/src/main/java/com/qtai/external/llm/**`, `qtai-server/src/test/java/com/qtai/external/llm/**`, `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

DeepSeek 호출, generation runner, 자동 검증 최소 구현이 연결된 이후의 런타임 실패 처리 기준을 코드와 테스트로 고정한다. 이번 작업은 실제 retry/backoff 실행기가 아니라, 운영자가 실패 원인을 안전하게 구분할 수 있도록 `ai_generation_jobs.error_message`와 validation log error message에 저장되는 값을 정리하는 작업이다.

핵심 원칙은 외부 provider raw response, prompt 원문, API key, secret 계열 값을 저장하지 않고, timeout/429/5xx/응답 형식 오류/자동 검증 설정 오류를 짧은 문자열 코드로 남기는 것이다. 실제 재시도 횟수, backoff, 멈춘 `RUNNING` job 회수는 후속 PR로 분리한다.

## 범위

- `DeepSeekLlmClient` 외부 API 실패를 안전한 문자열 코드로 정규화한다.
- timeout은 `LLM_TIMEOUT`으로 변환한다.
- HTTP 429는 `LLM_RATE_LIMIT`으로 변환한다.
- HTTP 5xx와 기타 `RestClientException`은 `LLM_PROVIDER_ERROR`로 변환한다.
- HTTP 429 외 4xx는 `LLM_PROVIDER_REQUEST_REJECTED`로 변환한다.
- provider 응답 body가 비었거나 `choices/message/content`가 없으면 `LLM_RESPONSE_INVALID`로 변환한다.
- DeepSeek API key 미설정은 `LLM_CONFIGURATION_ERROR`로 변환한다.
- `AiGenerationJobRunner`는 기존처럼 `BusinessException.getMessage()`를 저장하되, 위 코드가 job `FAILED`의 `errorMessage`로 남는지 검증한다.
- 자동 검증 checklist 누락/중복 같은 설정 오류는 `AUTO_VALIDATION_CONFIGURATION_ERROR` 계열 메시지로 job `FAILED`에 남긴다.
- 자동 검증 결과 `REJECTED`는 기존 정책대로 validation log 생성, asset `REJECTED`, job `SUCCEEDED`를 유지한다.
- 작업 후 report를 작성해 실행한 검증 명령과 미실행 사유를 남긴다.

## 제외 범위

- DB migration, 신규 컬럼, 신규 enum, retry count, next retry time 추가.
- 실제 retry/backoff, circuit breaker, Redis rate limit, dead letter queue 구현.
- 멈춘 `RUNNING` job 회수 또는 재처리 batch 구현.
- 관리자 재생성 API 정책 변경.
- OpenAPI 변경. 외부 HTTP 계약은 바꾸지 않는다.
- F-15 Q&A API와 `ai_qa_requests` 구현.
- 사용자 API `/api/v1/ai/**` 구현 또는 변경.
- provider raw response, prompt 원문, validation reference 원문 저장.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/external/llm/DeepSeekLlmClient.java` | 외부 API 실패를 안전한 문자열 코드로 변환 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiAutoValidationService.java` | 활성 EXPLANATION checklist 설정 오류 메시지 정규화 |
| Inspect/Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRunner.java` | 기존 failure message 저장 정책이 정규화된 코드를 보존하는지 확인하고 필요한 최소 보강 |
| Test | `qtai-server/src/test/java/com/qtai/external/llm/DeepSeekLlmClientTest.java` | timeout, 429, 4xx, 5xx, invalid response, secret/prompt 미노출 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerTest.java` | runner가 LLM 실패 코드를 job `FAILED.errorMessage`로 저장하는지 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerIntegrationTest.java` | LLM/handler 실패와 자동 검증 실패 정책이 기존 DB 흐름과 충돌하지 않는지 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-01_ai-failure-retry-policy-runtime_report.md` | 구현 결과, 검증 결과, 제외 범위, 후속 작업 기록 |

## 구현 순서

1. `DeepSeekLlmClientTest`에서 기존 오류 테스트 기대값을 문자열 코드 기준으로 먼저 갱신한다.
2. timeout `ResourceAccessException`이 `LLM_TIMEOUT` 메시지를 가진 `BusinessException(INTERNAL_ERROR)`로 변환되는지 검증한다.
3. `HttpClientErrorException(TOO_MANY_REQUESTS)`가 `LLM_RATE_LIMIT`으로 변환되는지 검증한다.
4. `HttpServerErrorException(INTERNAL_SERVER_ERROR)`와 `SERVICE_UNAVAILABLE`이 `LLM_PROVIDER_ERROR`로 변환되는지 검증한다.
5. 401, 402, 403, 422 같은 429 외 4xx가 `LLM_PROVIDER_REQUEST_REJECTED`로 변환되는지 검증한다.
6. 빈 body, 빈 choices, blank content가 `LLM_RESPONSE_INVALID`로 변환되는지 검증한다.
7. API key 누락이 `LLM_CONFIGURATION_ERROR`로 변환되고 provider 호출을 하지 않는지 검증한다.
8. 오류 메시지에 provider body, prompt, API key, token, secret, password 계열 값이 포함되지 않는지 기존 테스트를 유지하거나 보강한다.
9. `DeepSeekLlmClient`에서 `HttpStatusCodeException`, `ResourceAccessException`, `RestClientException`, invalid response 처리 메시지를 위 코드로 정규화한다.
10. `AiAutoValidationServiceTest` 또는 runner 테스트에서 활성 EXPLANATION checklist가 없을 때 job `FAILED.errorMessage=AUTO_VALIDATION_CONFIGURATION_ERROR`로 남는 케이스를 추가한다.
11. `AiAutoValidationService`의 활성 checklist 조회 실패 메시지를 `AUTO_VALIDATION_CONFIGURATION_ERROR`로 정리한다.
12. `AiGenerationJobRunnerTest`에 LLM timeout/rate-limit/provider error가 발생하면 asset 저장 없이 job이 `FAILED`가 되고 안전한 코드가 `errorMessage`에 남는지 검증한다.
13. `AiGenerationJobRunnerIntegrationTest`의 기존 invalid JSON/out-of-scope verseId 테스트가 asset/log 없이 job `FAILED`를 유지하는지 확인한다.
14. 자동 검증 `REJECTED` 케이스는 job `SUCCEEDED`, asset `REJECTED`, validation log `REJECTED`를 유지하는지 회귀 검증한다.
15. 관련 테스트와 빌드를 실행한다.
16. report 파일에 변경 내용, 수용 기준, 검증 결과, 실행하지 못한 명령 사유를 기록한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 또는 확인할 검증 |
| --- | --- |
| `DeepSeekLlmClientTest` | timeout은 `LLM_TIMEOUT`으로 변환 |
| `DeepSeekLlmClientTest` | 429는 `LLM_RATE_LIMIT`으로 변환 |
| `DeepSeekLlmClientTest` | 5xx와 기타 client 오류는 `LLM_PROVIDER_ERROR`로 변환 |
| `DeepSeekLlmClientTest` | 429 외 4xx는 `LLM_PROVIDER_REQUEST_REJECTED`로 변환 |
| `DeepSeekLlmClientTest` | invalid provider response는 `LLM_RESPONSE_INVALID`로 변환 |
| `DeepSeekLlmClientTest` | API key 누락은 `LLM_CONFIGURATION_ERROR`이고 provider 호출 없음 |
| `DeepSeekLlmClientTest` | 오류 메시지에 provider body, prompt, API key, secret/token/password 계열 값이 포함되지 않음 |
| `AiGenerationJobRunnerTest` | LLM 실패 코드는 asset 없이 job `FAILED.errorMessage`에 저장 |
| `AiGenerationJobRunnerTest` | 자동 검증 설정 오류는 job `FAILED.errorMessage=AUTO_VALIDATION_CONFIGURATION_ERROR`로 저장 |
| `AiGenerationJobRunnerIntegrationTest` | handler 단계 invalid JSON/out-of-scope verseId는 asset/log 없이 job `FAILED` 유지 |
| `AiGenerationJobRunnerIntegrationTest` | 자동 검증 `REJECTED`는 job `SUCCEEDED`, asset `REJECTED`, validation log `REJECTED` 유지 |

## 수용 기준

- [ ] timeout 실패는 job `FAILED.errorMessage=LLM_TIMEOUT`으로 남는다.
- [ ] HTTP 429 실패는 job `FAILED.errorMessage=LLM_RATE_LIMIT`으로 남는다.
- [ ] HTTP 5xx 실패는 job `FAILED.errorMessage=LLM_PROVIDER_ERROR`로 남는다.
- [ ] HTTP 429 외 4xx 실패는 job `FAILED.errorMessage=LLM_PROVIDER_REQUEST_REJECTED`로 남는다.
- [ ] provider 응답 형식 오류는 `LLM_RESPONSE_INVALID`로 분류된다.
- [ ] API key 미설정은 `LLM_CONFIGURATION_ERROR`로 분류되고 provider 호출을 하지 않는다.
- [ ] 자동 검증 설정 오류는 job `FAILED`로 종료되고 `AUTO_VALIDATION_CONFIGURATION_ERROR`가 남는다.
- [ ] 자동 검증 결과 `REJECTED`는 job 실패가 아니라 job `SUCCEEDED`와 asset `REJECTED` 정책을 유지한다.
- [ ] 실제 retry/backoff, retry count, next retry time, DB migration은 추가하지 않는다.
- [ ] provider raw response, prompt 원문, validation reference 원문, secret/token/password 값은 error message, payload, checklistJson에 저장하지 않는다.
- [ ] 사용자 API `/api/v1/ai/**` 구현은 추가하지 않는다.
- [ ] 다른 도메인의 `internal`, `web`, repository 타입을 직접 import하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- DeepSeek client의 실패 메시지와 runner의 job 상태 저장 정책이 한 흐름으로 연결되어 있어 테스트 기대값을 순서대로 맞춰야 한다.
- 변경 파일은 외부 LLM client와 AI runner 테스트에 집중되어 있고, 병렬 작업 시 같은 테스트 파일 충돌 가능성이 높다.
- DB schema나 OpenAPI 변경이 없는 작은 정책 정리 PR이므로 단일 agent가 TDD로 직접 수행하는 편이 안전하다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 테스트 기대값 갱신, 구현, 통합 테스트 확인, report 작성을 순차적으로 직접 수행한다.

## 검증 계획

- `.\gradlew.bat test --tests "*DeepSeekLlmClientTest"` in `qtai-server`
- `.\gradlew.bat test --tests "*AiGenerationJobRunnerTest" --tests "*AiGenerationJobRunnerIntegrationTest"` in `qtai-server`
- `.\gradlew.bat test --tests "*AiGenerationJob*"` in `qtai-server`
- `.\gradlew.bat build` in `qtai-server`
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`
- `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/external/llm`
- `rg -n "providerRawResponse|rawResponse|validationReferenceText|promptText|password|private key|token|secret" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/external/llm qtai-server/src/test/java/com/qtai/external/llm`
- `git diff --check`

`spectral` ruleset이나 `gitleaks` 실행 파일이 로컬에 없으면 실행하지 못한 이유를 report와 최종 응답에 기록한다.

## 후속 작업으로 남길 항목

- 실제 retry count, backoff, next retry time 저장 정책과 DB migration.
- 멈춘 `RUNNING` job 감지와 회수 정책.
- 반복 실패 집계 기준과 관리자 조회/알림 정책.
- 04:00 KST batch, fixed-delay worker, 시스템 API 수동 트리거 중 운영 실행 방식 확정.
- 관리자 승인 후 사용자 노출 테이블 반영.
- F-15 Q&A 구현 여부 재검토.
