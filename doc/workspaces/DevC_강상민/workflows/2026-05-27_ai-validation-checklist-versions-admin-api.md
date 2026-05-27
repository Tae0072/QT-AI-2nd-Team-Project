# Workflow — 2026-05-27 ai-validation-checklist-versions-admin-api

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feat/admin-ai-validation-checklist-versions-api` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14 |
| 트리거 | W2 스케줄과 Lead 체크리스트에서 `validation_reference_jobs`보다 `ai_validation_checklist_versions` 관리 API를 먼저 구현하기로 확정 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/main/java/com/qtai/domain/audit/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/audit/**`, `qtai-server/apis/api-v1/openapi.yaml`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/workspaces/DevC_강상민/reports/**` |

## 기준 문서

- `doc/workspaces/DevC_강상민/강상민_2W_스케줄.md`
- `doc/workspaces/DevC_강상민/reports/checkList.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-26_ai-system-assets-validation-logs.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-26_admin-ai-assets-query.md`
- `doc/프로젝트 문서/07_요구사항_정의서.md`
- `doc/프로젝트 문서/03_아키텍처_정의서.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `doc/프로젝트 문서/23_도메인_용어사전.md`
- `doc/프로젝트 문서/25_기능_명세서.md`
- `qtai-server/02_ERD_문서.md`
- `CODE_CONVENTION.md`

## 작업 목표

관리자가 AI 검증 체크리스트 버전을 조회, 생성, 활성화, 폐기할 수 있도록 `GET/POST /api/v1/admin/ai/validation-checklists`와 상태 변경 API를 구현한다. 이 API는 F-14의 검증 체크리스트 버전 추적 요구사항을 뒷받침하며, 이후 생성되는 AI 산출물이 어떤 검증 기준을 사용했는지 확인할 수 있게 한다.

권한은 `04_API_명세서.md` 7.2 기준으로 `ADMIN + REVIEWER/SUPER_ADMIN`만 허용한다. 체크리스트 변경은 감사 로그 대상이므로 `CHECKLIST_CREATE`, `CHECKLIST_ACTIVATE`, `CHECKLIST_RETIRE` 기록까지 포함한다.

## 범위

- `GET /api/v1/admin/ai/validation-checklists` 목록 조회 API를 구현한다.
- 목록 조회 필터는 `checklistType`, `status`를 지원한다.
- 목록 조회는 공통 목록 규칙에 맞춰 `page`, `size` query parameter와 `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `sort` 페이징 응답을 사용한다.
- 목록 정렬은 `createdAt,desc`, 동률이면 `id,desc`로 고정한다.
- `POST /api/v1/admin/ai/validation-checklists` 생성 API를 구현한다.
- 생성 요청은 `checklistType`, `version`, `contentHash`, `status`를 받는다.
- 생성 시 `status`는 생략 또는 `DRAFT`만 허용한다. `ACTIVE`, `RETIRED` 직접 생성은 상태 전이 우회를 막기 위해 `400 INVALID_INPUT`으로 차단한다.
- 생성 응답은 `id`, `checklistType`, `version`, `contentHash`, `status`, nullable `createdByAdminId`, `createdAt`, `activatedAt`, `retiredAt`을 반환한다.
- `POST /api/v1/admin/ai/validation-checklists/{id}/activate` 활성화 API를 구현한다.
- 활성화 대상은 `DRAFT` 상태만 허용한다.
- 활성화 시 같은 `checklistType`의 기존 `ACTIVE` 버전은 API 명세의 MVP 권장 기준대로 자동 `RETIRED` 처리한다.
- 활성화 응답은 활성화된 버전의 최신 상태를 `200 OK`로 반환한다.
- `POST /api/v1/admin/ai/validation-checklists/{id}/retire` 폐기 API를 구현한다.
- 폐기 대상은 `ACTIVE` 상태만 허용한다. `DRAFT`, `RETIRED`는 `409 INVALID_STATUS_TRANSITION`으로 차단한다.
- 폐기 응답은 폐기된 버전의 최신 상태를 `200 OK`로 반환한다.
- 모든 API는 `ROLE_ADMIN`과 `ADMIN_ROLE_REVIEWER` 또는 `ADMIN_ROLE_SUPER_ADMIN` authority를 요구한다.
- 인증 없음은 `401 UNAUTHORIZED`, 권한 부족은 `403 FORBIDDEN`으로 응답한다.
- 없는 checklist id는 `404 CHECKLIST_NOT_FOUND`로 응답한다.
- 중복 `checklistType + version`은 `409 DUPLICATE_CHECKLIST_VERSION`으로 응답한다.
- 이번 범위의 서버 저장 대상은 `checklistType`, `version`, `contentHash`, `status`, 상태 시각이다.
- 실제 체크리스트 원문은 외부 문서/파일을 SSoT로 두고, 서버는 `contentHash`로 외부 원문과 버전을 대조하는 registry 역할만 한다.
- `created_by_admin_id`는 ERD상 `admin_users.id` 의미를 유지한다. 현재 인증 principal이 `admin_users.id`인지 보장되지 않으므로 이번 범위에서는 `createdByAdminId=null`을 허용하고, 수행 주체는 감사 로그의 actor 정보로 추적한다.
- `audit_logs` 실제 저장 경로가 현재 스텁 상태이므로, 이번 작업에서 체크리스트 변경 기록에 필요한 최소 append-only audit write 계약과 저장 구현을 함께 완성한다.
- `qtai-server/apis/api-v1/openapi.yaml`에 관리자 검증 체크리스트 API 4종의 path, request, response, error schema를 반영한다.
- 신규 에러 코드 `CHECKLIST_NOT_FOUND`, `DUPLICATE_CHECKLIST_VERSION`은 `04_API_명세서.md`, Java `ErrorCode`, OpenAPI를 같은 PR에서 정합화한다.
- 작업 후 DevC report를 작성해 변경 내용, 검증 결과, 제외 범위, 잔여 리스크를 남긴다.

## 제외 범위

- `validation_reference_jobs` 생성, 조회, 만료 API 구현은 제외한다.
- AI 평가 셋, 평가 케이스 API 구현은 제외한다.
- 체크리스트 항목 원문을 조회·수정할 수 있는 별도 저장소나 원문 저장 컬럼 추가는 제외한다.
- 서버 측 canonical hash 재계산 정책은 제외한다. 이번 PR은 요청 `contentHash` 형식 검증까지만 수행하고, 실제 원문 검증은 외부 SSoT 문서/파일과 hash 대조 운영 절차에 맡긴다.
- `ACTIVE_CHECKLIST_EXISTS` 충돌 반환 정책은 적용하지 않는다. 활성화 시 기존 ACTIVE 자동 폐기 정책을 사용한다.
- 관리자 계정 테이블(`admin_users`) 신규 구현과 관리자 권한 매핑 저장소 구현은 제외한다. 현재 컨트롤러 권한 해석은 기존 `Authentication` authority 패턴을 따른다.
- 감사 로그 목록 API와 비동기 감사 로그 처리 인프라는 제외한다.
- 사용자 앱 경로(`/api/v1/ai/**`)에는 체크리스트 관리 API를 추가하지 않는다.
- 실제 DeepSeek 호출, AI 산출물 생성, 검증 실행 로직 변경은 제외한다.
- prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 예시는 요청, 응답, 테스트 fixture, 로그, OpenAPI example에 포함하지 않는다.

## 주요 계약 결정

| 구분 | 결정 |
| --- | --- |
| 목록 API | `GET /api/v1/admin/ai/validation-checklists` |
| 생성 API | `POST /api/v1/admin/ai/validation-checklists` |
| 활성화 API | `POST /api/v1/admin/ai/validation-checklists/{id}/activate` |
| 폐기 API | `POST /api/v1/admin/ai/validation-checklists/{id}/retire` |
| 권한 | `ROLE_ADMIN` + `ADMIN_ROLE_REVIEWER` 또는 `ADMIN_ROLE_SUPER_ADMIN` |
| checklistType | `EXPLANATION`, `SIMULATOR`, `QA` |
| status | `DRAFT`, `ACTIVE`, `RETIRED` |
| 목록 응답 | 페이징 응답. `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `sort` 포함 |
| 생성 상태 | 생략 또는 `DRAFT`만 허용 |
| 활성화 정책 | 같은 `checklistType` 기존 ACTIVE를 자동 RETIRED 처리 후 대상 DRAFT를 ACTIVE 처리 |
| 폐기 정책 | ACTIVE만 RETIRED 처리 |
| 없는 ID | `CHECKLIST_NOT_FOUND`로 404 |
| 중복 버전 | `DUPLICATE_CHECKLIST_VERSION`으로 409 |
| 원문 저장 | 저장하지 않음. 외부 문서/파일을 원문 SSoT로 두고 서버는 hash registry만 저장 |
| createdByAdminId | `admin_users.id` 매핑 전까지 nullable. principal id를 `created_by_admin_id`로 저장하지 않음 |
| 감사 actionType | `CHECKLIST_CREATE`, `CHECKLIST_ACTIVATE`, `CHECKLIST_RETIRE` |
| 감사 targetType | `AI_VALIDATION_CHECKLIST_VERSION` |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/ListAdminAiValidationChecklistsUseCase.java` | 관리자 체크리스트 목록 조회 UseCase interface |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/CreateAdminAiValidationChecklistUseCase.java` | 관리자 체크리스트 생성 UseCase interface |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/ActivateAdminAiValidationChecklistUseCase.java` | 관리자 체크리스트 활성화 UseCase interface |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/RetireAdminAiValidationChecklistUseCase.java` | 관리자 체크리스트 폐기 UseCase interface |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/ListAdminAiValidationChecklistsQuery.java` | `adminId`, `memberRole`, `adminRole`, 필터, page, size DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/CreateAdminAiValidationChecklistCommand.java` | 생성 command DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/ChangeAdminAiValidationChecklistStatusCommand.java` | 활성화/폐기 command DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/AdminAiValidationChecklistResponse.java` | 단건 생성/상태 변경 응답 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/AdminAiValidationChecklistListResponse.java` | 목록 응답 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiValidationChecklistVersion.java` | `ai_validation_checklist_versions` entity와 상태 전이 메서드 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiValidationChecklistType.java` | `EXPLANATION`, `SIMULATOR`, `QA` enum |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiValidationChecklistStatus.java` | `DRAFT`, `ACTIVE`, `RETIRED` enum |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiValidationChecklistVersionRepository.java` | JPA repository, 타입/상태 조회, 중복 확인, ACTIVE 조회 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistService.java` | 권한 검증, 생성, 활성화, 자동 폐기, 폐기, 감사 로그 기록 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiValidationChecklistController.java` | 관리자 HTTP API 4종, 인증 주체 해석, 공통 응답 매핑 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiValidationChecklistRequest.java` | 생성 request DTO와 item validation |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/AiWebExceptionResponses.java` | 신규 checklist 에러 코드의 HTTP status 매핑 |
| Modify | `qtai-server/src/main/java/com/qtai/common/exception/ErrorCode.java` | `CHECKLIST_NOT_FOUND`, `DUPLICATE_CHECKLIST_VERSION` 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/audit/api/WriteAuditLogUseCase.java` | `write(AuditLogWriteRequest request)` 계약 활성화 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/audit/api/dto/AuditLogWriteRequest.java` | ERD 기준 감사 로그 write DTO 필드 정의 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/audit/internal/AuditLog.java` | `audit_logs` append-only entity 구현 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/audit/internal/AuditRepository.java` | `AuditLog` 저장 repository |
| Modify | `qtai-server/src/main/java/com/qtai/domain/audit/internal/AuditService.java` | 최소 동기 write 구현. 목록 조회와 async는 구현하지 않음 |
| Create | `qtai-server/src/main/resources/db/migration/V12__create_audit_logs.sql` | 현재 migration에 없는 `audit_logs` 테이블과 인덱스 생성. `admin_users` 테이블이 아직 없으므로 FK는 추가하지 않음 |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | 관리자 검증 체크리스트 API 계약 반영 |
| Modify | `doc/프로젝트 문서/04_API_명세서.md` | 신규 checklist 에러 코드와 목록 페이징 응답, nullable `createdByAdminId`, hash registry 정책 반영 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-05-27_ai-validation-checklist-versions-admin-api_report.md` | 구현 결과, 검증 결과, 제외 범위 기록 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiValidationChecklistControllerTest.java` | HTTP API, 인증/권한, request mapping, error mapping 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistServiceTest.java` | 권한, 상태 전이, 자동 폐기, 감사 로그 호출 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiValidationChecklistVersionTest.java` | entity 생성/활성화/폐기 상태 전이 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiValidationChecklistVersionRepositoryTest.java` | unique 제약, 필터 조회, 정렬 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | 신규 UseCase DTO record 계약 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/audit/internal/AuditServiceTest.java` | 최소 audit write 저장과 민감정보 미포함 메타데이터 검증 |

## 구현 순서

1. `AdminAiValidationChecklistControllerTest`에 목록 조회 성공 테스트를 추가한다. `ROLE_ADMIN + ADMIN_ROLE_REVIEWER` 요청이 `200 OK`와 공통 envelope를 반환하고 `checklistType/status/page/size`가 UseCase query로 전달되어야 한다.
2. 목록 조회 권한 테스트를 추가한다. 인증 없음은 `401`, `ROLE_USER`와 `ROLE_ADMIN + ADMIN_ROLE_OPERATOR`는 `403`, `ROLE_ADMIN + ADMIN_ROLE_SUPER_ADMIN`은 허용되어야 한다.
3. 생성 성공 테스트를 추가한다. `DRAFT` 요청이 `201 Created`와 `AdminAiValidationChecklistResponse`를 반환하고, principal에서 추출한 `adminId/memberRole/adminRole`이 command에 포함되어야 한다.
4. 생성 request validation 테스트를 추가한다. `checklistType`, `version`, `contentHash` 누락 또는 공백은 `400`으로 응답해야 한다.
5. 생성 status validation 테스트를 추가한다. `status` 생략 또는 `DRAFT`는 허용하고, `ACTIVE`, `RETIRED`는 `400 INVALID_INPUT`으로 응답해야 한다.
6. 활성화 성공 테스트를 추가한다. `POST /api/v1/admin/ai/validation-checklists/{id}/activate`가 `200 OK`와 `status=ACTIVE`, `activatedAt`을 반환해야 한다.
7. 폐기 성공 테스트를 추가한다. `POST /api/v1/admin/ai/validation-checklists/{id}/retire`가 `200 OK`와 `status=RETIRED`, `retiredAt`을 반환해야 한다.
8. controller test가 실패하는 것을 확인한 뒤 UseCase interface와 DTO를 생성한다.
9. `AdminAiValidationChecklistController`를 구현한다. 기존 `AdminAiAssetController`의 관리자 인증 해석 패턴을 재사용하되, 중복이 커지면 `domain.ai.web` package-private helper로 분리한다.
10. `AiValidationChecklistVersionTest`를 작성한다. 생성 시 필수값 검증, `DRAFT -> ACTIVE`, `ACTIVE -> RETIRED`, 잘못된 전이 예외를 확인한다.
11. `AiValidationChecklistVersion`, `AiValidationChecklistType`, `AiValidationChecklistStatus`를 구현한다. Entity는 `internal/`에 두고 API DTO로 직접 반환하지 않는다.
12. `AiValidationChecklistVersionRepositoryTest`를 작성한다. `checklist_type + version` unique 제약, `checklistType/status` 필터, `createdAt desc, id desc` 정렬을 검증한다.
13. `AiValidationChecklistVersionRepository`를 구현한다. 조회 전용 쿼리는 repository 메서드 또는 명시 JPQL을 사용하고 Controller에서 직접 호출하지 않는다.
14. `AuditServiceTest`를 작성한다. `WriteAuditLogUseCase.write`가 `audit_logs`에 `actorType=ADMIN`, `actionType=CHECKLIST_*`, `targetType=AI_VALIDATION_CHECKLIST_VERSION`을 저장해야 한다.
15. `audit_logs` migration, `AuditLog`, `AuditRepository`, `AuditLogWriteRequest`, `WriteAuditLogUseCase`, `AuditService`를 최소 범위로 구현한다. `admin_users` FK는 현재 migration 부재 때문에 추가하지 않는다.
16. `AdminAiValidationChecklistServiceTest`를 작성한다. `REVIEWER/SUPER_ADMIN`은 허용하고 `OPERATOR`, `CONTENT_CREATOR`, 일반 `USER`는 차단해야 한다.
17. service test에 생성 성공, 중복 버전 충돌, 없는 id 조회, 활성화 자동 폐기, 폐기 전이 실패, 감사 로그 호출을 추가한다.
18. `AdminAiValidationChecklistService`를 `@Service`로 구현한다. 생성과 상태 변경 public 메서드에는 `@Transactional`, 목록 조회에는 `@Transactional(readOnly = true)`를 사용한다.
19. 생성 구현은 `contentHash`를 저장한다. 현재 ERD에 원문 저장 컬럼이 없으므로 체크리스트 원문을 primary table이나 감사 로그 metadata에 저장하지 않는다.
20. 활성화 구현은 대상 row를 조회하고 `DRAFT`인지 확인한 뒤 같은 `checklistType`의 기존 ACTIVE를 모두 `retire()` 처리하고 대상 row를 `activate()` 처리한다.
21. 폐기 구현은 대상 row가 `ACTIVE`인지 확인하고 `retire()` 처리한다.
22. 생성/활성화/폐기 후 `WriteAuditLogUseCase.write`를 호출한다. 감사 로그 `beforeJson/afterJson`에는 id, checklistType, version, contentHash, status, timestamp만 넣고 raw prompt, provider response, secret 계열 값은 넣지 않는다.
23. `AiWebExceptionResponses`와 `ErrorCode`에 신규 에러 코드를 연결하고 controller/service 테스트로 HTTP status와 error body를 확인한다.
24. `AiUseCaseContractTest`에 신규 query/command/response record 필드 계약을 추가한다.
25. `qtai-server/apis/api-v1/openapi.yaml`에 tag, path, request schema, response schema, error response를 반영한다.
26. `doc/프로젝트 문서/04_API_명세서.md` 7.2에 `CHECKLIST_NOT_FOUND`, `DUPLICATE_CHECKLIST_VERSION`, 목록 페이징 응답, nullable `createdByAdminId`, hash registry 정책을 반영한다.
27. OpenAPI example에는 문서 예시 수준의 checklist item만 넣고 금지 번역본, prompt 원문, provider raw response, secret, token, password 예시는 넣지 않는다.
28. `rg`로 사용자 앱 경로, 금지 import, 민감 예시가 추가되지 않았는지 확인한다.
29. 관련 테스트와 API lint를 실행하고 실패가 있으면 원인을 수정한다.
30. `doc/workspaces/DevC_강상민/reports/2026-05-27_ai-validation-checklist-versions-admin-api_report.md`를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiValidationChecklistControllerTest.java` | `GET /api/v1/admin/ai/validation-checklists`가 `checklistType/status/page/size`를 UseCase query로 전달한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiValidationChecklistControllerTest.java` | `REVIEWER/SUPER_ADMIN` 허용, 인증 없음 `401`, 일반 사용자와 `OPERATOR` `403` |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiValidationChecklistControllerTest.java` | 생성 요청이 `201 Created`와 응답 DTO를 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiValidationChecklistControllerTest.java` | 생성 request 필수값 누락이 `400`으로 응답한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiValidationChecklistControllerTest.java` | 활성화/폐기 요청이 `200 OK`와 변경된 status/timestamp를 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistServiceTest.java` | 권한 검증, 생성 status 제한, 중복 버전 충돌 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistServiceTest.java` | 활성화 시 같은 type 기존 ACTIVE 자동 RETIRED 처리 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistServiceTest.java` | 생성 시 `createdByAdminId`는 admin_users 매핑 전까지 null로 유지한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistServiceTest.java` | 폐기 대상이 ACTIVE가 아니면 `INVALID_STATUS_TRANSITION` |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistServiceTest.java` | 생성/활성화/폐기마다 `CHECKLIST_*` 감사 로그를 기록한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiValidationChecklistVersionTest.java` | Entity 상태 전이와 timestamp 설정 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiValidationChecklistVersionRepositoryTest.java` | unique 제약, 필터 조회, 정렬 |
| `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | 신규 UseCase DTO record 필드 계약 |
| `qtai-server/src/test/java/com/qtai/domain/audit/internal/AuditServiceTest.java` | 최소 audit write 저장과 금지 문자열 미포함 |

## 수용 기준

- [ ] 관리자 검증 체크리스트 목록 조회 API가 구현된다.
- [ ] 관리자 검증 체크리스트 생성 API가 구현된다.
- [ ] 관리자 검증 체크리스트 활성화 API가 구현된다.
- [ ] 관리자 검증 체크리스트 폐기 API가 구현된다.
- [ ] 네 API 모두 `ADMIN + REVIEWER/SUPER_ADMIN`만 허용한다.
- [ ] `OPERATOR`, `CONTENT_CREATOR`, 일반 사용자는 차단된다.
- [ ] 생성은 `DRAFT`만 허용하고 ACTIVE 직접 생성은 차단된다.
- [ ] 활성화 시 같은 `checklistType` 기존 ACTIVE가 자동 RETIRED 처리된다.
- [ ] 폐기는 ACTIVE 상태만 허용한다.
- [ ] 중복 `checklistType + version`은 409로 응답한다.
- [ ] 중복 버전 에러 코드는 `DUPLICATE_CHECKLIST_VERSION`, 없는 ID 에러 코드는 `CHECKLIST_NOT_FOUND`를 사용한다.
- [ ] 목록 응답은 공통 페이징 구조를 사용한다.
- [ ] 생성/활성화/폐기 모두 `audit_logs.action_type=CHECKLIST_*`로 기록된다.
- [ ] OpenAPI에 API 4종과 request/response/error schema가 반영된다.
- [ ] `04_API_명세서.md`에 신규 에러 코드와 hash registry 정책이 반영된다.
- [ ] `ai` 도메인이 다른 도메인의 `internal`, `web`, repository 타입을 import하지 않는다.
- [ ] prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 예시가 코드, 테스트, OpenAPI, 감사 로그 metadata에 추가되지 않는다.
- [ ] `ai_validation_checklist_versions`의 현 ERD 범위를 넘는 raw checklist item 저장 컬럼을 추가하지 않는다.
- [ ] 실제 체크리스트 원문은 외부 문서/파일 SSoT로 유지하고 서버에는 저장하지 않는다.
- [ ] `created_by_admin_id`에는 불명확한 principal id를 저장하지 않고 null을 허용한다.
- [ ] 구현 결과 report에 실행한 검증 명령과 제외 범위를 기록한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- controller, UseCase DTO, service, entity, repository, OpenAPI schema가 같은 request/response 계약에 강하게 연결되어 있다.
- 활성화 자동 폐기와 감사 로그 기록은 단일 트랜잭션 경계 안에서 맞춰야 하므로 병렬 편집보다 순차 구현이 충돌을 줄인다.
- audit 도메인은 현재 스텁 상태라 최소 구현 범위를 메인 에이전트가 직접 통제하는 편이 안전하다.

### 위임 가능한 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 메인 에이전트 | 전체 구현, 테스트, OpenAPI, report 작성 | 위 파일 구조와 책임에 명시한 전체 경로 |

### 직접 실행 판단

메인 에이전트가 controller test, service test, entity/repository test, audit write test, 구현, OpenAPI 반영, 최종 검증을 순서대로 직접 수행한다.

## 검증 계획

- `Push-Location qtai-server; .\gradlew.bat test --tests "*AdminAiValidationChecklistControllerTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*AdminAiValidationChecklistServiceTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*AiValidationChecklistVersionTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*AiValidationChecklistVersionRepositoryTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*AuditServiceTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*AiUseCaseContractTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*Ai*"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat build; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test jacocoTestReport; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat jacocoTestCoverageVerification; Pop-Location`
- `npx.cmd @apidevtools/swagger-cli validate qtai-server/apis/api-v1/openapi.yaml`
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `rg -n "^import .*domain\\.[a-z]+\\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai`
- `rg -n -g "*.java" "RequestMapping\\(\"/api/v1/ai|validation-checklists" qtai-server/src/main/java/com/qtai/domain/ai/web`
- `rg -n "raw response|provider raw|password|private key|secret|example.*token" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/domain/audit qtai-server/src/test/java/com/qtai/domain/audit qtai-server/apis/api-v1/openapi.yaml`
- `gitleaks detect --source . --redact --exit-code 1`
- `.spectral.yaml` 또는 `gitleaks` 실행 파일이 없으면 실패/미실행 사유를 report와 최종 응답에 기록한다.

## 다음 작업으로 남길 항목

- `validation_reference_jobs` 생성, 조회, 만료 API 구현
- AI 평가 셋과 평가 케이스 관리 API 구현
- 감사 로그 목록 API와 비동기 감사 로그 처리 인프라 정식 구현
- `admin_users` 테이블과 관리자 권한 매핑 저장소 구현
- 체크리스트 원문 조회가 필요해질 경우 원문 저장 컬럼 또는 별도 checklist item 테이블에 대한 요구사항/ERD/API 변경 검토
- `contentHash`를 서버가 canonical JSON 기준으로 재계산하는 정책 검토
