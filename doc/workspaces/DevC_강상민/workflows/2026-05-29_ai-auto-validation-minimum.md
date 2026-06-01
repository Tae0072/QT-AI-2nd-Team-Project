# Workflow - 2026-05-29 ai-auto-validation-minimum

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-auto-validation-minimum` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | AI 구현 순서 8번: 자동 검증 최소 구현 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**` |

## 작업 목표

`EXPLANATION` AI 산출물에 한정해 최소 자동 검증 레이어를 추가한다. generation worker가 DeepSeek 응답을 바탕으로 `ai_generated_assets`를 저장한 직후 자동 검증을 실행하고, 결과를 `ai_validation_logs`에 `reviewerType=AUTO`로 기록한다.

이번 작업의 핵심은 검증 성공과 실패가 DB 로그로 남도록 만드는 것이다. 검증 통과 산출물은 아직 사용자 노출 상태로 전환하지 않고 `VALIDATING` 상태를 유지하며, 검증 실패 산출물은 `REJECTED`로 전환한다.

## 범위

- `EXPLANATION` 산출물만 자동 검증한다.
- 자동 검증 결과는 이번 범위에서 `PASSED` 또는 `REJECTED`만 생성한다.
- 활성 `AiValidationChecklistType.EXPLANATION` 체크리스트 버전을 찾아 `checklistVersionId`로 기록한다.
- asset 저장 후 자동 검증 로그를 같은 처리 흐름에서 남긴다.
- 검증 실패 시 `AiLogService.registerValidationLog(...)`를 통해 asset 상태를 `REJECTED`로 바꾼다.
- 기존 handler 단계의 JSON 파싱 실패, verseId 범위 오류, 필수 필드 오류는 asset 생성 전 실패로 유지하고 job을 `FAILED` 처리한다.
- 실제 DeepSeek API를 호출하지 않는 mock 기반 테스트를 추가한다.

## 제외 범위

- `NEEDS_REVIEW` 자동 생성 조건 정의.
- `SIMULATOR`, `QA_RESPONSE` 자동 검증.
- 관리자 승인 후 `verse_explanations`, `glossary_terms`, `simulator_clips` 사용자 노출본 반영.
- F-15 Q&A API와 `ai_qa_requests` 구현.
- 의미 검증, 신학 기준 검증, 외부 검증 agent, 평가 셋 연동.
- retry/backoff, timeout/429/5xx 세부 재시도 정책.
- OpenAPI 변경. 기존 system validation log 수동 등록 API 계약은 유지한다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiAutoValidationService.java` | EXPLANATION asset payload 최소 검증, 활성 checklist 조회, AUTO validation log 등록 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRunner.java` | asset 저장 직후 자동 검증 실행, 검증 실패 기록 후 job 종료 정책 연결 |
| Inspect/Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogService.java` | 기존 `registerValidationLog(...)`의 REJECTED 상태 전환 재사용 여부 확인 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiAutoValidationServiceTest.java` | 자동 검증 단위 테스트 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerIntegrationTest.java` | job 실행, asset 저장, validation log 생성 통합 흐름 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerTest.java` | runner 생성자와 성공 경로 mock 테스트 보강 |

## 구현 순서

1. `AiAutoValidationServiceTest`를 먼저 작성한다.
2. 정상 EXPLANATION payload가 `PASSED`, `reviewerType=AUTO`, 활성 EXPLANATION checklist id로 로그를 생성하는 테스트를 추가한다.
3. `explanations[]` 누락, 빈 배열, 필수 필드 누락, `sourceMetadata.verseIds[]`와 explanation verseId 불일치가 `REJECTED` 로그와 asset `REJECTED` 상태를 만드는 테스트를 추가한다.
4. `providerRawResponse`, `rawResponse`, `validationReferenceText`, `promptText` 같은 금지 필드가 payload에 있으면 `REJECTED` 로그를 남기는 테스트를 추가한다.
5. 활성 EXPLANATION checklist가 없으면 `BusinessException`을 던져 자동 검증 구성이 잘못되었음을 드러내는 테스트를 추가한다.
6. `AiAutoValidationService`를 구현한다.
7. checklist 조회는 `AiValidationChecklistVersionRepository.findByChecklistTypeAndStatus(EXPLANATION, ACTIVE)`를 사용한다.
8. 활성 checklist가 정확히 1건이어야 정상으로 처리한다. 0건이면 `INVALID_INPUT` 또는 `INTERNAL_ERROR` 계열 `BusinessException`으로 실패시킨다.
9. 자동 검증 로그의 공통 값은 `layer=1`, `reviewerType=AUTO`, `validationReferenceJobId=null`로 고정한다.
10. `checklistJson`에는 서버가 저장 가능한 최소 결과 JSON만 넣는다. 예: `{"validator":"AI_AUTO_VALIDATION_MINIMUM","result":"PASSED","rules":["EXPLANATION_SCHEMA","VERSE_SCOPE","FORBIDDEN_FIELDS"]}`.
11. `AiGenerationJobRunner`에 `AiAutoValidationService` 의존성을 추가한다.
12. runner의 성공 트랜잭션에서 asset 저장 후 저장된 asset id를 기준으로 자동 검증을 호출한다.
13. 자동 검증이 `PASSED`이면 asset은 `VALIDATING` 상태로 유지하고 job은 `SUCCEEDED`로 종료한다.
14. 자동 검증이 `REJECTED`이면 asset은 `REJECTED` 상태가 되고 job은 `SUCCEEDED`로 종료한다. 산출물 생성은 완료됐고 검증 결과가 반려로 기록됐기 때문이다.
15. 자동 검증 설정 오류나 저장 오류가 발생하면 job을 `FAILED`로 종료한다.
16. 기존 `ExplanationGenerationJobHandler`의 asset 생성 전 검증은 유지한다. invalid JSON, out-of-scope verseId 등은 asset/log 없이 job `FAILED`로 남긴다.
17. `AiGenerationJobRunnerIntegrationTest`에 활성 EXPLANATION checklist fixture를 추가한다.
18. 정상 LLM mock 응답이 asset 1건, validation log `PASSED` 1건, job `SUCCEEDED`를 만드는지 검증한다.
19. 자동 검증 실패를 유도하는 저장 payload 케이스를 추가해 asset `REJECTED`, validation log `REJECTED`, job `SUCCEEDED`를 검증한다.
20. 기존 invalid JSON 테스트는 asset과 validation log가 모두 없는 상태로 job `FAILED`를 유지하는지 갱신한다.
21. `AiGenerationJobRunnerTest`는 새 생성자 의존성에 맞게 mock `AiAutoValidationService`를 주입하고 기존 성공/실패 테스트 기대값을 보존한다.
22. 관련 테스트와 빌드를 실행하고, 실행하지 못한 명령은 report 또는 최종 응답에 사유를 남긴다.

## 테스트 보강 목록

| 테스트 파일 | 추가 또는 확인할 검증 |
| --- | --- |
| `AiAutoValidationServiceTest` | 정상 EXPLANATION payload는 `PASSED` AUTO validation log 생성 |
| `AiAutoValidationServiceTest` | schema/verse scope/금지 필드 위반은 `REJECTED` AUTO validation log 생성 |
| `AiAutoValidationServiceTest` | `REJECTED` 결과는 asset 상태를 `REJECTED`로 전환 |
| `AiAutoValidationServiceTest` | 활성 EXPLANATION checklist 누락은 명시적 예외 발생 |
| `AiGenerationJobRunnerIntegrationTest` | 정상 job 실행 후 asset `VALIDATING`, validation log `PASSED`, job `SUCCEEDED` |
| `AiGenerationJobRunnerIntegrationTest` | 자동 검증 실패 후 asset `REJECTED`, validation log `REJECTED`, job `SUCCEEDED` |
| `AiGenerationJobRunnerIntegrationTest` | asset 생성 전 LLM 응답 오류는 asset/log 없이 job `FAILED` |
| `AiGenerationJobRunnerTest` | runner mock 테스트가 자동 검증 호출 여부를 검증 |

## 수용 기준

- [ ] `EXPLANATION` job 성공 경로에서 `ai_generated_assets` 저장 후 `ai_validation_logs`가 자동 생성된다.
- [ ] 자동 검증 로그는 `reviewerType=AUTO`를 사용한다.
- [ ] 자동 검증 로그는 활성 EXPLANATION checklist version id를 기록한다.
- [ ] 자동 검증 통과 결과는 `PASSED`이고 asset은 `VALIDATING` 상태를 유지한다.
- [ ] 자동 검증 실패 결과는 `REJECTED`이고 asset은 `REJECTED` 상태로 전환된다.
- [ ] 이번 PR에서 자동 검증은 `NEEDS_REVIEW`를 만들지 않는다.
- [ ] `SIMULATOR`, `QA_RESPONSE`는 자동 검증 대상이 아니다.
- [ ] provider raw response, prompt 원문, validation reference 원문, secret/token/password 계열 값은 payload와 checklistJson에 저장하지 않는다.
- [ ] 사용자 API `/api/v1/ai/**` 구현은 추가하지 않는다.
- [ ] 다른 도메인의 `internal`, `web`, repository 타입을 직접 import하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- runner, 자동 검증 서비스, validation log 저장 정책이 같은 `domain.ai.internal` 흐름에 강하게 결합되어 있다.
- 테스트와 구현을 순서대로 맞춰야 하므로 병렬 작업보다 단일 agent의 TDD 진행이 안전하다.
- 같은 통합 테스트 파일을 여러 작업자가 동시에 수정할 가능성이 높다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 테스트 작성, 구현, 통합 테스트 갱신, 검증 명령 실행을 순차적으로 직접 수행한다.

## 검증 계획

- `.\gradlew.bat test --tests "*AiAutoValidationServiceTest"` in `qtai-server`
- `.\gradlew.bat test --tests "*AiGenerationJobRunnerTest" --tests "*AiGenerationJobRunnerIntegrationTest"` in `qtai-server`
- `.\gradlew.bat test --tests "*AiGenerationJob*"` in `qtai-server`
- `.\gradlew.bat build` in `qtai-server`
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`
- `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai`
- `rg -n "providerRawResponse|rawResponse|validationReferenceText|promptText|password|private key|token|secret" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai`
- `git diff --check`

`spectral` ruleset이나 `gitleaks` 실행 파일이 로컬에 없으면 실행하지 못한 이유를 명확히 기록한다.

## 다음 작업으로 남길 항목

- `NEEDS_REVIEW` 조건을 의미 검증/정책 검증 PR에서 정의한다.
- timeout, 429, 5xx, 검증 실패별 재시도 정책은 9번 작업에서 정리한다.
- 04:00 KST 배치와 fixed-delay worker 운영 방식은 10번 작업에서 확정한다.
- 관리자 승인 후 사용자 노출 테이블 반영은 별도 PR에서 구현한다.
- F-15 Q&A 구현 여부는 보류 상태를 유지하고 별도 workflow로 재검토한다.
