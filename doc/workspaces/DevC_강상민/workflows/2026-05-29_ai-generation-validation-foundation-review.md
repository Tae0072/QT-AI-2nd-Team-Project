# Workflow - 2026-05-29 ai-generation-validation-foundation-review

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-generation-validation-foundation-review` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | AI 구현 순서 점검표의 PR 2 항목 4~6: 생성 job 처리 흐름, 산출물 저장 흐름, 검증 로그 흐름 점검 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/05_시퀀스_다이어그램.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/23_도메인_용어사전.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-28_ai-implementation-order.md`, `doc/workspaces/DevC_강상민/reports/checkList.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**`, `qtai-server/apis/api-v1/openapi.yaml`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

PR 3에서 DeepSeek 호출과 generation job processor를 연결하기 전에, 이미 구현된 AI 생성/검증 기반 구조가 문서 기준과 충돌하지 않는지 점검한다. 대상은 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs`의 생성, 저장, 상태 전이, 시스템 API 접수 흐름이다.

이번 작업은 실행 파이프라인 구현이 아니라 구조 점검과 최소 보강이다. 불일치가 없으면 테스트와 리포트로 현재 기준을 고정하고, 불일치가 있으면 같은 PR 안에서 최소 변경으로 정합화한다.

## 범위

- `ai_generation_jobs` 생성과 상태 전이가 문서의 `QUEUED -> RUNNING -> SUCCEEDED | FAILED` 기준을 만족하는지 확인한다.
- `CreateAiGenerationJobUseCase`와 `SystemAiGenerationJobController`가 진행 중 job 중복 차단, `SYSTEM_BATCH` 요청 주체, `DAILY_QT_EXPLANATION`/`DAILY_QT_SIMULATOR` 매핑을 유지하는지 확인한다.
- `SUMMARY`/`GLOSSARY`는 독립 generation job type으로 열지 않고, 필요 시 `EXPLANATION` 산출물 계열로만 다루는 기존 결정과 충돌하지 않게 둔다.
- `ai_generated_assets` 저장 흐름이 항상 `VALIDATING`으로 시작하고, 요청자가 `APPROVED`, `REJECTED`, `HIDDEN`을 직접 지정하지 못하는지 확인한다.
- `QA_RESPONSE`는 enum과 공통 로그 모델의 호환성만 확인한다. PR 2의 시스템 사전 생성 산출물 접수 대상에는 포함하지 않는다.
- `ai_validation_logs`가 자동 검증 결과를 `reviewerType=AUTO`로 남길 수 있고, `PASSED`, `NEEDS_REVIEW`, `REJECTED`별 asset 상태 처리가 문서와 맞는지 확인한다.
- OpenAPI와 구현이 다르면 둘 중 하나를 기준 문서에 맞춰 정합화한다.
- 작업 후 DevC report를 작성해 점검 결과, 변경 내용, 검증 결과, 제외 범위, 남은 리스크를 남긴다.

## 제외 범위

- 실제 DeepSeek 호출과 LLM 응답 파싱은 구현하지 않는다.
- generation job processor, scheduler, worker, batch 실행 루프는 만들지 않는다.
- F-15 Q&A API, `AiController` 구현, `ai_qa_requests` 테이블/entity/repository는 만들지 않는다.
- `QA_RESPONSE`를 사용자 Q&A 저장 흐름으로 연결하지 않는다.
- 자동 검증 룰 엔진, 금지 표현 검사기, validation agent 실행 로직은 만들지 않는다.
- 관리자 승인 후 `verse_explanations`, `simulator_clips` 노출본 테이블에 연결하는 흐름은 구현하지 않는다.
- `inputHash` 컬럼, 서버 계산 helper, unique key 확장은 W2/MVP 제외 결정을 유지한다.
- `service_accounts` 기반 인증, 감사 로그 actorId 연결, 전역 `/api/v1/system/**` 보안 필터는 구현하지 않는다.
- prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 예시는 추가하지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Inspect/Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJob.java` | job 상태 전이와 active unique key 해제 기준 확인. 불일치 시 최소 수정 |
| Inspect/Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | generation job 생성, 중복 차단, prompt type 매핑, 재생성 대상 상태 확인 |
| Inspect/Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGeneratedAsset.java` | asset 초기 상태와 승인/반려/숨김 상태 전이 확인 |
| Inspect/Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogService.java` | asset 저장, validation log 저장, `REJECTED` 상태 전환 기준 확인 |
| Inspect/Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogUseCaseService.java` | system asset/log 접수 UseCase validation과 결과 status 매핑 확인 |
| Inspect/Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobController.java` | 시스템 job 접수 API의 authority, jobType, targetType 매핑 확인 |
| Inspect/Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiAssetController.java` | 시스템 asset 접수 API의 `VALIDATING` 강제와 `QA_RESPONSE` 제외 여부 확인 |
| Inspect/Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiValidationLogController.java` | 시스템 validation log 접수 API의 `AUTO` 로그 저장 가능 여부 확인 |
| Inspect/Modify | `qtai-server/apis/api-v1/openapi.yaml` | 구현과 API 계약의 enum, request, response 정합성 확인. 변경 시 테스트와 함께 수정 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobTest.java` | job 상태 전이와 terminal 상태 후 재전이 차단 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGeneratedAssetTest.java` | asset 상태 전이와 직접 승인/반려/숨김 경계 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogServiceTest.java` | asset 저장, validation log 저장, 결과별 asset 상태 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogUseCaseServiceTest.java` | UseCase command validation, enum parsing, result status 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` | generation job 중복 차단, unsupported job type, prompt type mismatch 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | 시스템 job API 권한, 매핑, 오류 응답 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiAssetControllerTest.java` | 시스템 asset API 권한, `VALIDATING` 강제, `QA_RESPONSE` 제외 기준 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiValidationLogControllerTest.java` | 시스템 validation log API 권한, `AUTO` 로그 등록, 오류 응답 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-05-29_ai-generation-validation-foundation-review_report.md` | 구현 결과, 점검 결과, 검증 결과, 제외 범위 기록 |

## 구현 순서

1. 기준 문서에서 F-02, F-14, 시스템 AI API, AI 사전 생성 배치, 검증 로그 상태 전이 기준을 다시 확인한다.
2. `AiGenerationJob`, `AiService`, `SystemAiGenerationJobController`를 읽고 현재 흐름을 표로 정리한다. 정리 항목은 job type, target type, prompt type, initial status, allowed transition, duplicate blocking, terminal cleanup이다.
3. `AiGeneratedAsset`, `AiLogService`, `SystemAiAssetController`를 읽고 asset 저장 흐름을 표로 정리한다. 정리 항목은 asset type, target type, initial status, allowed request status, approval status, rejection status, hide status이다.
4. `AiValidationLog`, `AiLogService.registerValidationLog(...)`, `SystemAiValidationLogController`를 읽고 validation log 저장 흐름을 표로 정리한다. 정리 항목은 reviewerType, result, checklistVersionId, validationReferenceJobId, asset status after log이다.
5. `AiController`, `RequestAiQaUseCase`, `ai_qa_requests` 관련 검색 결과를 확인해 F-15 Q&A가 아직 placeholder 범위인지 다시 검증한다.
6. `SystemAiGenerationJobControllerTest`에 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`, unsupported job type, non-`QT_PASSAGE` target, duplicate conflict 매핑이 충분한지 확인한다. 누락된 케이스가 있으면 추가한다.
7. `AiServiceTest`에 `SUMMARY`/`GLOSSARY`가 independent job으로 열리지 않는 검증과 prompt type mismatch 검증이 충분한지 확인한다. 누락된 케이스가 있으면 추가한다.
8. `SystemAiAssetControllerTest`에 요청 `status`가 없거나 `VALIDATING`일 때만 통과하고, `APPROVED`, `REJECTED`, `HIDDEN`은 `400 INVALID_INPUT`인 케이스가 있는지 확인한다.
9. `SystemAiAssetControllerTest` 또는 `AiLogUseCaseServiceTest`에 PR 2 범위에서 `QA_RESPONSE`를 시스템 사전 생성 산출물 접수 대상으로 열지 않는 검증을 추가한다. 구현 정책은 `QA_RESPONSE` 요청을 `400 INVALID_INPUT`으로 차단하는 것을 기본값으로 한다.
10. `AiLogServiceTest`에 `PASSED`와 `NEEDS_REVIEW`는 asset을 `VALIDATING`으로 유지하고, `REJECTED`는 asset을 `REJECTED`로 전환하는 검증이 충분한지 확인한다. 누락된 케이스가 있으면 추가한다.
11. `SystemAiValidationLogControllerTest`에 `reviewerType=AUTO`, nullable `validationReferenceJobId`, `checklistVersionId` 필수, `asset not found`, invalid transition 응답이 충분한지 확인한다.
12. 테스트가 실패하는 경우 문서 기준에 맞춰 production code를 최소 수정한다. 수정 우선순위는 domain entity/service, UseCase service, web controller, OpenAPI 순서로 판단한다.
13. OpenAPI의 `SystemAiGenerationJobRequest`, `SystemAiAssetRequest`, `SystemAiValidationLogRequest` enum과 구현 validation이 다르면 같은 PR에서 맞춘다.
14. 사용자 경로 `/api/v1/ai/**`에 asset/log 등록 또는 generation job 생성 API가 열리지 않았는지 `rg`로 확인한다.
15. AI 도메인이 다른 도메인의 `internal`, `web`, `repository` 타입을 직접 import하지 않는지 `rg`로 확인한다.
16. prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 계열 문자열이 추가되지 않았는지 `rg`로 확인한다.
17. 관련 테스트와 `build`를 실행한다.
18. 결과 report를 작성한다. report에는 변경 파일, 점검 결론, 실행한 검증 명령, 실패 또는 생략한 검증의 이유, PR 3으로 넘길 항목을 포함한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 또는 확인할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | `DAILY_QT_EXPLANATION -> EXPLANATION`, `DAILY_QT_SIMULATOR -> SIMULATOR`, unsupported job type 400, non-`QT_PASSAGE` target 400, `SYSTEM_BATCH` 권한 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` | `QUEUED`/`RUNNING` 진행 중 job 중복 차단, `SUMMARY`/`GLOSSARY` 독립 job 차단, prompt type mismatch 차단 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobTest.java` | `QUEUED -> RUNNING -> SUCCEEDED`, `QUEUED/RUNNING -> FAILED`, terminal 상태 이후 잘못된 재전이 차단 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiAssetControllerTest.java` | asset 요청 status가 없거나 `VALIDATING`인 경우만 허용, `APPROVED`/`REJECTED`/`HIDDEN` 차단, PR 2 범위의 `QA_RESPONSE` 차단 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogUseCaseServiceTest.java` | asset type과 target type enum validation, `QA_RESPONSE` 제외 정책이 service layer에서도 우회되지 않음 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGeneratedAssetTest.java` | `VALIDATING -> APPROVED | REJECTED | HIDDEN`, `APPROVED -> HIDDEN`, invalid transition 차단 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogServiceTest.java` | validation log `PASSED`/`NEEDS_REVIEW`는 asset `VALIDATING` 유지, `REJECTED`는 asset `REJECTED` 전환 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiValidationLogControllerTest.java` | `reviewerType=AUTO`, nullable `validationReferenceJobId`, `checklistVersionId` 필수, 401/403/404/409 응답 |
| `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | public UseCase/DTO record 필드가 OpenAPI와 충돌하지 않음 |

## 수용 기준

- [ ] `ai_generation_jobs`는 문서 기준 상태값 `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`만 사용한다.
- [ ] job 상태 전이는 `QUEUED -> RUNNING -> SUCCEEDED | FAILED`와 `QUEUED -> FAILED`만 허용한다.
- [ ] job 성공/실패 후 `activeUniqueKey`가 해제되어 같은 대상의 후속 job을 만들 수 있다.
- [ ] 진행 중인 동일 `jobType + targetType + targetId + promptVersionId` job이 있으면 새 job 생성은 `409 INVALID_STATUS_TRANSITION`으로 차단된다.
- [ ] 시스템 job API는 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`, `QT_PASSAGE`만 허용한다.
- [ ] `SUMMARY`와 `GLOSSARY`는 독립 generation job으로 열리지 않는다.
- [ ] 시스템 asset 등록 API는 산출물을 항상 `VALIDATING`으로 생성한다.
- [ ] 요청자가 `APPROVED`, `REJECTED`, `HIDDEN` 상태로 asset을 직접 만들 수 없다.
- [ ] PR 2 범위에서 `QA_RESPONSE`는 시스템 사전 생성 산출물 등록 대상으로 열리지 않는다.
- [ ] validation log는 `reviewerType=AUTO` 자동 검증 결과를 저장할 수 있다.
- [ ] validation result `PASSED`와 `NEEDS_REVIEW`는 asset을 사용자 노출 상태로 자동 승인하지 않는다.
- [ ] validation result `REJECTED`는 asset을 `REJECTED`로 전환한다.
- [ ] 사용자 API 경로 `/api/v1/ai/**`에 생성 job, asset 등록, validation log 등록 기능이 추가되지 않는다.
- [ ] prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 예시가 새로 저장되지 않는다.
- [ ] 작업 결과 report가 `doc/workspaces/DevC_강상민/reports/2026-05-29_ai-generation-validation-foundation-review_report.md`에 작성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 이번 작업은 새 기능 구현보다 기존 job, asset, validation log 흐름의 정합성 점검과 최소 보강이 중심이다.
- controller, UseCase, domain service, OpenAPI enum이 같은 정책 결정을 공유하므로 병렬 편집보다 한 흐름에서 순차 확인하는 편이 안전하다.
- `QA_RESPONSE` 제외 범위와 `SUMMARY`/`GLOSSARY` job 차단 기준은 작은 정책 차이가 API 계약을 바꿀 수 있어 메인 agent가 직접 통합 판단해야 한다.
- 테스트 보강과 production code 최소 수정이 같은 파일군에 걸쳐 있어 병렬 작업 시 중복 수정 가능성이 높다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 문서 기준 확인, 테스트 보강, 최소 구현 수정, OpenAPI 정합성 확인, 최종 검증, report 작성을 순서대로 직접 수행한다.

## 검증 계획

- `.\qtai-server\gradlew.bat -p qtai-server test --tests "*SystemAiGenerationJobControllerTest" --tests "*SystemAiAssetControllerTest" --tests "*SystemAiValidationLogControllerTest" --tests "*AiGenerationJobTest" --tests "*AiGeneratedAssetTest" --tests "*AiValidationLogTest" --tests "*AiLogServiceTest" --tests "*AiLogUseCaseServiceTest" --tests "*AiServiceTest" --tests "*AiUseCaseContractTest"`
- `.\qtai-server\gradlew.bat -p qtai-server test --tests "*Ai*"`
- `.\qtai-server\gradlew.bat -p qtai-server build`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`
- `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai`
- `rg -n "RequestMapping\(\"/api/v1/ai|PostMapping.*generation-jobs|PostMapping.*assets|PostMapping.*validation-logs" qtai-server/src/main/java/com/qtai/domain/ai/web`
- `rg -n "raw response|provider raw|password|private key|token|secret" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai qtai-server/apis/api-v1/openapi.yaml`
- `git diff --check`

`spectral` 또는 `gitleaks` 실행 파일이 현재 환경에 없으면 report에 실행 불가 사유를 명확히 기록한다. OpenAPI를 변경하지 않은 경우에도 구현 enum과 schema enum의 정합성을 확인하기 위해 Spectral 실행을 우선 시도한다.

## 후속 작업으로 남길 항목

- PR 3: generation job processor를 만들고 `QUEUED` job 실행, DeepSeek 호출, asset 저장까지 연결한다.
- PR 4: 자동 검증 최소 레이어를 구현하고 `ai_validation_logs` 생성 시점을 실제 검증 실행과 연결한다.
- F-15 Q&A: `ai_qa_requests` 설계와 사용자 Q&A API 구현 여부를 별도 workflow로 재검토한다.
- 관리자 승인: `APPROVED` 산출물을 `verse_explanations`, `simulator_clips` 사용자 노출 테이블로 연결하는 흐름을 별도 PR로 구현한다.
- 감사 로그: `SYSTEM_BATCH` actor와 관리자 재생성/승인/반려 이벤트를 audit 흐름에 연결한다.
