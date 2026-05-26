# Workflow — 2026-05-26 admin-ai-assets-query

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-ai-assets-query` |
| PR 대상 | `dev` |
| 관련 F-ID | F-06, F-14 |
| 트리거 | W2 다음 작업. 관리자 웹에서 AI 산출물과 검증 로그를 조회할 수 있는 API 초안을 별도 PR로 구현 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**`, `qtai-server/apis/api-v1/openapi.yaml`, `doc/workspaces/DevC_강상민/reports/**` |

## 기준 문서

- `doc/workspaces/DevC_강상민/강상민_2W_스케줄.md`
- `doc/workspaces/DevC_강상민/reports/checkList.md`
- `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-prompt-version-id-mapping_report.md`
- `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-assets-validation-logs_report.md`
- `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-openapi-contract_report.md`
- `doc/프로젝트 문서/07_요구사항_정의서.md`
- `doc/프로젝트 문서/03_아키텍처_정의서.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `doc/프로젝트 문서/23_도메인_용어사전.md`
- `doc/프로젝트 문서/25_기능_명세서.md`
- `CODE_CONVENTION.md`

## 작업 목표

관리자 웹에서 AI 산출물 검토 대상을 조회할 수 있도록 `GET /api/v1/admin/ai/assets` 목록 API와 `GET /api/v1/admin/ai/assets/{assetId}` 상세 API 초안을 구현한다. 조회 결과는 `ai_generated_assets`, `ai_generation_jobs`, `ai_prompt_versions`, `ai_validation_logs`를 기준으로 조립하고, 관리자 검토에 필요한 최신 검증 결과와 검증 로그 이력을 제공한다.

권한은 `04_API_명세서.md` 4.7.3 기준으로 `ADMIN + REVIEWER/SUPER_ADMIN`만 허용한다. `OPERATOR` 조회 허용 여부는 Lead 확인 항목으로 남아 있으므로 이번 PR에서 허용하지 않는다.

## 범위

- `GET /api/v1/admin/ai/assets` 목록 조회 API를 추가한다.
- `GET /api/v1/admin/ai/assets/{assetId}` 상세 조회 API를 추가한다.
- 두 API 모두 `ROLE_ADMIN`과 `ADMIN_ROLE_REVIEWER` 또는 `ADMIN_ROLE_SUPER_ADMIN` authority를 요구한다.
- `ADMIN_ROLE_OPERATOR`는 이번 PR에서 `403`으로 차단한다.
- 목록 필터는 `assetType`, `targetType`, `status`, `promptVersionId`, `checklistVersionId`를 지원한다.
- 목록 기본 정렬은 `createdAt,desc`로 둔다.
- 목록 응답은 `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `sort`를 포함한다.
- 목록 item은 `id`, `assetType`, `targetType`, `targetId`, `status`, `promptVersion`, `checklistVersionId`, `latestValidationResult`, `sourceLabelPresent`, `createdAt`을 포함한다.
- 상세 응답은 산출물 기본 정보, 생성 job 요약, prompt version 요약, 검증 로그 목록을 포함한다.
- 상세 응답의 `payloadJson`은 관리자 검토에 필요한 최소 JSON으로 포함한다. 응답 DTO는 저장된 payload 외에 prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 성격 필드를 새로 만들거나 조립하지 않는다.
- 검증 로그 목록은 `validationLogId`, `validationReferenceJobId`, `checklistVersionId`, `layer`, `result`, `reviewerType`, `errorMessage`, `createdAt`을 포함한다.
- `qtai-server/apis/api-v1/openapi.yaml`에 관리자 조회 API 2종의 path, query parameter, response schema, error response를 반영한다.
- 구현 후 DevC report를 작성한다.

## 제외 범위

- 승인, 반려, 숨김 API 구현은 제외한다.
- 평가 후보 등록 API 구현은 제외한다.
- 관리자 재생성 API의 기존 동작 변경은 제외한다.
- `OPERATOR` 조회 허용은 제외한다. Lead 결정 후 별도 PR로 권한을 확장한다.
- `validation_reference_jobs` 생성, 조회, 만료 API 구현은 제외한다.
- `ai_validation_checklist_versions` 관리 API 구현은 제외한다.
- `service_accounts` 기반 시스템 토큰 검증 필터와 `/api/v1/system/**` 전역 보안 설정은 제외한다.
- `inputHash` 저장 컬럼 또는 unique key 확장은 제외한다.
- `SUMMARY`/`GLOSSARY` 독립 generation job type 지원은 제외한다.
- 승인된 산출물을 `verse_explanations`, `simulator_clips` 등 사용자 노출 테이블로 연결하는 흐름은 제외한다.
- 실제 DeepSeek 호출, batch worker 실행 흐름은 제외한다.
- 감사 로그 신규 기록은 조회 API이므로 제외한다.

## 주요 계약 결정

| 구분 | 결정 |
| --- | --- |
| 목록 API | `GET /api/v1/admin/ai/assets` |
| 상세 API | `GET /api/v1/admin/ai/assets/{assetId}` |
| 권한 | `ROLE_ADMIN` + `ADMIN_ROLE_REVIEWER` 또는 `ADMIN_ROLE_SUPER_ADMIN` |
| `OPERATOR` | 이번 PR에서는 차단. Lead 확인 항목으로 유지 |
| 목록 필터 | `assetType`, `targetType`, `status`, `promptVersionId`, `checklistVersionId` |
| 목록 정렬 | `createdAt,desc` |
| 목록 페이징 응답 | `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `sort` |
| 상세 `payloadJson` | 관리자 검토용 최소 JSON. prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 성격 필드 추가 금지 |
| 상세 검증 로그 정렬 | `createdAt,desc`, 같은 시각이면 `id,desc` |
| 없는 asset | `AI_ASSET_NOT_FOUND`로 `404` |
| 인증 없음 | `UNAUTHORIZED`로 `401` |
| 권한 부족 | `FORBIDDEN`으로 `403` |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/ListAdminAiAssetsUseCase.java` | 관리자 AI 산출물 목록 조회 UseCase interface |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/GetAdminAiAssetUseCase.java` | 관리자 AI 산출물 상세 조회 UseCase interface |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/ListAdminAiAssetsQuery.java` | `adminId`, `memberRole`, `adminRole`, 목록 필터, pageable 입력 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/AdminAiAssetListResponse.java` | `sort`를 포함한 목록 페이지 응답 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/AdminAiAssetListItem.java` | 목록 item DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/GetAdminAiAssetQuery.java` | `adminId`, `memberRole`, `adminRole`, `assetId` 상세 조회 입력 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/AdminAiAssetDetailResponse.java` | 상세 응답 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/AdminAiValidationLogItem.java` | 상세 검증 로그 item DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryService.java` | read-only 트랜잭션, 권한 검증, 목록/상세 조회 조립 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java` | EntityManager 기반 목록/상세 query와 count query |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiAssetController.java` | 기존 재생성 endpoint 유지, GET 목록/상세 endpoint 추가, 관리자 권한 검증 재사용 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiAssetQueryResponse.java` | web 응답 DTO가 api DTO와 다를 필요가 있을 때만 생성. 동일 구조면 생성하지 않음 |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | 관리자 AI 조회 path, query parameter, response schema, error response 반영 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-05-26_admin-ai-assets-query_report.md` | 변경 내용, 검증 결과, 제외 범위, Lead 확인 항목 기록 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | GET 목록/상세 인증, 권한, 매핑, 오류 응답 테스트 보강 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryServiceTest.java` | 권한 검증, repository 위임, not found 매핑 테스트 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepositoryTest.java` | 필터, 정렬, latest validation result, checklistVersionId 필터 query 테스트 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | 새 query/result DTO 계약 테스트 |

## 구현 순서

1. `AdminAiAssetControllerTest`에 목록 조회 성공 테스트를 추가한다. `ROLE_ADMIN + ADMIN_ROLE_REVIEWER` 요청이 `200 OK`와 `content[0].id`, `promptVersion`, `latestValidationResult`, `sourceLabelPresent`를 반환해야 한다.
2. 목록 조회 테스트에서 인증 주체의 `adminId`, `memberRole`, `adminRole`과 query parameter `assetType`, `targetType`, `status`, `promptVersionId`, `checklistVersionId`, `page`, `size`가 `ListAdminAiAssetsQuery`와 `Pageable`로 전달되는지 검증한다.
3. 목록 조회 권한 테스트를 추가한다. 인증 없음은 `401`, `ROLE_USER`는 `403`, `ROLE_ADMIN + ADMIN_ROLE_OPERATOR`는 `403`, `ROLE_ADMIN + ADMIN_ROLE_SUPER_ADMIN`은 허용한다.
4. 상세 조회 성공 테스트를 추가한다. `GET /api/v1/admin/ai/assets/{assetId}`가 산출물 기본 정보, generation job, prompt version, 관리자 검토용 최소 `payloadJson`, validationLogs를 반환해야 한다.
5. 상세 조회 테스트에서 없는 asset은 UseCase의 `AI_ASSET_NOT_FOUND`를 `404`로 매핑하는지 검증한다.
6. 컨트롤러 테스트가 실패하는 것을 확인한다.
7. `ListAdminAiAssetsUseCase`, `GetAdminAiAssetUseCase`, query/result DTO를 추가한다.
8. `AdminAiAssetController` 생성자에 새 UseCase 2개를 추가하고 기존 재생성 endpoint 동작은 유지한다.
9. `AdminAiAssetController`에 `@GetMapping` 목록 endpoint를 추가한다.
10. `AdminAiAssetController`에 `@GetMapping("/{assetId}")` 상세 endpoint를 추가한다.
11. 기존 private 관리자 인증 로직을 두 GET endpoint에서도 재사용한다. 인증 해석 결과는 `ListAdminAiAssetsQuery`와 `GetAdminAiAssetQuery`의 `adminId`, `memberRole`, `adminRole`에 담아 UseCase로 전달한다. 중복이 커지면 `domain.ai.web` package-private helper로 분리하되, 기존 테스트 기대값을 유지한다.
12. `AdminAiAssetQueryServiceTest`를 작성한다. query DTO의 `memberRole=ADMIN`과 `adminRole=REVIEWER/SUPER_ADMIN`은 허용하고, `adminRole=OPERATOR` 또는 `memberRole` 불일치는 `FORBIDDEN`으로 차단해야 한다.
13. `AdminAiAssetQueryServiceTest`에 목록 조회가 repository에 query와 pageable을 그대로 전달하고 결과를 `sort` 포함 페이지 DTO로 변환하는지 검증한다.
14. `AdminAiAssetQueryServiceTest`에 상세 조회 결과가 없으면 `BusinessException(ErrorCode.AI_ASSET_NOT_FOUND)`를 던지는지 검증한다.
15. `AdminAiAssetQueryServiceTest`에 상세 응답이 저장된 payload 외의 prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 성격 필드를 새로 만들지 않는지 검증한다.
16. `AdminAiAssetQueryService`를 추가하고 `@Service`, `@Transactional(readOnly = true)`를 적용한다.
17. `AdminAiAssetQueryRepositoryTest`를 작성한다. 테스트 데이터는 금지 번역본, prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 예시를 포함하지 않는다.
18. repository 목록 테스트에 필터 없음 기본 조회, `assetType`, `targetType`, `status`, `promptVersionId`, `checklistVersionId` 필터 케이스를 추가한다.
19. repository 목록 테스트에 `createdAt desc` 정렬, page/size/count, `sort=createdAt,desc`가 맞는지 검증한다.
20. repository 목록 테스트에 각 asset의 최신 검증 로그 기준 `latestValidationResult`와 `checklistVersionId`가 응답되는지 검증한다.
21. repository 상세 테스트에 validation logs가 `createdAt desc`, `id desc` 순서로 반환되는지 검증한다.
22. repository 상세 테스트에 `payloadJson`이 안전한 관리자 검토용 JSON fixture로만 조회되는지 검증하고, 검증 참조 원문/AI provider raw response/secret 계열 fixture를 추가하지 않는다.
23. `AdminAiAssetQueryRepository`를 EntityManager 기반으로 구현한다. 동적 필터는 Criteria API 또는 명시 JPQL을 사용하고, 문자열 조립 SQL은 사용하지 않는다.
24. 목록 query는 `ai_generated_assets`, `ai_generation_jobs`, `ai_prompt_versions`, 최신 `ai_validation_logs`를 조합해 목록 item에 필요한 필드만 조회한다.
25. 상세 query는 산출물, generation job, prompt version을 조회하고, 검증 로그 목록은 별도 query로 조회한다.
26. checklistVersionId 필터는 최신 검증 로그 기준으로 적용한다. 구현상 전체 로그 기준으로 적용해야 할 필요가 발견되면 구현을 멈추고 `checkList.md`에 결정 항목으로 올린다.
27. `AiUseCaseContractTest`에 새 UseCase DTO record 생성과 필드 접근자 계약을 추가한다. `ListAdminAiAssetsQuery`, `GetAdminAiAssetQuery`에는 `adminId`, `memberRole`, `adminRole` 접근자가 있어야 한다.
28. `qtai-server/apis/api-v1/openapi.yaml`에 `Admin AI` tag를 추가하고 관리자 조회 API 2종을 문서화한다.
29. OpenAPI 목록 response schema는 `sort`를 포함한 `AdminAiAssetListResponse`, `AdminAiAssetListItem` 기준으로 작성한다.
30. OpenAPI 상세 response schema는 관리자 검토용 최소 `payloadJson`, `AdminAiAssetDetailResponse`, `AdminAiValidationLogItem` 기준으로 작성한다.
31. OpenAPI에는 `OPERATOR` 허용 문구를 넣지 않고 `ADMIN + REVIEWER/SUPER_ADMIN` 기준을 명시한다.
32. 관련 테스트와 OpenAPI 검증을 실행한다.
33. 금지 import, 사용자 AI 경로 추가 여부, 민감 문자열 포함 여부를 `rg`로 확인한다.
34. `doc/workspaces/DevC_강상민/reports/2026-05-26_admin-ai-assets-query_report.md`를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | `GET /api/v1/admin/ai/assets`가 `200 OK`와 목록 페이지 응답을 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | 인증 주체의 `adminId/memberRole/adminRole`과 목록 query parameter가 UseCase query와 Pageable로 전달된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | `ROLE_ADMIN + ADMIN_ROLE_REVIEWER`와 `ROLE_ADMIN + ADMIN_ROLE_SUPER_ADMIN`은 허용된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | 인증 없음은 `401`, `ROLE_USER`와 `ADMIN_ROLE_OPERATOR`는 `403`이다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | `GET /api/v1/admin/ai/assets/{assetId}`가 상세 응답과 validationLogs를 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | 상세 조회 대상이 없으면 `404 AI_ASSET_NOT_FOUND`로 응답한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryServiceTest.java` | query DTO의 `memberRole/adminRole` 기준으로 `REVIEWER/SUPER_ADMIN` 권한은 허용하고 `OPERATOR`는 차단한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryServiceTest.java` | 목록 조회가 repository 결과를 `sort` 포함 페이지 DTO로 변환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryServiceTest.java` | 상세 조회 결과가 없으면 `AI_ASSET_NOT_FOUND`를 던진다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryServiceTest.java` | 상세 응답은 저장 payload 외의 prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 성격 필드를 새로 만들지 않는다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepositoryTest.java` | 필터 없음 기본 목록 조회가 `createdAt desc`와 `sort=createdAt,desc`로 반환된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepositoryTest.java` | `assetType`, `targetType`, `status`, `promptVersionId`, `checklistVersionId` 필터가 동작한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepositoryTest.java` | 최신 검증 로그의 `result`, `checklistVersionId`가 목록 item에 반영된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepositoryTest.java` | 상세 validation logs가 `createdAt desc`, `id desc`로 반환된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepositoryTest.java` | 상세 `payloadJson` 테스트 fixture는 관리자 검토용 안전 필드만 사용한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | 새 query/result DTO record 계약이 유지되고 query DTO에 `adminId/memberRole/adminRole`이 포함된다 |

## 수용 기준

- [ ] `GET /api/v1/admin/ai/assets`가 구현된다.
- [ ] `GET /api/v1/admin/ai/assets/{assetId}`가 구현된다.
- [ ] 두 API 모두 `ADMIN + REVIEWER/SUPER_ADMIN`만 허용한다.
- [ ] `OPERATOR`는 이번 PR에서 `403`으로 차단된다.
- [ ] 목록 필터 `assetType`, `targetType`, `status`, `promptVersionId`, `checklistVersionId`가 동작한다.
- [ ] 목록 기본 정렬은 `createdAt,desc`이다.
- [ ] 목록 응답은 문서의 `content/page/size/totalElements/totalPages/first/last/sort` 구조를 따른다.
- [ ] 목록 item은 최신 검증 결과와 최신 체크리스트 버전 id를 포함한다.
- [ ] 상세 응답은 산출물, generation job, prompt version, 관리자 검토용 최소 `payloadJson`, validation logs를 포함한다.
- [ ] 상세 응답은 저장 payload 외의 prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 성격 필드를 새로 만들지 않는다.
- [ ] 없는 asset 상세 조회는 `404 AI_ASSET_NOT_FOUND`로 응답한다.
- [ ] OpenAPI에 관리자 조회 API 2종이 반영된다.
- [ ] 사용자 앱 경로(`/api/v1/ai/**`)에 관리자 조회 API가 추가되지 않는다.
- [ ] AI 도메인은 다른 도메인의 `internal`, `web`, `repository` 타입을 직접 import하지 않는다.
- [ ] prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 예시는 요청, 응답, 테스트 fixture, 로그에 포함하지 않는다.
- [ ] `doc/workspaces/DevC_강상민/reports/checkList.md`의 `OPERATOR` 확인 항목은 해결 처리하지 않고 유지한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- controller, UseCase DTO, query service, query repository, OpenAPI schema가 같은 응답 계약에 강하게 연결되어 있다.
- 최신 검증 로그 기준 필터와 목록 item 조립은 구현과 테스트를 같은 맥락에서 순서대로 맞춰야 한다.
- 기존 `AdminAiAssetController`의 재생성 endpoint를 건드리므로 병렬 편집보다 직접 실행이 회귀 위험을 줄인다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 controller 테스트, service 테스트, repository 테스트, 구현, OpenAPI 반영, 최종 검증을 순서대로 직접 수행한다.

## 검증 계획

- `Push-Location qtai-server; .\gradlew.bat test --tests "*AdminAiAssetControllerTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*AdminAiAssetQueryServiceTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*AdminAiAssetQueryRepositoryTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*AiUseCaseContractTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*Ai*"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat build; Pop-Location`
- `npx.cmd @apidevtools/swagger-cli validate qtai-server/apis/api-v1/openapi.yaml`
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `rg -n "^import .*domain\\.[a-z]+\\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai`
- `rg -n -g "*.java" "RequestMapping\\(\"/api/v1/ai|GetMapping.*admin/ai" qtai-server/src/main/java/com/qtai/domain/ai/web`
- `rg -n "raw response|provider raw|password|private key|secret|example.*token" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai qtai-server/apis/api-v1/openapi.yaml`
- 환경에 `gitleaks` 실행 파일이 있으면 `gitleaks detect --source . --redact --exit-code 1`을 실행한다.
- `.spectral.yaml` 또는 `gitleaks`가 없으면 실패/미실행 사유를 report에 명확히 기록한다.

## 후속 작업으로 남길 항목

- Lead가 관리자 AI 산출물 조회에서 `OPERATOR` 조회 허용 여부를 확정한다.
- `POST /api/v1/admin/ai/assets/{assetId}/approve`, `reject`, `hide` 구현
- `POST /api/v1/admin/ai/assets/{assetId}/evaluation-candidates` 구현
- `validation_reference_jobs` 생성, 조회, 만료 API 구현
- `ai_validation_checklist_versions` 관리 API 구현
- 승인된 산출물을 사용자 노출 테이블로 연결하는 승인 흐름 구현
- 감사 로그 UseCase 계약 확정 후 승인/반려/숨김/재생성 기록 연결
