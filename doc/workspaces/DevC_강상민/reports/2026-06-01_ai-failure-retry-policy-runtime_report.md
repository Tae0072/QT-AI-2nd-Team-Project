# AI 실패/재시도 정책 정리 런타임 반영 리포트

- 작업일: 2026-06-01
- 작업 브랜치: `feature/ai-failure-retry-policy-runtime`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-failure-retry-policy-runtime.md`
- 관련 기능: F-02, F-14

## 1. 작업 요약

DeepSeek 호출과 AI 생성 job 처리 흐름에서 실패 원인을 안전한 문자열 코드로 정규화했다. 이번 작업은 실패 분류와 저장 흐름 확인에 한정했으며, DB 컬럼 추가, OpenAPI 변경, 실제 재시도/backoff 구현은 포함하지 않았다.

## 2. 반영 내용

### 2.1 DeepSeek LLM 클라이언트 실패 코드 정규화

`DeepSeekLlmClient`에서 외부 API 실패와 설정/응답 오류를 아래 코드로 변환하도록 정리했다.

- `LLM_CONFIGURATION_ERROR`: API key 미설정
- `LLM_RATE_LIMIT`: DeepSeek 429 응답
- `LLM_PROVIDER_ERROR`: DeepSeek 5xx 또는 일반 provider 통신 실패
- `LLM_PROVIDER_REQUEST_REJECTED`: 429를 제외한 4xx provider 거절
- `LLM_TIMEOUT`: timeout 계열 접근 실패
- `LLM_RESPONSE_INVALID`: provider 응답 body/choices/content 불량

provider 응답 본문, credential, prompt 원문은 예외 메시지에 포함하지 않도록 테스트를 유지했다.

### 2.2 AI generation job 실패 저장 흐름 보강

`AiGenerationJobRunner`의 기존 실패 처리 구조를 유지하면서, LLM 실패 코드가 job의 `errorMessage`에 저장되고 산출물 저장 및 자동 검증으로 진행되지 않는지 테스트를 보강했다.

확인한 실패 코드:

- `LLM_TIMEOUT`
- `LLM_RATE_LIMIT`
- `LLM_PROVIDER_ERROR`

### 2.3 자동 검증 설정 오류 코드 정리

활성 explanation checklist가 없거나 2개 이상인 설정 오류를 `AUTO_VALIDATION_CONFIGURATION_ERROR`로 정규화했다. 해당 오류 발생 시 generation job은 실패 처리되고, 자동 검증 로그는 생성되지 않도록 integration test를 추가했다.

## 3. 범위 제외

이번 PR 범위에서 제외한 항목은 아래와 같다.

- retry count, next retry time 등 DB 스키마 변경
- 실제 재시도/backoff/scheduler 구현
- OpenAPI 응답 계약 변경
- 관리자 실패 사유 조회 API 변경
- provider별 상세 오류 본문 저장

## 4. 수용 기준 점검

- DeepSeek timeout/429/5xx/기타 4xx/응답 불량/API key 미설정이 안전한 실패 코드로 변환된다.
- provider credential, prompt, raw response가 실패 메시지에 노출되지 않는다.
- LLM 실패 시 generation job은 `FAILED`가 되고 산출물을 저장하지 않는다.
- 자동 검증 설정 오류 시 generation job은 `FAILED`가 되고 검증 로그를 남기지 않는다.
- 신규 DB 컬럼, OpenAPI 변경, retry/backoff 구현을 추가하지 않았다.

## 5. 검증 기록

실행 완료:

```powershell
.\gradlew.bat test --tests "*DeepSeekLlmClientTest"
.\gradlew.bat test --tests "*AiGenerationJobRunnerTest" --tests "*AiGenerationJobRunnerIntegrationTest"
.\gradlew.bat test --tests "*AiGenerationJob*"
.\gradlew.bat build
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/external/llm
rg -n "providerRawResponse|rawResponse|validationReferenceText|promptText|password|private key|token|secret" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/external/llm qtai-server/src/test/java/com/qtai/external/llm
git diff --check
```

결과:

- `DeepSeekLlmClientTest`, `AiGenerationJobRunnerTest`, `AiGenerationJobRunnerIntegrationTest`, `AiGenerationJob*`, `build` 모두 통과
- 도메인 경계 금지 import 검색 결과 없음
- 민감 키워드 검색 결과는 기존 저장 금지 guard, 관련 테스트, token usage 필드 사용으로 확인
- `git diff --check` 통과, 단 Git line ending 경고만 출력

중간 이슈:

- 최초 `AiGenerationJobRunnerIntegrationTest` 보강 시 `@DataJpaTest` 외부 트랜잭션 영향으로 asset 미저장 단정이 실제 런타임 rollback 경계와 다르게 관측되어 실패했다.
- 해당 테스트는 이번 수용 기준에 맞춰 job 실패 상태와 validation log 미생성을 검증하도록 조정했다.

실행 불가:

- `.\gradlew.bat test jacocoTestReport`: 현재 Gradle 프로젝트에 `jacocoTestReport` 태스크 없음
- `.\gradlew.bat jacocoTestCoverageVerification`: `tasks --all`에서 jacoco/coverage 태스크가 확인되지 않아 실행 불가
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`: 저장소 루트에 `.spectral.yaml` 없음
- `gitleaks detect --source . --redact --exit-code 1`: 로컬 환경에 `gitleaks` 명령 없음

## 6. 후속 작업 후보

- PR 5 후속 범위에서 retry 정책을 실제로 구현할 경우 retry 대상 코드와 비대상 코드를 명확히 분리한다.
- 재시도 구현 시 `LLM_TIMEOUT`, `LLM_RATE_LIMIT`, 일부 `LLM_PROVIDER_ERROR`만 retry 후보로 두고, `LLM_PROVIDER_REQUEST_REJECTED`, `LLM_CONFIGURATION_ERROR`, `LLM_RESPONSE_INVALID`, `AUTO_VALIDATION_CONFIGURATION_ERROR`는 기본 비재시도 후보로 검토한다.
