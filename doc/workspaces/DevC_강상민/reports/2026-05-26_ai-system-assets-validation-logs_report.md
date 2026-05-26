# Report - 2026-05-26 ai-system-assets-validation-logs

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-system-assets-validation-logs` |
| PR 대상 | `dev` |
| 실행 경로 | 직접 실행 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-26_ai-system-assets-validation-logs.md` |
| 관련 F-ID | F-02, F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-assets-validation-logs_report.md` |

## 작업 결과

시스템 배치가 AI 생성 산출물을 `ai_generated_assets`에 등록하고, 자동 검증 결과를 `ai_validation_logs`에 남길 수 있도록 시스템 API 접수 계층을 구현했다.

`POST /api/v1/system/ai/assets`와 `POST /api/v1/system/ai/validation-logs`는 컨트롤러에서 `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH` authority만 허용한다. 인증이 없으면 `401`, 권한이 부족하면 `403`으로 응답하며, 사용자 앱 경로(`/api/v1/ai/**`)에는 산출물 등록 또는 검증 로그 등록 기능을 추가하지 않았다.

문서 정합성에 맞춰 `ai_validation_logs.validation_reference_job_id`를 nullable FK로 반영했다. `RegisterAiValidationLogCommand`, `AiValidationLog`, `AiLogService.registerValidationLog(...)`, UseCase 구현체, Flyway migration, 테스트에 모두 `validationReferenceJobId` 흐름을 연결했다.

## 변경 요약

1. `SystemAiAssetController`를 추가해 산출물 등록 시스템 API를 구현했다.
2. `SystemAiValidationLogController`를 추가해 검증 로그 등록 시스템 API를 구현했다.
3. `SystemAiAuthentication` helper로 `SYSTEM_BATCH`/`ROLE_SYSTEM_BATCH` authority 검증을 공통화했다.
4. HTTP DTO의 `payloadJson`, `checklistJson`을 `JsonNode`로 받고 내부 command에는 compact JSON 문자열로 전달하도록 했다.
5. 산출물 등록 요청의 `status`는 없거나 `VALIDATING`일 때만 허용하고, 서버 저장 상태는 UseCase/도메인 흐름에서 `VALIDATING`으로 결정하도록 했다.
6. `AiLogUseCaseService`를 추가해 `RegisterAiGeneratedAssetUseCase`, `RegisterAiValidationLogUseCase` 실제 구현체를 제공했다.
7. `AiLogService.registerGeneratedAsset(...)`에서 generation job 존재 여부를 먼저 확인해 `AI_GENERATION_JOB_NOT_FOUND`로 응답 매핑될 수 있게 했다.
8. `AiValidationLog`에 nullable `validationReferenceJobId` 필드와 getter를 추가했다.
9. `V6__create_ai_generation_logging.sql`에 AI 생성 job, 산출물, 검증 참조 작업, 체크리스트 버전, 검증 로그 테이블과 FK/인덱스를 추가했다.
10. 컨트롤러, UseCase, Entity/service, API contract 테스트를 추가 또는 보강했다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiAssetController.java` | 산출물 등록 시스템 API 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiAssetRequest.java` | 산출물 등록 요청 DTO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiAssetResponse.java` | 산출물 등록 응답 DTO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiValidationLogController.java` | 검증 로그 등록 시스템 API 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiValidationLogRequest.java` | 검증 로그 등록 요청 DTO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiValidationLogResponse.java` | 검증 로그 등록 응답 DTO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiAuthentication.java` | 시스템 배치 authority 검증 helper 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/AiWebExceptionResponses.java` | AI web 예외 응답 매핑 helper 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogUseCaseService.java` | 산출물/검증 로그 등록 UseCase 구현체 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/RegisterAiValidationLogCommand.java` | nullable `validationReferenceJobId` 필드 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiValidationLog.java` | `validation_reference_job_id` 필드/getter 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogService.java` | generation job 확인, 검증 로그 `validationReferenceJobId` 전달 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiJsonStorageGuard.java` | 금지 필드 차단 메시지 정리 |
| `qtai-server/src/main/resources/db/migration/V6__create_ai_generation_logging.sql` | AI 생성/산출물/검증 로그 운영 DB migration 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiAssetControllerTest.java` | 산출물 API 인증, 매핑, 응답, 오류 테스트 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiValidationLogControllerTest.java` | 검증 로그 API 인증, 매핑, 응답, 오류 테스트 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogUseCaseServiceTest.java` | UseCase 구현체 위임/검증 테스트 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiValidationLogTest.java` | `validationReferenceJobId` nullable 저장 테스트 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogServiceTest.java` | generation job 확인, 검증 로그 상태/참조 id 테스트 보강 |
| `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | command 계약에 `validationReferenceJobId` 포함 검증 |

## 수용 기준 평가

| 수용 기준 | 상태 | 근거 |
| --- | --- | --- |
| `POST /api/v1/system/ai/assets`가 추가된다 | 충족 | `SystemAiAssetController` 추가 |
| `POST /api/v1/system/ai/validation-logs`가 추가된다 | 충족 | `SystemAiValidationLogController` 추가 |
| 두 API 모두 `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH` authority만 허용한다 | 충족 | `SystemAiAuthentication`, controller 테스트 |
| 산출물 등록 API는 `RegisterAiGeneratedAssetUseCase` 실제 구현체를 호출한다 | 충족 | controller와 `AiLogUseCaseService` 연결 |
| 검증 로그 등록 API는 `RegisterAiValidationLogUseCase` 실제 구현체를 호출한다 | 충족 | controller와 `AiLogUseCaseService` 연결 |
| 산출물 등록 성공 응답은 `assetId`, `status=VALIDATING` 중심의 최소 응답이다 | 충족 | `SystemAiAssetResponse`, controller 테스트 |
| 산출물 상태는 요청자가 임의로 `APPROVED`, `REJECTED`, `HIDDEN`으로 만들 수 없다 | 충족 | `status` 검증과 차단 테스트 |
| 검증 로그 등록은 `validationReferenceJobId`를 nullable로 받을 수 있다 | 충족 | DTO, command, controller 테스트 |
| `RegisterAiValidationLogCommand`, `AiValidationLog`, `AiLogService.registerValidationLog(...)`에 `validationReferenceJobId`가 반영된다 | 충족 | 코드와 테스트 반영 |
| `REJECTED` 검증 결과는 산출물 상태를 `REJECTED`로 전환한다 | 충족 | 기존 `AiLogService` 흐름 유지 및 테스트 |
| `PASSED`, `NEEDS_REVIEW` 검증 결과는 산출물 상태를 `VALIDATING`으로 유지한다 | 충족 | `AiLogServiceTest` |
| 운영 DB용 Flyway migration에 AI 생성/산출물/검증 로그 핵심 테이블이 추가된다 | 충족 | `V6__create_ai_generation_logging.sql` |
| migration의 `ai_validation_logs.validation_reference_job_id`는 nullable FK와 인덱스를 가진다 | 충족 | `validation_reference_job_id BIGINT`, FK, `idx_validation_reference_job` |
| migration의 `ai_validation_logs.checklist_version_id`는 체크리스트 버전 테이블 FK와 인덱스를 가진다 | 충족 | FK, `idx_validation_checklist_version` |
| 사용자 앱 경로에서 AI 산출물 등록 또는 검증 로그 등록을 시작할 수 없다 | 충족 | `/api/v1/system/ai/**`에만 API 추가, `rg` 검증 |
| AI 도메인은 다른 도메인의 `internal`, `web`, `repository` 타입을 직접 import하지 않는다 | 충족 | `rg` 검증 |
| prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 예시는 저장하지 않는다 | 충족 | guard 유지, 검색 검증 |
| `qtai-server/apis/api-v1/openapi.yaml`은 이번 작업에서 변경하지 않는다 | 충족 | diff 없음 |

## 검증 결과

저장소에 `gradlew`/로컬 `gradle` 실행 파일이 없어, 검증은 `%TEMP%/codex-gradle-8.10.2/gradle-8.10.2/bin/gradle.bat` 임시 Gradle 배포본으로 수행했다.

| 명령 | 결과 |
| --- | --- |
| `test --tests=*SystemAiAssetControllerTest --tests=*SystemAiValidationLogControllerTest --tests=*AiLogUseCaseServiceTest --tests=*AiValidationLogTest --tests=*AiLogServiceTest --tests=*AiUseCaseContractTest` | 성공 |
| `test --tests=*AiGeneratedAssetTest --tests=*AiValidationLogTest --tests=*AiUseCaseContractTest` | 성공 |
| `test --tests=*Ai*` | 성공 |
| `build` | 성공 |
| `git diff --check` | 성공, CRLF 경고만 출력 |
| `rg -n "^import .*domain\\.[a-z]+\\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |
| `rg -n -g '*.java' 'RequestMapping\\("/api/v1/ai' qtai-server/src/main/java/com/qtai/domain/ai/web` | 매치 없음 |
| `rg -n -g '*.java' 'PostMapping.*assets' qtai-server/src/main/java/com/qtai/domain/ai/web` | 매치 없음 |
| `rg -n -g '*.java' 'PostMapping.*validation-logs' qtai-server/src/main/java/com/qtai/domain/ai/web` | 매치 없음 |
| `rg -n "raw response\|provider raw\|password\|private key\|token" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai` | 매치 없음 |
| `git diff --name-only -- qtai-server/apis/api-v1/openapi.yaml` | 매치 없음 |

## 실행하지 않은 검증

| 명령 | 사유 |
| --- | --- |
| `./gradlew -p qtai-server ...` | 저장소 루트에 `gradlew`가 없고 로컬 `gradle`도 PATH에 없음. 임시 Gradle 배포본으로 동등 범위를 검증함 |
| `./gradlew -p qtai-server test jacocoTestReport` | 현재 Gradle tasks에 Jacoco task가 등록되어 있지 않음 |
| `./gradlew -p qtai-server jacocoTestCoverageVerification` | 현재 Gradle tasks에 Jacoco task가 등록되어 있지 않음 |
| `npx @stoplight/spectral-cli lint ...` | OpenAPI 갱신은 이번 workflow 제외 범위이며 `openapi.yaml`도 변경하지 않음 |
| `gitleaks detect --source . --redact --exit-code 1` | 현재 환경에서 `gitleaks` 실행 파일을 찾지 못함 |

## 후속 작업

1. `qtai-server/apis/api-v1/openapi.yaml`에 `POST /api/v1/system/ai/assets`, `POST /api/v1/system/ai/validation-logs`, `validationReferenceJobId` schema 반영
2. `service_accounts` 기반 서버 간 인증 필터와 전역 `/api/v1/system/**` 보안 설정
3. `POST /api/v1/system/validation-reference-jobs` 생성, 조회, 만료 API 구현
4. `ai_validation_checklist_versions` 관리 API 구현
5. 관리자 AI 로그 조회 API 구현
6. 승인된 산출물을 `verse_explanations`, `simulator_clips` 등 사용자 노출 테이블로 연결하는 승인 흐름 구현
7. 실제 DeepSeek 호출과 batch worker 실행 흐름 구현
8. 감사 로그의 `SYSTEM_BATCH` actor 연결 구현

