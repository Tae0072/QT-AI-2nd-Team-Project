# 2026-05-29 ai-auto-validation-minimum 작업 보고

## 개요

- 관련 F-ID: F-02, F-14
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-05-29_ai-auto-validation-minimum.md`
- 브랜치: `feature/ai-auto-validation-minimum`
- PR 대상: `dev`
- 실행 경로: workflow-spec-runner 직접 실행

## 작업 결과

`EXPLANATION` AI 산출물에 한정해 최소 자동 검증 레이어를 추가했다. generation runner가 DeepSeek 응답 기반 asset을 `ai_generated_assets`에 저장한 직후 자동 검증을 실행하고, 결과를 `ai_validation_logs`에 `reviewerType=AUTO`, `layer=1`로 기록한다.

자동 검증은 활성 `AiValidationChecklistType.EXPLANATION` 체크리스트 버전을 정확히 1건 조회해 `checklistVersionId`로 사용한다. 정상 payload는 `PASSED` 로그를 만들고 asset 상태를 `VALIDATING`으로 유지한다. schema, verse scope, 금지 필드 위반 payload는 `REJECTED` 로그를 만들고 기존 `AiLogService.registerValidationLog(...)` 상태 전이 정책을 통해 asset을 `REJECTED`로 전환한다.

이번 작업에서는 사용자 노출 상태 전환이나 관리자 승인 후 노출 테이블 반영을 구현하지 않았다. `NEEDS_REVIEW`, `SIMULATOR`, `QA_RESPONSE` 자동 검증도 제외 범위로 유지했다.

## 변경 내용

- `AiAutoValidationService`를 추가해 EXPLANATION payload 최소 검증과 AUTO validation log 생성을 담당하도록 했다.
- 자동 검증 결과 `checklistJson`은 서버가 저장 가능한 최소 결과 JSON만 저장하도록 했다.
- `AiGenerationJobRunner`에 자동 검증 서비스를 주입하고, EXPLANATION asset 저장 직후 자동 검증을 호출하도록 연결했다.
- 자동 검증 통과 시 job은 `SUCCEEDED`, asset은 `VALIDATING`을 유지한다.
- 자동 검증 실패 시 job은 `SUCCEEDED`, asset은 `REJECTED`로 종료한다.
- 활성 EXPLANATION checklist 누락 같은 자동 검증 설정 오류는 job `FAILED`로 종료되도록 했다.
- 기존 handler 단계의 invalid JSON, out-of-scope verseId 오류는 asset/log 없이 job `FAILED`로 유지했다.
- 단위 테스트와 JPA 통합 테스트를 보강해 success, reject, pre-asset failure 흐름을 검증했다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiAutoValidationService.java` | EXPLANATION payload 최소 자동 검증, 활성 checklist 조회, AUTO validation log 등록 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRunner.java` | asset 저장 직후 EXPLANATION 자동 검증 호출 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiAutoValidationServiceTest.java` | PASSED/REJECTED, 금지 필드, checklist 누락 단위 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerIntegrationTest.java` | job 실행 후 asset/log 생성, REJECTED 전환, pre-asset failure 유지 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerTest.java` | runner 자동 검증 호출과 설정 오류 시 job FAILED 검증 |
| `doc/workspaces/DevC_강상민/reports/2026-05-29_ai-auto-validation-minimum_report.md` | 작업 결과, 검증 결과, 남은 리스크 기록 |

## 수용 기준 확인

| 수용 기준 | 상태 | 근거 |
| --- | --- | --- |
| EXPLANATION job 성공 경로에서 asset 저장 후 validation log 자동 생성 | 충족 | `AiGenerationJobRunnerIntegrationTest` |
| 자동 검증 로그는 `reviewerType=AUTO` 사용 | 충족 | 단위/통합 테스트에서 검증 |
| 자동 검증 로그는 활성 EXPLANATION checklist version id 기록 | 충족 | `AiAutoValidationService`, 통합 fixture 검증 |
| 자동 검증 통과 결과는 `PASSED`이고 asset은 `VALIDATING` 유지 | 충족 | `AiAutoValidationServiceTest`, `AiGenerationJobRunnerIntegrationTest` |
| 자동 검증 실패 결과는 `REJECTED`이고 asset은 `REJECTED` 전환 | 충족 | `AiAutoValidationServiceTest`, `AiGenerationJobRunnerIntegrationTest` |
| 이번 PR에서 자동 검증은 `NEEDS_REVIEW`를 만들지 않음 | 충족 | 자동 검증 결과를 `PASSED`/`REJECTED`로 제한 |
| `SIMULATOR`, `QA_RESPONSE`는 자동 검증 대상 아님 | 충족 | runner는 EXPLANATION asset만 자동 검증 호출 |
| provider raw response, prompt 원문, validation reference 원문 계열 값 저장 없음 | 충족 | payload/checklistJson 부재 검증 및 저장값 최소화 |
| 사용자 API `/api/v1/ai/**` 구현 추가 없음 | 충족 | web/API 계약 변경 없음 |
| 다른 도메인의 `internal`, `web`, repository 직접 import 없음 | 충족 | 금지 import 검색 매칭 없음 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AiAutoValidationServiceTest"` (`qtai-server`) | 통과 |
| `.\gradlew.bat test --tests "*AiGenerationJobRunnerTest" --tests "*AiGenerationJobRunnerIntegrationTest"` (`qtai-server`) | 통과 |
| `.\gradlew.bat test --tests "*AiGenerationJob*"` (`qtai-server`) | 통과 |
| `.\gradlew.bat build` (`qtai-server`) | 통과 |
| `rg -n "^import .*domain\.[a-z]+\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | 매칭 없음 |
| `rg -n "providerRawResponse\|rawResponse\|validationReferenceText\|promptText\|password\|private key\|token\|secret" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai` | 매칭 있음. 기존/신규 테스트의 차단 필드명, `tokenUsage`, 부재 검증 문자열이며 저장 원문 값은 없음 |
| `git diff --check` | 통과. CRLF 변환 경고만 출력 |

## 미실행 또는 실패한 검증

| 명령 | 사유 |
| --- | --- |
| `.\gradlew.bat -p qtai-server test jacocoTestReport` | 루트에서 `.\gradlew.bat`가 없어 실행 불가. `qtai-server` 디렉터리에서 재시도했으나 `jacocoTestReport` task가 현재 Gradle 프로젝트에 없음 |
| `.\gradlew.bat jacocoTestCoverageVerification` (`qtai-server`) | `jacocoTestCoverageVerification` task가 현재 Gradle 프로젝트에 없음 |
| `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | 루트에 `.spectral.yaml`이 없어 `ENOENT` 실패 |
| `gitleaks detect --source . --redact --exit-code 1` | 현재 환경 PATH에서 `gitleaks` 실행 파일을 찾지 못함 |

## 제외 범위 준수

- `NEEDS_REVIEW` 자동 생성 조건은 정의하지 않았다.
- `SIMULATOR`, `QA_RESPONSE` 자동 검증은 구현하지 않았다.
- 관리자 승인 후 `verse_explanations`, `glossary_terms`, `simulator_clips` 반영은 구현하지 않았다.
- F-15 Q&A API와 `ai_qa_requests`는 구현하지 않았다.
- 의미 검증, 신학 기준 검증, 외부 검증 agent, 평가 셋 연동은 구현하지 않았다.
- retry/backoff, timeout/429/5xx 세부 정책은 구현하지 않았다.
- OpenAPI 계약은 변경하지 않았다.

## 남은 리스크

- 활성 EXPLANATION checklist가 0건 또는 2건 이상이면 자동 검증 설정 오류로 job이 `FAILED` 처리된다. 운영 배포 전 checklist 활성 상태 데이터 정합성 확인이 필요하다.
- 자동 검증은 최소 schema/scope/금지 필드 검증만 수행한다. 의미 검증과 신학 기준 검증은 후속 PR에서 별도 정책으로 보강해야 한다.
- 현재 저장소에는 JaCoCo task, Spectral ruleset, gitleaks 실행 파일이 없어 일부 품질 게이트 명령을 로컬에서 완료하지 못했다.

## 후속 항목

1. `NEEDS_REVIEW` 조건은 의미 검증/정책 검증 PR에서 정의한다.
2. timeout, 429, 5xx, 검증 실패별 재시도 정책은 후속 workflow에서 정리한다.
3. 04:00 KST 배치와 fixed-delay worker 운영 방식은 별도 작업에서 확정한다.
4. 관리자 승인 후 사용자 노출 테이블 반영은 별도 PR에서 구현한다.
5. F-15 Q&A 구현 여부는 별도 workflow로 재검토한다.
