# Workflow — 2026-05-28 ai-validation-reference-jobs

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-validation-reference-jobs` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14 |
| 트리거 | W2 스케줄에서 `ai_validation_checklist_versions` 관리 API가 완료되어 다음 작업으로 `validation_reference_jobs` 생성/조회/만료 API를 진행 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**`, `qtai-server/apis/api-v1/openapi.yaml`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/workspaces/DevC_강상민/reports/**` |

## 기준 문서

- `doc/workspaces/DevC_강상민/강상민_2W_스케줄.md`
- `doc/workspaces/DevC_강상민/reports/checkList.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-26_ai-system-assets-validation-logs.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-27_ai-validation-checklist-versions-admin-api.md`
- `doc/프로젝트 문서/07_요구사항_정의서.md`
- `doc/프로젝트 문서/03_아키텍처_정의서.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `doc/프로젝트 문서/23_도메인_용어사전.md`
- `doc/프로젝트 문서/25_기능_명세서.md`
- `qtai-server/02_ERD_문서.md`
- `CODE_CONVENTION.md`

## 작업 목표

검증 agent가 임시 주석/참조 자료를 사용할 때, 서버가 참조 작업의 메타데이터와 상태를 추적할 수 있도록 `validation_reference_jobs` 생성, 단건 조회, 만료 API를 구현한다. 이 작업은 F-14의 AI 산출물 품질 관리 흐름에서 `ai_validation_logs.validation_reference_job_id`가 실제로 참조할 수 있는 작업 레지스트리를 완성한다.

이번 PR은 검증 참조 원문 전체를 서버 API 응답, 로그, 테스트 fixture, OpenAPI 예시에 노출하지 않는다. 서버는 원문을 정규화 저장하는 저장소가 아니라 임시 파일/색인 위치와 삭제 예정 상태를 관리하는 레지스트리 역할만 한다.

## 범위

- `POST /api/v1/system/validation-reference-jobs` 생성 API를 구현한다.
- `GET /api/v1/system/validation-reference-jobs/{jobId}` 단건 조회 API를 구현한다.
- `POST /api/v1/system/validation-reference-jobs/{jobId}/expire` 만료 API를 구현한다.
- 모든 API는 `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH` authority만 허용한다.
- 인증 없음은 `401 UNAUTHORIZED`, 권한 부족은 `403 FORBIDDEN`으로 응답한다.
- `ROLE_ADMIN + ADMIN_ROLE_CONTENT_CREATOR`는 이번 PR에서 직접 호출 권한으로 만들지 않는다.
- 생성 요청은 `sourceName`, `sourceFileName`, `sourceFileHash`, nullable `storageUri`, nullable `indexStorageUri`, nullable `expiresAt`을 받는다.
- `sourceName`, `sourceFileName`, `sourceFileHash`는 공백이 아닌 값이어야 하며 DB 길이 제한을 넘기지 않는다.
- `storageUri`, `indexStorageUri`는 선택 값이며 DB 길이 제한을 넘기지 않는다.
- 생성 시 상태는 항상 `ACTIVE`로 시작한다. 요청자가 `status`를 지정할 수 있는 필드는 만들지 않는다.
- 조회 응답은 `id`, `sourceName`, `sourceFileName`, `status`, `expiresAt`, `deletedAt`, `createdAt`, `updatedAt`을 반환한다.
- `sourceFileHash`, `storageUri`, `indexStorageUri`는 저장은 하되 기본 응답에는 포함하지 않는다. 참조 원문 접근 위치가 일반 응답 표면으로 확장되는 것을 막기 위한 결정이다.
- 없는 job id는 `404 VALIDATION_REFERENCE_JOB_NOT_FOUND`로 응답한다.
- 만료는 `ACTIVE -> EXPIRED` 전이만 허용한다.
- 이미 `EXPIRED`이거나 `DELETED`인 작업의 만료 요청은 `409 INVALID_STATUS_TRANSITION`으로 응답한다.
- 만료 API는 `deletedAt`을 설정하지 않는다. `deletedAt`은 실제 파일/색인 삭제가 완료되는 별도 작업에서 사용하는 값으로 남긴다.
- 생성과 만료는 기존 `WriteAuditLogUseCase`를 사용해 최소 감사 로그를 남긴다.
- 감사 로그는 `actorType=SYSTEM_BATCH`, `actorLabel=SYSTEM_BATCH`, `actionType=VALIDATION_REFERENCE_JOB_CREATE` 또는 `VALIDATION_REFERENCE_JOB_EXPIRE`, `targetType=VALIDATION_REFERENCE_JOB`로 기록한다.
- service account id가 아직 없으므로 이번 PR의 감사 로그 `actorId`는 nullable로 둔다.
- 감사 로그 snapshot에는 `id`, `sourceName`, `sourceFileName`, `status`, `expiresAt`, `deletedAt`, `timestamp`만 포함한다. 원문, `storageUri`, `indexStorageUri`, secret, token, password 계열 값은 포함하지 않는다.
- `qtai-server/apis/api-v1/openapi.yaml`에 3개 API path, request, response, error schema를 추가한다.
- `doc/프로젝트 문서/04_API_명세서.md` 7.4의 권한 표현을 구현 범위에 맞춰 `SYSTEM_BATCH` 기준으로 정리하고, `CONTENT_CREATOR` 직접 접근은 제외한다고 명시한다.
- 작업 후 DevC report를 작성해 변경 내용, 검증 결과, 제외 범위, 잔여 리스크를 남긴다.

## 제외 범위

- 목록 조회 API는 구현하지 않는다. 이번 범위의 조회는 `GET /api/v1/system/validation-reference-jobs/{jobId}` 단건 조회다.
- `CONTENT_CREATOR` 직접 접근 권한은 구현하지 않는다.
- service account credential 저장 방식, 토큰 포맷, 검증 필터, `/api/v1/system/**` 전역 보안 설정은 구현하지 않는다.
- `service_accounts` migration 추가는 구현하지 않는다.
- 임시 파일 또는 색인의 실제 삭제, 외부 저장소 client, 삭제 배치, `DELETED` 상태 전이는 구현하지 않는다.
- `commentary_sources`, `commentary_materials`, `commentary_material_verses`와 연결되는 영구 주석 자료 관리 API는 구현하지 않는다.
- 검증 참조 원문 전체, 주석 본문, PDF 본문, 금지 번역본 본문을 DB, API 응답, 로그, fixture, OpenAPI example에 넣지 않는다.
- `ai_validation_logs` 등록 흐름은 이미 nullable `validationReferenceJobId`를 받으므로 이번 PR에서 다시 변경하지 않는다.
- AI 생성, 검증 실행, DeepSeek 호출, 관리자 산출물 승인/반려 흐름은 변경하지 않는다.
- `inputHash` 컬럼, 서버 계산, unique key 확장은 이번 workflow와 무관하므로 구현하지 않는다.

## 주요 계약 결정

| 구분 | 결정 |
| --- | --- |
| 생성 API | `POST /api/v1/system/validation-reference-jobs` |
| 조회 API | `GET /api/v1/system/validation-reference-jobs/{jobId}` |
| 만료 API | `POST /api/v1/system/validation-reference-jobs/{jobId}/expire` |
| 권한 | `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH` |
| CONTENT_CREATOR | 직접 호출 권한 제외 |
| 생성 HTTP status | `201 Created` |
| 조회 HTTP status | `200 OK` |
| 만료 HTTP status | `200 OK` |
| 상태 값 | `ACTIVE`, `EXPIRED`, `DELETED` |
| 생성 상태 | 항상 `ACTIVE` |
| 만료 전이 | `ACTIVE -> EXPIRED`만 허용 |
| 없는 id | `VALIDATION_REFERENCE_JOB_NOT_FOUND`로 404 |
| 잘못된 상태 전이 | `INVALID_STATUS_TRANSITION`으로 409 |
| 응답 노출 | 원문, `storageUri`, `indexStorageUri`, `sourceFileHash` 미노출 |
| 감사 로그 | 생성/만료만 기록, 조회는 기록하지 않음 |
| 실제 삭제 | 이번 PR에서 수행하지 않음 |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/CreateValidationReferenceJobUseCase.java` | 검증 참조 작업 생성 UseCase interface |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/GetValidationReferenceJobUseCase.java` | 검증 참조 작업 단건 조회 UseCase interface |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/ExpireValidationReferenceJobUseCase.java` | 검증 참조 작업 만료 UseCase interface |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/CreateValidationReferenceJobCommand.java` | 생성 command DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/GetValidationReferenceJobQuery.java` | 단건 조회 query DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/ExpireValidationReferenceJobCommand.java` | 만료 command DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/ValidationReferenceJobResponse.java` | 생성/조회/만료 공통 응답 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/ValidationReferenceJob.java` | `validation_reference_jobs` entity와 상태 전이 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/ValidationReferenceJobStatus.java` | `ACTIVE`, `EXPIRED`, `DELETED` enum |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/ValidationReferenceJobRepository.java` | JPA repository |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/ValidationReferenceJobService.java` | 생성, 조회, 만료, 감사 로그 기록 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemValidationReferenceJobController.java` | 시스템 HTTP API 3종, 인증, 응답 envelope |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemValidationReferenceJobRequest.java` | 생성 request DTO와 bean validation |
| Modify | `qtai-server/src/main/java/com/qtai/common/exception/ErrorCode.java` | `VALIDATION_REFERENCE_JOB_NOT_FOUND` 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/AiWebExceptionResponses.java` | 신규 not found error mapping |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | validation reference jobs path/schema/error 계약 추가 |
| Modify | `doc/프로젝트 문서/04_API_명세서.md` | 7.4 권한과 응답 필드 계약 정리 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-05-28_ai-validation-reference-jobs_report.md` | 구현 결과, 검증 결과, 제외 범위 기록 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemValidationReferenceJobControllerTest.java` | HTTP API, 인증/권한, request mapping, response/error mapping |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/ValidationReferenceJobTest.java` | entity 생성, 상태 전이, 불변식 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/ValidationReferenceJobServiceTest.java` | 생성/조회/만료, not found, 상태 전이 실패, 감사 로그 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/ValidationReferenceJobRepositoryTest.java` | 저장 매핑, enum 저장, nullable 필드 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | 신규 UseCase DTO 계약과 원문 미노출 검증 |

## 구현 순서

1. `SystemValidationReferenceJobControllerTest`에 생성 성공 테스트를 먼저 추가한다. `ROLE_SYSTEM_BATCH` 요청이 `201 Created`와 `ValidationReferenceJobResponse`를 반환하고, request 필드가 `CreateValidationReferenceJobCommand`로 전달되는지 검증한다.
2. 생성 API 권한 테스트를 추가한다. 인증 없음은 `401`, `ROLE_USER`는 `403`, `ROLE_ADMIN + ADMIN_ROLE_CONTENT_CREATOR`도 `403`, `SYSTEM_BATCH`와 `ROLE_SYSTEM_BATCH`는 허용되어야 한다.
3. 생성 API request validation 테스트를 추가한다. `sourceName`, `sourceFileName`, `sourceFileHash` 누락 또는 공백은 `400 INVALID_INPUT`이어야 한다.
4. 조회 성공 테스트를 추가한다. `GET /api/v1/system/validation-reference-jobs/{jobId}`가 UseCase query로 id를 전달하고 `200 OK`를 반환해야 한다.
5. 만료 성공 테스트를 추가한다. `POST /api/v1/system/validation-reference-jobs/{jobId}/expire`가 command로 id를 전달하고 `200 OK`, `status=EXPIRED`를 반환해야 한다.
6. not found와 invalid status transition controller 테스트를 추가한다. `VALIDATION_REFERENCE_JOB_NOT_FOUND`는 `404`, `INVALID_STATUS_TRANSITION`은 `409`로 매핑되어야 한다.
7. controller 테스트가 실패하는 것을 확인한 뒤 신규 UseCase interface와 DTO record를 생성한다.
8. `SystemValidationReferenceJobRequest`와 `SystemValidationReferenceJobController`를 구현한다. 인증은 기존 `SystemAiAuthentication.requireSystemBatch(...)`를 재사용한다.
9. `ValidationReferenceJobTest`를 작성한다. 생성 시 필수 값 검증, `ACTIVE` 초기 상태, nullable URI/expiresAt 허용, `expire()` 호출 시 `EXPIRED`와 `updatedAt` 반영을 검증한다.
10. `ValidationReferenceJobStatus`, `ValidationReferenceJob` entity를 구현한다. table name은 `validation_reference_jobs`, 컬럼명과 길이는 `V6__create_auth_ai_explanation_tables.sql`과 `qtai-server/02_ERD_문서.md`를 따른다.
11. `ValidationReferenceJobRepositoryTest`를 작성한다. 저장 후 `source_name`, `source_file_name`, `source_file_hash`, nullable URI, `status`, `expires_at`, `deleted_at`, `created_at`, `updated_at` 매핑을 검증한다.
12. `ValidationReferenceJobRepository`를 생성한다. 조회는 `findById`만 필요하므로 불필요한 query method를 추가하지 않는다.
13. `ValidationReferenceJobServiceTest`를 작성한다. 생성 성공, 조회 성공, 없는 id 404, 만료 성공, 이미 만료된 작업 409, audit write 호출을 검증한다.
14. service 테스트에서 audit snapshot에 `storageUri`, `indexStorageUri`, 원문 본문, secret, token, password 계열 값이 들어가지 않는지 검증한다.
15. `ValidationReferenceJobService`를 구현한다. 생성/만료 메서드는 `@Transactional`, 조회 메서드는 `@Transactional(readOnly = true)`를 사용한다.
16. `ValidationReferenceJobService`는 `CreateValidationReferenceJobUseCase`, `GetValidationReferenceJobUseCase`, `ExpireValidationReferenceJobUseCase`를 모두 구현한다.
17. `ErrorCode`에 `VALIDATION_REFERENCE_JOB_NOT_FOUND`를 추가하고 `AiWebExceptionResponses`에서 404로 매핑한다.
18. `AiUseCaseContractTest`에 신규 command/query/response DTO 필드 계약을 추가한다. 응답 DTO에 `storageUri`, `indexStorageUri`, `sourceFileHash`가 없는지도 검증한다.
19. `qtai-server/apis/api-v1/openapi.yaml`에 3개 path와 request/response schema를 추가한다. security 설명은 `SYSTEM_BATCH 또는 ROLE_SYSTEM_BATCH authority` 기준으로 작성한다.
20. OpenAPI schema에는 `SystemValidationReferenceJobRequest`, `ValidationReferenceJobResponse`, `ValidationReferenceJobApiResponse` 계열을 추가한다.
21. OpenAPI example에는 실제 주석 원문, 금지 번역본 본문, signed URL, token, password 예시를 넣지 않는다.
22. `doc/프로젝트 문서/04_API_명세서.md` 7.4를 구현 계약에 맞춘다. 본문 권한은 `SYSTEM_BATCH`로 정리하고, `CONTENT_CREATOR` 직접 접근은 이번 구현에서 제외한다고 적는다.
23. `rg`로 사용자 앱 경로(`/api/v1/ai/**`)나 일반 관리자 경로에 validation reference jobs API가 노출되지 않았는지 확인한다.
24. 관련 단위 테스트와 OpenAPI lint를 실행한다. 실패가 있으면 원인을 확인하고 같은 PR 범위 안에서 수정한다.
25. `doc/workspaces/DevC_강상민/reports/2026-05-28_ai-validation-reference-jobs_report.md`를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemValidationReferenceJobControllerTest.java` | `POST /api/v1/system/validation-reference-jobs`가 request를 command로 매핑하고 `201 Created`를 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemValidationReferenceJobControllerTest.java` | 인증 없음 `401`, `ROLE_USER` `403`, `ROLE_ADMIN + ADMIN_ROLE_CONTENT_CREATOR` `403`, `SYSTEM_BATCH/ROLE_SYSTEM_BATCH` 허용 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemValidationReferenceJobControllerTest.java` | 필수 필드 누락과 공백 필드가 `400 INVALID_INPUT`으로 응답된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemValidationReferenceJobControllerTest.java` | `GET /api/v1/system/validation-reference-jobs/{jobId}`가 단건 조회 응답을 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemValidationReferenceJobControllerTest.java` | `POST /api/v1/system/validation-reference-jobs/{jobId}/expire`가 `status=EXPIRED` 응답을 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemValidationReferenceJobControllerTest.java` | not found는 `404`, 잘못된 상태 전이는 `409`로 매핑된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/ValidationReferenceJobTest.java` | 생성 필수 값, 초기 `ACTIVE`, nullable URI/expiresAt, `ACTIVE -> EXPIRED` 전이 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/ValidationReferenceJobTest.java` | `EXPIRED` 또는 `DELETED` 상태에서 expire 요청 시 `INVALID_STATUS_TRANSITION` |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/ValidationReferenceJobRepositoryTest.java` | `validation_reference_jobs` 컬럼 매핑과 nullable 필드 저장 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/ValidationReferenceJobServiceTest.java` | 생성, 조회, 만료, 없는 id, 상태 전이 실패, audit write 호출 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/ValidationReferenceJobServiceTest.java` | audit snapshot에 원문, URI, secret, token, password 계열 값이 저장되지 않음 |
| `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | 신규 UseCase DTO record 계약과 응답 DTO의 민감 필드 미노출 |

## 수용 기준

- [ ] `POST /api/v1/system/validation-reference-jobs`가 `SYSTEM_BATCH/ROLE_SYSTEM_BATCH` 권한에서만 성공한다.
- [ ] `GET /api/v1/system/validation-reference-jobs/{jobId}`가 존재하는 작업을 단건 조회한다.
- [ ] `POST /api/v1/system/validation-reference-jobs/{jobId}/expire`가 `ACTIVE` 작업만 `EXPIRED`로 바꾼다.
- [ ] `ROLE_ADMIN + ADMIN_ROLE_CONTENT_CREATOR` 직접 호출은 `403 FORBIDDEN`으로 차단된다.
- [ ] 없는 job id는 `VALIDATION_REFERENCE_JOB_NOT_FOUND`로 `404` 응답된다.
- [ ] `EXPIRED` 또는 `DELETED` 작업의 만료 요청은 `INVALID_STATUS_TRANSITION`으로 `409` 응답된다.
- [ ] 생성/조회/만료 응답에 검증 참조 원문 전체, `storageUri`, `indexStorageUri`, `sourceFileHash`가 노출되지 않는다.
- [ ] 생성/만료 감사 로그가 남고 audit snapshot에 원문, URI, secret, token, password 계열 값이 들어가지 않는다.
- [ ] OpenAPI와 `04_API_명세서.md`가 구현된 권한과 응답 필드 기준으로 정합화된다.
- [ ] 사용자 앱 경로와 일반 관리자 경로에 검증 참조 작업 API가 추가되지 않는다.
- [ ] 구현 결과 report가 `doc/workspaces/DevC_강상민/reports/2026-05-28_ai-validation-reference-jobs_report.md`에 작성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- API 계약, entity 상태 전이, audit snapshot, OpenAPI 문서가 같은 필드 결정을 공유하므로 한 흐름에서 순서대로 맞추는 편이 충돌을 줄인다.
- `CONTENT_CREATOR` 직접 접근 제외, URI 미노출, audit snapshot 필드 제한은 작은 계약 차이가 보안 표면을 바꾸므로 메인 agent가 직접 통합 판단하는 편이 안전하다.
- 신규 파일 수는 많지만 모두 `domain.ai` 안의 단일 기능에 묶여 있어 병렬화 이득보다 재작업 위험이 크다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 controller 테스트, service/entity 구현, OpenAPI, 문서, report를 순서대로 직접 수행한다. 단, 구현 후 독립 리뷰가 필요하면 변경 diff 기준으로 별도 코드 리뷰를 요청할 수 있다.

## 검증 계획

- `./gradlew -p qtai-server test --tests "*ValidationReferenceJob*"`
- `./gradlew -p qtai-server test --tests "com.qtai.domain.ai.api.AiUseCaseContractTest"`
- `./gradlew -p qtai-server test --tests "com.qtai.domain.ai.web.SystemValidationReferenceJobControllerTest"`
- `./gradlew -p qtai-server test --tests "com.qtai.domain.ai.internal.ValidationReferenceJobServiceTest"`
- `./gradlew -p qtai-server test --tests "com.qtai.domain.ai.internal.ValidationReferenceJobRepositoryTest"`
- `./gradlew -p qtai-server test --tests "com.qtai.domain.ai.internal.ValidationReferenceJobTest"`
- `./gradlew -p qtai-server build`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `rg --line-number "CONTENT_CREATOR|ADMIN_ROLE_CONTENT_CREATOR|storageUri|indexStorageUri|sourceFileHash" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/apis/api-v1/openapi.yaml doc/프로젝트 문서/04_API_명세서.md`
- `rg --line-number "개역개정|ESV|NIV|성서유니온|두란노|password|token|private key|secret" qtai-server/src/test/java/com/qtai/domain/ai qtai-server/apis/api-v1/openapi.yaml doc/workspaces/DevC_강상민/reports/2026-05-28_ai-validation-reference-jobs_report.md`
- `git diff --check`

`npx @stoplight/spectral-cli`는 저장소 루트 `.spectral.yaml`이 없으면 실패할 수 있다. 그 경우 report에 ruleset 부재로 실행 불가 또는 실패했다고 명확히 기록한다.

## 후속 작업으로 남길 항목

- `service_accounts` 테이블, credential 저장 방식, 시스템 토큰 검증 필터, `/api/v1/system/**` 전역 보안 설정
- service account id를 audit `actorId`에 연결하는 정책
- 임시 파일/색인 실제 삭제 작업과 `DELETED` 상태 전이
- 만료 예정 작업을 자동으로 찾는 cleanup batch
- `CONTENT_CREATOR`가 검증 참조 작업을 직접 생성해야 하는 별도 운영 화면 또는 관리자 API 필요성 재검토
- `commentary_sources`, `commentary_materials`, `commentary_material_verses` 기반 영구 주석 자료 관리 흐름
