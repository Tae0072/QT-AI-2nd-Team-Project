# Workflow — 2026-05-26 ai-system-openapi-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-system-openapi-contract` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | W2 스케줄의 다음 작업. 이미 구현된 `/api/v1/system/ai/**` 시스템 AI API 3종을 OpenAPI 계약에 반영 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 담당 경로 | `qtai-server/apis/api-v1/openapi.yaml`, `doc/workspaces/DevC_강상민/reports/**` |

## 기준 문서

- `doc/workspaces/DevC_강상민/강상민_2W_스케줄.md`
- `doc/workspaces/DevC_강상민/reports/2026-05-22_ai-system-generation-job-trigger-api_report.md`
- `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-prompt-version-id-mapping_report.md`
- `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-assets-validation-logs_report.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `doc/프로젝트 문서/23_도메인_용어사전.md`
- `CODE_CONVENTION.md`

## 작업 목표

현재 서버에는 시스템 배치가 AI 생성 작업, AI 산출물, AI 검증 로그를 접수하는 `/api/v1/system/ai/**` 컨트롤러가 구현되어 있다. 하지만 `qtai-server/apis/api-v1/openapi.yaml`은 아직 `paths: {}` 상태이므로, 실제 구현된 시스템 AI API 3종을 OpenAPI 계약에 반영한다.

이번 작업은 문서 계약 정합화에 한정한다. 공통 보안 필터, service account 토큰 검증, 관리자 조회 API, 미확정 정책은 OpenAPI에 선반영하지 않는다.

## 범위

- `qtai-server/apis/api-v1/openapi.yaml`의 `paths: {}`를 실제 path map으로 전환한다.
- `POST /api/v1/system/ai/generation-jobs`를 문서화한다.
- `POST /api/v1/system/ai/assets`를 문서화한다.
- `POST /api/v1/system/ai/validation-logs`를 문서화한다.
- 세 API 모두 `SYSTEM_BATCH` 권한 요구를 operation description 또는 security 설명에 명시한다.
- `generation-jobs` 요청의 `jobType`은 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 허용 enum으로 문서화한다.
- `generation-jobs` 요청의 `targetType`은 현재 컨트롤러 기준 `QT_PASSAGE`만 허용 enum으로 문서화한다.
- `generation-jobs` 요청에는 `promptVersionId`, `jobType`, `targetType`, `targetId`를 required로 둔다.
- `assets` 요청에는 `generationJobId`, `assetType`, `targetType`, `targetId`, `payloadJson`을 required로 둔다.
- `assets.status`는 optional이며, 제공 시 `VALIDATING`만 허용되는 필드로 문서화한다.
- `validation-logs` 요청에는 `aiAssetId`, `checklistVersionId`, `layer`, `result`, `checklistJson`, `reviewerType`을 required로 둔다.
- `validationReferenceJobId`는 nullable optional 필드로 문서화한다.
- 각 API 성공 응답은 현재 web response DTO와 맞춘다.
  - generation job: `generationJobId`, `status`, `createdAt`
  - asset: `assetId`, `status`
  - validation log: `validationLogId`, `result`, `assetStatus`
- 공통 오류 응답은 기존 `ApiResponse.error(code, message)` 구조에 맞춰 400, 401, 403, 404, 409, 500 응답 schema를 연결한다.
- 작업 후 DevC report를 작성해 lint 결과와 제외 범위를 남긴다.

## 제외 범위

- `service_accounts` 기반 service account token 검증 필터 구현은 제외한다.
- `/api/v1/system/**` 전역 Spring Security 설정 구현은 제외한다.
- `validation_reference_jobs` 생성, 조회, 만료 API 문서화와 구현은 제외한다.
- `ai_validation_checklist_versions` 관리 API 문서화와 구현은 제외한다.
- 관리자 AI 로그 조회 API 문서화와 구현은 제외한다.
- `inputHash` 요청/응답 필드, 저장 컬럼, unique key 문서화와 구현은 제외한다.
- `SUMMARY`/`GLOSSARY`를 독립 generation job type으로 문서화하지 않는다.
- 실제 DeepSeek 호출, batch worker 실행기, 산출물 생성 로직은 제외한다.
- prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 예시는 OpenAPI example에 포함하지 않는다.

## 주요 계약 결정

| 구분 | OpenAPI 반영 기준 |
| --- | --- |
| 생성 작업 API | `POST /api/v1/system/ai/generation-jobs` |
| 산출물 등록 API | `POST /api/v1/system/ai/assets` |
| 검증 로그 등록 API | `POST /api/v1/system/ai/validation-logs` |
| 권한 | `SYSTEM_BATCH` 필요. 현재 구현은 `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH` authority를 컨트롤러에서 확인 |
| 생성 작업 `jobType` | `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR` |
| 생성 작업 `targetType` | `QT_PASSAGE` |
| 생성 작업 `promptVersionId` | required, positive integer, int64 |
| 산출물 `status` | optional, enum `VALIDATING` |
| 검증 로그 `validationReferenceJobId` | optional, nullable, positive integer, int64 |
| 검증 로그 `result` | `PASSED`, `REJECTED`, `NEEDS_REVIEW` |
| 검증 로그 `reviewerType` | `AUTO`, `ADMIN`, `ADVISOR` |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | 시스템 AI API 3종 paths, request schema, response schema, error response, security 설명 반영 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-openapi-contract_report.md` | 변경 내용, 검증 결과, 제외 범위, 후속 리스크 기록 |
| Reuse | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobController.java` | OpenAPI generation-jobs 경로와 요청/응답 DTO 기준 확인 |
| Reuse | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiAssetController.java` | OpenAPI assets 경로와 요청/응답 DTO 기준 확인 |
| Reuse | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiValidationLogController.java` | OpenAPI validation-logs 경로와 요청/응답 DTO 기준 확인 |

## 구현 순서

1. `SystemAiGenerationJobController`, `SystemAiGenerationJobRequest`, `SystemAiGenerationJobResponse`를 확인해 OpenAPI에 반영할 필드와 HTTP status를 확정한다.
2. `SystemAiAssetController`, `SystemAiAssetRequest`, `SystemAiAssetResponse`를 확인해 OpenAPI에 반영할 필드와 HTTP status를 확정한다.
3. `SystemAiValidationLogController`, `SystemAiValidationLogRequest`, `SystemAiValidationLogResponse`를 확인해 `validationReferenceJobId` nullable optional 계약과 HTTP status를 확정한다.
4. `qtai-server/apis/api-v1/openapi.yaml`의 `paths: {}`를 다중 path 구조로 바꾼다.
5. `/api/v1/system/ai/generation-jobs` `post` operation을 추가한다. `operationId`는 `createSystemAiGenerationJob`로 둔다.
6. generation job request schema에는 `promptVersionId`, `jobType`, `targetType`, `targetId` required를 명시한다.
7. generation job response schema에는 `generationJobId`, `status`, `createdAt`을 명시하고, 성공 응답은 `202`로 둔다.
8. `/api/v1/system/ai/assets` `post` operation을 추가한다. `operationId`는 `registerSystemAiAsset`로 둔다.
9. asset request schema에는 `generationJobId`, `assetType`, `targetType`, `targetId`, `payloadJson` required를 명시한다.
10. asset request의 `status`는 optional `VALIDATING` enum으로 둔다.
11. asset response schema에는 `assetId`, `status`를 명시하고, 성공 응답은 `202`로 둔다.
12. `/api/v1/system/ai/validation-logs` `post` operation을 추가한다. `operationId`는 `registerSystemAiValidationLog`로 둔다.
13. validation log request schema에는 `aiAssetId`, `checklistVersionId`, `layer`, `result`, `checklistJson`, `reviewerType` required를 명시한다.
14. validation log request의 `validationReferenceJobId`는 `nullable: true`이고 required 목록에는 넣지 않는다.
15. validation log response schema에는 `validationLogId`, `result`, `assetStatus`를 명시하고, 성공 응답은 `202`로 둔다.
16. 세 operation에 `400`, `401`, `403`, `404`, `409`, `500` 오류 응답을 연결한다. 없는 리소스 오류가 현재 controller에서 매핑되지 않는 generation job API는 구현과 다른 오류를 만들지 않도록 실제 예외 매핑을 다시 확인한다.
17. `components.schemas.ApiResponse` 기존 구조를 유지하고, 각 성공 응답 wrapper schema를 추가한다.
18. `components.securitySchemes`가 필요하면 `bearerAuth`를 추가하되, service account 기반 인증 게이트는 구현되지 않았음을 description에 명시한다.
19. `inputHash`, `SUMMARY`, `GLOSSARY`가 generation job 요청 schema에 들어가지 않았는지 확인한다.
20. 금지 데이터와 민감정보가 OpenAPI examples에 들어가지 않았는지 확인한다.
21. Spectral lint를 실행한다.
22. `rg`로 시스템 AI API 3종과 핵심 필드가 OpenAPI에 존재하는지 확인한다.
23. `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-openapi-contract_report.md`를 작성한다.

## 검증 항목

| 검증 대상 | 확인할 내용 |
| --- | --- |
| `qtai-server/apis/api-v1/openapi.yaml` | 시스템 AI API 3종 path가 존재한다 |
| `qtai-server/apis/api-v1/openapi.yaml` | `SYSTEM_BATCH` 권한 요구가 세 operation에 명시되어 있다 |
| `qtai-server/apis/api-v1/openapi.yaml` | `generation-jobs.jobType` enum은 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 포함한다 |
| `qtai-server/apis/api-v1/openapi.yaml` | `validationReferenceJobId`는 nullable optional이다 |
| `qtai-server/apis/api-v1/openapi.yaml` | `inputHash` 요청/응답 필드가 없다 |
| `qtai-server/apis/api-v1/openapi.yaml` | `SUMMARY`/`GLOSSARY`가 generation job type으로 문서화되지 않는다 |
| `qtai-server/apis/api-v1/openapi.yaml` | prompt 원문, provider raw response, secret, token, password 예시가 없다 |

## 수용 기준

- [ ] `POST /api/v1/system/ai/generation-jobs`가 OpenAPI에 추가된다.
- [ ] `POST /api/v1/system/ai/assets`가 OpenAPI에 추가된다.
- [ ] `POST /api/v1/system/ai/validation-logs`가 OpenAPI에 추가된다.
- [ ] 세 API 모두 `SYSTEM_BATCH` 권한 요구가 명시된다.
- [ ] `generation-jobs.jobType`은 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 문서화된다.
- [ ] `validationReferenceJobId`는 nullable optional로 문서화된다.
- [ ] `inputHash` 요청/응답 필드는 추가되지 않는다.
- [ ] `SUMMARY`/`GLOSSARY`는 독립 generation job type으로 문서화되지 않는다.
- [ ] `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` 결과를 report에 남긴다.
- [ ] 서버 코드 변경이 없다면 Gradle 테스트 미실행 사유를 report에 남긴다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `qtai-server/apis/api-v1/openapi.yaml` 단일 계약 파일에 집중되어 병렬 편집 이점이 작다.
- 세 API의 schema 이름, 공통 error response, security 설명을 한 파일 안에서 일관되게 맞춰야 하므로 한 에이전트가 직접 수행하는 편이 충돌과 중복을 줄인다.
- 구현 코드는 이미 존재하고, 이번 작업은 코드 변경이 아니라 문서 계약 정합화다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 기존 컨트롤러와 DTO를 확인한 뒤 OpenAPI 수정, lint, `rg` 검증, report 작성을 순서대로 직접 수행한다.

## 검증 계획

- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `rg -n "/api/v1/system/ai|generation-jobs|validation-logs|validationReferenceJobId|promptVersionId" qtai-server/apis/api-v1/openapi.yaml`
- `rg -n "inputHash|SUMMARY|GLOSSARY|provider raw|raw response|password|private key|secret|example.*token" qtai-server/apis/api-v1/openapi.yaml`
- OpenAPI 문서만 변경하는 경우 Gradle 테스트는 실행하지 않고, report에 "문서 계약 변경이라 서버 테스트 미실행"으로 남긴다.

## 후속 작업으로 남길 항목

- `service_accounts` 기반 시스템 토큰 검증 필터 구현
- `/api/v1/system/**` 전역 Spring Security 설정 구현
- `validation_reference_jobs` 생성, 조회, 만료 API 확정 후 OpenAPI와 구현 반영
- `ai_validation_checklist_versions` 관리 API 확정 후 OpenAPI와 구현 반영
- 관리자 AI 로그 조회 API 별도 PR 작성
- `inputHash` 저장 위치와 unique key 반영 여부 Lead 결정
- `SUMMARY`/`GLOSSARY` 독립 generation job type 지원 여부 Lead 결정
