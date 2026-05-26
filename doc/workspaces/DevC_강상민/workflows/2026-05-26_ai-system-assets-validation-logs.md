# Workflow — 2026-05-26 ai-system-assets-validation-logs

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-system-assets-validation-logs` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | W2 후속 작업 중 시스템 배치 AI 산출물 등록 API, 검증 로그 등록 API, `validationReferenceJobId` 정합화, 운영 DB용 AI 로그 테이블 migration 작성 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**`, `qtai-server/src/main/resources/db/migration/**` |

## 기준 문서

- `qtai-server/02_ERD_문서.md`
- `doc/프로젝트 문서/07_요구사항_정의서.md`
- `doc/프로젝트 문서/03_아키텍처_정의서.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/05_시퀀스_다이어그램.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `doc/프로젝트 문서/22_구현_저장소_반영_체크리스트.md`
- `doc/프로젝트 문서/23_도메인_용어사전.md`
- `doc/프로젝트 문서/25_기능_명세서.md`
- `CODE_CONVENTION.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-20_ai-generation-log-model.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-21_create-ai-generation-job-usecase-implementation.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-22_ai-system-generation-job-trigger-api.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-26_ai-prompt-version-id-mapping.md`

## 작업 목표

시스템 배치가 AI 생성 결과를 `ai_generated_assets`에 등록하고, 자동 검증 결과를 `ai_validation_logs`에 기록할 수 있는 시스템 API를 구현한다. 이번 작업은 실제 DeepSeek 호출이나 관리자 승인 노출본 생성이 아니라, 이미 생성된 batch 결과와 검증 결과를 서버의 표준 로그 테이블에 남기는 접수 계층을 닫는 작업이다.

문서 정합성상 `ai_validation_logs.validation_reference_job_id`는 `validation_reference_jobs.id`를 nullable로 참조해야 한다. 따라서 검증 로그 API, UseCase command, Entity, service helper, Flyway migration에 `validationReferenceJobId`를 함께 반영한다.

## 범위

- `POST /api/v1/system/ai/assets` 시스템 API를 추가한다.
- `POST /api/v1/system/ai/validation-logs` 시스템 API를 추가한다.
- 두 시스템 API는 컨트롤러에서 `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH` authority를 직접 확인한다.
- 인증 정보가 없거나 anonymous면 `401`, authority가 부족하면 `403`으로 응답한다.
- 산출물 등록 API는 `RegisterAiGeneratedAssetUseCase` 실제 구현체를 호출한다.
- 검증 로그 등록 API는 `RegisterAiValidationLogUseCase` 실제 구현체를 호출한다.
- UseCase 구현체는 `domain.ai.internal`에 두고 package-private `AiLogService`를 통해 저장 흐름을 재사용한다.
- `AiLogService.registerGeneratedAsset(...)`를 산출물 등록 UseCase에 연결한다.
- `AiLogService.registerValidationLog(...)`를 검증 로그 등록 UseCase에 연결한다.
- `RegisterAiValidationLogCommand`에 nullable `Long validationReferenceJobId`를 추가한다.
- `AiValidationLog`에 nullable `validation_reference_job_id` 컬럼과 getter를 추가한다.
- `AiLogService.registerValidationLog(...)` 파라미터에 nullable `validationReferenceJobId`를 추가한다.
- 검증 로그 결과가 `REJECTED`이면 현재 구현처럼 대상 산출물 상태를 `REJECTED`로 전환한다.
- 검증 로그 결과가 `PASSED` 또는 `NEEDS_REVIEW`이면 산출물 상태는 `VALIDATING`으로 유지한다.
- 산출물 등록 성공 응답은 `assetId`, `status`만 반환한다.
- 검증 로그 등록 성공 응답은 기존 `RegisterAiValidationLogResult` 기준으로 `validationLogId`, `result`, `assetStatus`를 반환한다.
- 요청의 `payloadJson`, `checklistJson`은 HTTP DTO에서 JSON 객체로 받고, 내부 command에는 compact JSON 문자열로 전달한다.
- 산출물 요청의 `status` 필드는 상태 변경 권한으로 사용하지 않는다. 요청에 포함된 경우 `VALIDATING`만 허용하고, 저장 상태는 서버가 `VALIDATING`으로 결정한다.
- 검증 로그의 `validationReferenceJobId`는 nullable이다. 참조 작업을 사용하지 않는 형식 검증, 일부 Q&A 자동 검증도 로그를 남길 수 있어야 한다.
- 운영 DB용 Flyway migration을 추가한다.
- migration에는 현재 AI 생성/검증 로그 흐름이 의존하는 테이블과 FK/인덱스를 포함한다.

## 제외 범위

- OpenAPI 갱신은 이번 작업에서 제외한다. `qtai-server/apis/api-v1/openapi.yaml` 반영은 별도 담당 작업으로 분리한다.
- 실제 DeepSeek 호출, batch worker 실행기, payload 생성 로직은 제외한다.
- 관리자 로그 조회 API는 제외한다.
- 관리자 승인, 승인본 사용자 노출 테이블 연결, `verse_explanations`, `simulator_clips` 반영은 제외한다.
- `service_accounts` 기반 서버 간 인증 필터와 전역 `/api/v1/system/**` Spring Security 설정은 제외한다.
- `/api/v1/ai/**` 사용자 경로에서 산출물 등록이나 검증 로그 등록을 시작하는 기능은 추가하지 않는다.
- `validation_reference_jobs` 생성, 조회, 만료 API 구현은 제외한다. 이번 작업은 검증 로그가 참조 작업 id를 nullable로 기록할 수 있게만 한다.
- `ai_validation_checklist_versions` 관리 API 구현은 제외한다. migration에는 FK 정합성에 필요한 테이블 구조만 포함한다.
- 감사 로그 저장 구현은 제외한다.
- prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 예시는 요청, 응답, 테스트 fixture, 로그에 포함하지 않는다.

## 정책 결정

| 구분 | 기준 |
| --- | --- |
| 산출물 등록 API | `POST /api/v1/system/ai/assets` |
| 검증 로그 등록 API | `POST /api/v1/system/ai/validation-logs` |
| 인증 | `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH` authority |
| 산출물 저장 상태 | 서버가 항상 `VALIDATING`으로 생성 |
| 산출물 등록 응답 | `{ "assetId": 500, "status": "VALIDATING" }` |
| 검증 로그 `validationReferenceJobId` | nullable |
| 검증 로그 `REJECTED` | 로그 저장 후 산출물 상태 `REJECTED` |
| 검증 로그 `PASSED` | 로그 저장 후 산출물 상태 `VALIDATING` 유지 |
| 검증 로그 `NEEDS_REVIEW` | 로그 저장 후 산출물 상태 `VALIDATING` 유지 |
| JSON 입력 처리 | web DTO는 `JsonNode`, UseCase command는 JSON 문자열 |
| OpenAPI | 이번 작업 제외, 별도 담당 작업 |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiAssetController.java` | `/api/v1/system/ai/assets` 인증, 요청 검증, JSON 직렬화, UseCase 호출, 최소 응답 반환 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiAssetRequest.java` | `generationJobId`, `assetType`, `targetType`, `targetId`, `payloadJson`, `sourceLabel`, optional `status` 요청 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiAssetResponse.java` | `assetId`, `status` 응답 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiValidationLogController.java` | `/api/v1/system/ai/validation-logs` 인증, 요청 검증, JSON 직렬화, UseCase 호출, 응답 반환 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiValidationLogRequest.java` | `aiAssetId`, `validationReferenceJobId`, `checklistVersionId`, `layer`, `result`, `checklistJson`, `reviewerType`, `errorMessage` 요청 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiValidationLogResponse.java` | `validationLogId`, `result`, `assetStatus` 응답 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogUseCaseService.java` | `RegisterAiGeneratedAssetUseCase`, `RegisterAiValidationLogUseCase` 구현체. `AiLogService`로 저장 위임 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/RegisterAiValidationLogCommand.java` | nullable `validationReferenceJobId` 필드 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiValidationLog.java` | `validation_reference_job_id` 컬럼, 생성자 인자, factory 인자, getter 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogService.java` | `registerValidationLog(...)`에 nullable `validationReferenceJobId` 전달, 결과 DTO에 필요한 asset status 반환 가능 구조 확인 |
| Create | `qtai-server/src/main/resources/db/migration/V6__create_ai_generation_logging.sql` | AI 생성/산출물/검증 로그 운영 DB 테이블, FK, 인덱스 추가 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiAssetControllerTest.java` | 산출물 API 인증, 매핑, 응답, 오류 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiValidationLogControllerTest.java` | 검증 로그 API 인증, `validationReferenceJobId` 매핑, 응답, 오류 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogUseCaseServiceTest.java` | UseCase 구현체가 command 검증 후 `AiLogService` 저장 흐름을 호출하는지 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiValidationLogTest.java` | `validationReferenceJobId` nullable/저장 getter와 기존 불변식 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogServiceTest.java` | `validationReferenceJobId` 저장 전달, `REJECTED` 상태 전환, `PASSED`/`NEEDS_REVIEW` 유지 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | command/result record 계약 변경 반영 |

## 구현 순서

1. `SystemAiAssetControllerTest`를 먼저 작성한다. `ROLE_SYSTEM_BATCH` 요청이 `202 Accepted`와 `assetId`, `status=VALIDATING`을 반환해야 한다.
2. 산출물 컨트롤러 성공 테스트에서 요청의 `payloadJson` JSON 객체가 `RegisterAiGeneratedAssetCommand.payloadJson` compact JSON 문자열로 전달되는지 검증한다.
3. 산출물 컨트롤러 성공 테스트에서 요청 헤더나 body의 actor 값을 믿지 않고 서버가 `createdAt`을 fixed clock 기준으로 command에 넣는지 검증한다.
4. 산출물 컨트롤러 테스트에 `SYSTEM_BATCH` authority 허용, 인증 없음 `401`, `ROLE_USER` 권한 부족 `403` 케이스를 추가한다.
5. 산출물 컨트롤러 테스트에 `status`가 없거나 `VALIDATING`이면 허용하고, `APPROVED`, `REJECTED`, `HIDDEN`이면 `400`으로 차단하는 케이스를 추가한다.
6. 테스트를 실행해 컨트롤러가 없어 실패하는 것을 확인한다.
7. `SystemAiAssetRequest`, `SystemAiAssetResponse`, `SystemAiAssetController`를 추가한다.
8. `SystemAiAssetController`는 `Authentication` 인자를 우선 사용하고 없으면 `SecurityContextHolder`에서 인증 정보를 읽는다.
9. `SystemAiAssetController`는 `RegisterAiGeneratedAssetUseCase`를 호출하고, 결과를 `SystemAiAssetResponse`로 감싸 `ApiResponse.success(...)`로 반환한다.
10. `SystemAiValidationLogControllerTest`를 먼저 작성한다. `ROLE_SYSTEM_BATCH` 요청이 `202 Accepted`와 `validationLogId`, `result`, `assetStatus`를 반환해야 한다.
11. 검증 로그 컨트롤러 성공 테스트에서 `aiAssetId`가 command의 `assetId`로 매핑되는지 검증한다.
12. 검증 로그 컨트롤러 성공 테스트에서 nullable `validationReferenceJobId`가 command에 그대로 전달되는지 검증한다.
13. 검증 로그 컨트롤러 테스트에 `validationReferenceJobId`가 없는 요청도 정상 처리되는지 추가한다.
14. 검증 로그 컨트롤러 테스트에 `checklistJson` JSON 객체가 command의 JSON 문자열로 전달되는지 검증한다.
15. 검증 로그 컨트롤러 테스트에 인증 없음 `401`, authority 부족 `403`, 필수값 누락 `400`, asset not found `404`, invalid transition `409` 매핑을 추가한다.
16. 테스트를 실행해 컨트롤러가 없어 실패하는 것을 확인한다.
17. `SystemAiValidationLogRequest`, `SystemAiValidationLogResponse`, `SystemAiValidationLogController`를 추가한다.
18. 두 컨트롤러의 `SYSTEM_BATCH` authority 확인 로직은 중복이 크면 `domain.ai.web` package-private helper로 분리한다. 분리 시 helper는 web DTO와 Spring Security 타입만 다룬다.
19. `AiLogUseCaseServiceTest`를 작성한다. 정상 산출물 command가 `AiLogService.registerGeneratedAsset(...)`로 전달되고 result가 `assetId`, `status`를 반환해야 한다.
20. `AiLogUseCaseServiceTest`에 정상 검증 로그 command가 `validationReferenceJobId`를 포함해 `AiLogService.registerValidationLog(...)`로 전달되는지 검증한다.
21. `AiLogUseCaseService`를 추가해 `RegisterAiGeneratedAssetUseCase`, `RegisterAiValidationLogUseCase`를 구현한다.
22. `AiLogUseCaseService`는 command null, enum 문자열, 양수 id, layer, `createdAt`을 검증하고 잘못된 값은 `BusinessException(ErrorCode.INVALID_INPUT)`으로 변환한다.
23. `AiValidationLogTest`를 먼저 수정한다. `validationReferenceJobId`가 있는 로그와 null 로그가 모두 생성 가능한지 검증한다.
24. `AiValidationLog`에 `validationReferenceJobId` 필드, 컬럼, 생성자 인자, factory 인자, getter를 추가한다.
25. `AiLogServiceTest`를 먼저 수정한다. `registerValidationLog(...)` 호출 시 `validationReferenceJobId`가 저장된 log에 남아야 한다.
26. `AiLogService.registerValidationLog(...)` 시그니처에 nullable `validationReferenceJobId`를 추가하고 `AiValidationLog.create(...)`로 전달한다.
27. `RegisterAiValidationLogCommand`에 nullable `validationReferenceJobId`를 추가하고 관련 테스트 기대값을 갱신한다.
28. `RegisterAiValidationLogResult.assetStatus`가 `PASSED`/`NEEDS_REVIEW`에서 `VALIDATING`, `REJECTED`에서 `REJECTED`를 반환하도록 UseCase 구현에서 저장 후 asset 상태를 읽는 구조를 확정한다.
29. `V6__create_ai_generation_logging.sql`을 추가한다.
30. migration에는 `ai_prompt_versions`, `ai_generation_jobs`, `ai_generated_assets`, `validation_reference_jobs`, `ai_validation_checklist_versions`, `ai_validation_logs`를 생성한다.
31. migration의 `ai_validation_logs`에는 `validation_reference_job_id BIGINT NULL`, `checklist_version_id BIGINT`, `idx_validation_reference_job`, `idx_validation_checklist_version`을 포함한다.
32. migration FK는 `ai_generation_jobs.prompt_version_id -> ai_prompt_versions.id`, `ai_generated_assets.generation_job_id -> ai_generation_jobs.id`, `ai_validation_logs.ai_asset_id -> ai_generated_assets.id`, `ai_validation_logs.validation_reference_job_id -> validation_reference_jobs.id`, `ai_validation_logs.checklist_version_id -> ai_validation_checklist_versions.id`를 포함한다.
33. `AiUseCaseContractTest`에서 `RegisterAiValidationLogCommand` 필드 변경을 반영한다.
34. 전체 AI 테스트와 build를 실행한다.
35. `rg`로 사용자 경로(`/api/v1/ai/**`)에 산출물 등록 또는 검증 로그 등록 API가 추가되지 않았는지 확인한다.
36. `rg`로 AI 도메인에서 다른 도메인의 `internal`, `web`, `repository` 타입을 직접 import하지 않았는지 확인한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiAssetControllerTest.java` | `ROLE_SYSTEM_BATCH` 요청은 `202 Accepted`, `assetId`, `status=VALIDATING`을 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiAssetControllerTest.java` | `SYSTEM_BATCH` authority도 허용한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiAssetControllerTest.java` | 인증 없음은 `401`, 권한 부족은 `403`이고 UseCase를 호출하지 않는다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiAssetControllerTest.java` | `payloadJson` JSON 객체가 command JSON 문자열로 전달된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiAssetControllerTest.java` | `status`가 `VALIDATING` 외 값이면 `400`이고 UseCase를 호출하지 않는다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiAssetControllerTest.java` | UseCase가 `AI_GENERATION_JOB_NOT_FOUND`를 던지면 `404`로 응답한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiValidationLogControllerTest.java` | `ROLE_SYSTEM_BATCH` 요청은 `202 Accepted`, `validationLogId`, `result`, `assetStatus`를 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiValidationLogControllerTest.java` | `aiAssetId`가 command의 `assetId`로 매핑된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiValidationLogControllerTest.java` | `validationReferenceJobId`가 있는 요청과 없는 요청이 모두 command로 전달된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiValidationLogControllerTest.java` | `checklistJson` JSON 객체가 command JSON 문자열로 전달된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiValidationLogControllerTest.java` | 인증 없음은 `401`, 권한 부족은 `403`이고 UseCase를 호출하지 않는다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiValidationLogControllerTest.java` | UseCase가 `AI_ASSET_NOT_FOUND`를 던지면 `404`로 응답한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiValidationLogControllerTest.java` | UseCase가 `INVALID_STATUS_TRANSITION` 또는 상태 전이 계열 예외를 던지면 `409`로 응답한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogUseCaseServiceTest.java` | 산출물 command가 `AiLogService.registerGeneratedAsset(...)`로 위임되고 result가 `assetId`, `status`를 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogUseCaseServiceTest.java` | 검증 로그 command가 `validationReferenceJobId`를 포함해 `AiLogService.registerValidationLog(...)`로 위임된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogUseCaseServiceTest.java` | 잘못된 enum 문자열, 양수가 아닌 id, null `createdAt`은 `INVALID_INPUT`으로 차단된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiValidationLogTest.java` | `validationReferenceJobId`가 있는 로그와 null 로그가 모두 생성된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogServiceTest.java` | `validationReferenceJobId`가 저장된 검증 로그에 남는다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogServiceTest.java` | `REJECTED`는 산출물을 `REJECTED`로 전환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogServiceTest.java` | `PASSED`, `NEEDS_REVIEW`는 산출물 상태를 `VALIDATING`으로 유지한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | `RegisterAiValidationLogCommand`가 `validationReferenceJobId` nullable 필드를 포함한다 |

## 수용 기준

- [ ] `POST /api/v1/system/ai/assets`가 추가된다.
- [ ] `POST /api/v1/system/ai/validation-logs`가 추가된다.
- [ ] 두 API 모두 `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH` authority만 허용한다.
- [ ] 산출물 등록 API는 `RegisterAiGeneratedAssetUseCase` 실제 구현체를 호출한다.
- [ ] 검증 로그 등록 API는 `RegisterAiValidationLogUseCase` 실제 구현체를 호출한다.
- [ ] 산출물 등록 성공 응답은 `assetId`, `status=VALIDATING` 중심의 최소 응답이다.
- [ ] 산출물 상태는 요청자가 임의로 `APPROVED`, `REJECTED`, `HIDDEN`으로 만들 수 없다.
- [ ] 검증 로그 등록은 `validationReferenceJobId`를 nullable로 받을 수 있다.
- [ ] `RegisterAiValidationLogCommand`, `AiValidationLog`, `AiLogService.registerValidationLog(...)`에 `validationReferenceJobId`가 반영된다.
- [ ] `REJECTED` 검증 결과는 산출물 상태를 `REJECTED`로 전환한다.
- [ ] `PASSED`, `NEEDS_REVIEW` 검증 결과는 산출물 상태를 `VALIDATING`으로 유지한다.
- [ ] 운영 DB용 Flyway migration에 AI 생성/산출물/검증 로그 핵심 테이블이 추가된다.
- [ ] migration의 `ai_validation_logs.validation_reference_job_id`는 nullable FK와 인덱스를 가진다.
- [ ] migration의 `ai_validation_logs.checklist_version_id`는 체크리스트 버전 테이블 FK와 인덱스를 가진다.
- [ ] 사용자 앱 경로에서 AI 산출물 등록 또는 검증 로그 등록을 시작할 수 없다.
- [ ] AI 도메인은 다른 도메인의 `internal`, `web`, `repository` 타입을 직접 import하지 않는다.
- [ ] prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 예시는 저장하지 않는다.
- [ ] `qtai-server/apis/api-v1/openapi.yaml`은 이번 작업에서 변경하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 컨트롤러, UseCase 구현체, `AiLogService`, `AiValidationLog`, migration이 같은 저장 계약에 강하게 연결되어 있다.
- `validationReferenceJobId` 시그니처 변경은 command, Entity, service helper, 테스트를 순서대로 맞춰야 하므로 병렬 편집 시 충돌 가능성이 높다.
- OpenAPI 갱신이 제외되어 문서 작업 병렬화 이점이 작다.
- 테스트-first 순서를 지키며 실패 확인 후 구현하는 편이 회귀 위험을 줄인다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 controller 테스트, UseCase 테스트, Entity/service 테스트, 구현, migration, 최종 검증을 순서대로 직접 수행한다.

## 검증 계획

- `./gradlew -p qtai-server test --tests "*SystemAiAssetControllerTest"`
- `./gradlew -p qtai-server test --tests "*SystemAiValidationLogControllerTest"`
- `./gradlew -p qtai-server test --tests "*AiLogUseCaseServiceTest"`
- `./gradlew -p qtai-server test --tests "*AiValidationLogTest"`
- `./gradlew -p qtai-server test --tests "*AiLogServiceTest"`
- `./gradlew -p qtai-server test --tests "*AiUseCaseContractTest"`
- `./gradlew -p qtai-server test --tests "*Ai*"`
- `./gradlew -p qtai-server build`
- `rg -n "^import .*domain\\.[a-z]+\\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai`
- `rg -n "RequestMapping\\(\\\"/api/v1/ai|PostMapping.*assets|PostMapping.*validation-logs" qtai-server/src/main/java/com/qtai/domain/ai/web`
- `rg -n "raw response|provider raw|password|private key|token" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai`
- OpenAPI 파일을 변경하지 않으므로 Spectral lint는 이번 작업 검증에서 제외한다.
- 환경에 `gitleaks` 실행 파일이 있으면 `gitleaks detect --source . --redact --exit-code 1`을 실행한다.

## 후속 작업으로 남길 항목

- `qtai-server/apis/api-v1/openapi.yaml`에 `POST /api/v1/system/ai/assets`, `POST /api/v1/system/ai/validation-logs`, `validationReferenceJobId` schema 반영
- `service_accounts` 기반 서버 간 인증 필터와 전역 `/api/v1/system/**` 보안 설정
- `POST /api/v1/system/validation-reference-jobs` 생성, 조회, 만료 API 구현
- `ai_validation_checklist_versions` 관리 API 구현
- 관리자 AI 로그 조회 API 구현
- 승인된 산출물을 `verse_explanations`, `simulator_clips` 등 사용자 노출 테이블로 연결하는 승인 흐름 구현
- 실제 DeepSeek 호출과 batch worker 실행 흐름 구현
- 감사 로그의 `SYSTEM_BATCH` actor 연결 구현
