# Workflow — 2026-05-26 ai-system-openapi-review-fixes

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-system-openapi-contract` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | `docs(ai): 시스템 AI OpenAPI 계약 반영` PR 리뷰에서 OpenAPI 계약 정합성 보완 항목 제기 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 담당 경로 | `qtai-server/apis/api-v1/openapi.yaml`, `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-openapi-contract_report.md` |

## 기준 문서

- `doc/workspaces/DevC_강상민/workflows/2026-05-26_ai-system-openapi-contract.md`
- `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-openapi-contract_report.md`
- `doc/프로젝트 문서/03_아키텍처_정의서.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `qtai-server/src/main/java/com/qtai/common/dto/ApiResponse.java`
- `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobController.java`
- `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java`
- `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogUseCaseService.java`

## 작업 목표

시스템 AI OpenAPI 계약 PR 리뷰에서 확인된 구현 불일치와 클라이언트 계약 모호성을 바로잡는다. 이번 작업은 기존 구현을 바꾸지 않고 OpenAPI 문서와 DevC report만 수정해, 문서가 실제 서버 응답과 enum 계약을 정확히 설명하도록 만드는 데 집중한다.

특히 `generation-jobs`의 실제 예외 매핑과 맞지 않는 404 응답을 제거하고, MDC 기반 `traceId`가 UUID가 아닐 수 있는 점을 반영해 `format: uuid`를 제거한다. 또한 assets 요청과 주요 응답 필드의 enum을 보강해 클라이언트가 기대 가능한 값 범위를 알 수 있게 한다.

## 범위

- `POST /api/v1/system/ai/generation-jobs` OpenAPI operation에서 `404` 응답을 제거한다.
- `traceId` schema에서 `format: uuid`를 제거하고 `type: string`만 유지한다.
- `SystemAiAssetRequest.assetType`에 구현 enum 기준 허용 값을 문서화한다.
- `SystemAiAssetRequest.targetType`에 구현 enum 기준 허용 값을 문서화한다.
- `SystemAiGenerationJobResponse.status`에 생성 job 상태 enum을 문서화한다.
- `SystemAiAssetResponse.status`에 산출물 상태 enum을 문서화한다.
- `SystemAiValidationLogResponse.result`에 검증 결과 enum을 문서화한다.
- `SystemAiValidationLogResponse.assetStatus`에 산출물 상태 enum을 문서화한다.
- `ErrorBody.fields`는 실제 `ApiResponse.ErrorBody` 구현에 없으므로 OpenAPI에 선반영하지 않고 후속 작업으로 report에 명시한다.
- OpenAPI 검증과 review-fix 결과를 `2026-05-26_ai-system-openapi-contract_report.md`에 추가한다.

## 제외 범위

- `ApiResponse.ErrorBody`에 `fields`를 추가하는 Java 코드 변경은 제외한다.
- 공통 오류 응답 envelope 정책 변경은 제외한다.
- `SystemAiGenerationJobController`, `AiService`, `AiLogUseCaseService` 구현 변경은 제외한다.
- `*AcceptedResponse` schema 중복을 `allOf`로 리팩터링하는 작업은 제외한다.
- service account 기반 인증 필터와 `/api/v1/system/**` 전역 보안 설정은 제외한다.
- `validation_reference_jobs`, `ai_validation_checklist_versions`, 관리자 AI 로그 조회 API 문서화와 구현은 제외한다.
- `inputHash`, `SUMMARY`, `GLOSSARY` generation job type 지원 여부는 계속 후속 결정으로 둔다.
- OpenAPI example 추가는 제외한다. prompt 원문, provider raw response, secret, token, password 예시는 추가하지 않는다.

## 주요 계약 결정

| 구분 | 결정 |
| --- | --- |
| `generation-jobs` 404 | 현재 `SystemAiGenerationJobController`는 `AI_GENERATION_JOB_NOT_FOUND`를 404로 매핑하지 않고, `promptVersionId` 미존재도 `INVALID_INPUT`으로 400 처리하므로 404 응답을 문서에서 제거 |
| `traceId` format | MDC traceId는 UUID가 아닐 수 있고 API 문서 예시도 `01HX...` 형태이므로 `format: uuid` 제거 |
| assetType enum | 구현 enum `AiGeneratedAssetType` 기준 `EXPLANATION`, `SUMMARY`, `GLOSSARY`, `SIMULATOR`, `QA_RESPONSE` 문서화 |
| asset targetType enum | 구현 enum `AiTargetType` 기준 `BIBLE_VERSE`, `QT_PASSAGE`, `QA_REQUEST` 문서화 |
| generation job status enum | 구현 enum `AiGenerationJobStatus` 기준 `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED` 문서화 |
| asset status enum | 구현 enum `AiGeneratedAssetStatus` 기준 `VALIDATING`, `APPROVED`, `REJECTED`, `HIDDEN` 문서화 |
| validation result enum | 구현 enum `AiValidationResult` 기준 `PASSED`, `REJECTED`, `NEEDS_REVIEW` 문서화 |
| ErrorBody.fields | 기준 문서와 구현 간 gap으로 report 후속 작업에 명시. 이번 PR에서 OpenAPI만 앞서가지 않게 유지 |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | 404 제거, traceId format 수정, request/response enum 보강 |
| Modify | `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-openapi-contract_report.md` | 리뷰 반영 내용, 검증 결과, `ErrorBody.fields` 후속 항목 기록 |
| Reuse | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobController.java` | generation-jobs 실제 예외 매핑 확인 |
| Reuse | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | promptVersionId 미존재 처리와 generation job status enum 확인 |
| Reuse | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogUseCaseService.java` | assets/validation enum 파싱 흐름 확인 |

## 구현 순서

1. `SystemAiGenerationJobController.handleBusinessException(...)`와 `AiService.requireUsablePromptVersion(...)`를 재확인한다.
2. `generation-jobs` operation의 `404` response를 제거한다.
3. `ApiResponse`, `ErrorApiResponse`, 각 성공 wrapper schema의 `traceId.format: uuid`를 제거한다.
4. `SystemAiAssetRequest.assetType`에 `EXPLANATION`, `SUMMARY`, `GLOSSARY`, `SIMULATOR`, `QA_RESPONSE` enum을 추가한다.
5. `SystemAiAssetRequest.targetType`에 `BIBLE_VERSE`, `QT_PASSAGE`, `QA_REQUEST` enum을 추가한다.
6. `SystemAiGenerationJobResponse.status`에 `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED` enum을 추가한다.
7. `SystemAiAssetResponse.status`와 `SystemAiValidationLogResponse.assetStatus`에 `VALIDATING`, `APPROVED`, `REJECTED`, `HIDDEN` enum을 추가한다.
8. `SystemAiValidationLogResponse.result`에 `PASSED`, `REJECTED`, `NEEDS_REVIEW` enum을 추가한다.
9. `ErrorBody`는 현재 구현 기준 `code`, `message`만 유지한다.
10. report에 리뷰 반영 요약과 `ErrorBody.fields` 후속 작업을 추가한다.
11. OpenAPI 구조 검증과 금지어 검색을 실행한다.
12. 서버 Java 코드 변경이 없으면 Gradle 테스트는 선택 검증으로만 실행하고, 실행 여부와 결과를 report에 남긴다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | OpenAPI 문서 정정만 수행하므로 Java 테스트 추가 없음 |

## 수용 기준

- [ ] `POST /api/v1/system/ai/generation-jobs` operation에 `404` response가 없다.
- [ ] `traceId` schema는 `type: string`이며 `format: uuid`를 사용하지 않는다.
- [ ] `SystemAiAssetRequest.assetType`은 구현 enum 값을 문서화한다.
- [ ] `SystemAiAssetRequest.targetType`은 구현 enum 값을 문서화한다.
- [ ] `SystemAiGenerationJobResponse.status`는 생성 job 상태 enum 값을 문서화한다.
- [ ] `SystemAiAssetResponse.status`는 산출물 상태 enum 값을 문서화한다.
- [ ] `SystemAiValidationLogResponse.result`는 검증 결과 enum 값을 문서화한다.
- [ ] `SystemAiValidationLogResponse.assetStatus`는 산출물 상태 enum 값을 문서화한다.
- [ ] `ErrorBody.fields`는 이번 OpenAPI에 추가하지 않고 report 후속 작업에 명시한다.
- [ ] `inputHash`, `SUMMARY`, `GLOSSARY`, prompt 원문, provider raw response, secret, token, password 예시가 OpenAPI에 추가되지 않는다.
- [ ] OpenAPI validate 또는 동등 구조 검증 결과를 report에 남긴다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `qtai-server/apis/api-v1/openapi.yaml`와 기존 report 파일에 집중되어 병렬 편집 이점이 작다.
- enum과 오류 응답을 한 OpenAPI 문서 안에서 일관되게 맞춰야 하므로 단일 에이전트 직접 실행이 충돌 가능성을 줄인다.
- Java 코드 변경 없이 문서 정합성만 조정하는 작업이다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 실제 컨트롤러와 enum 구현을 확인한 뒤 OpenAPI 수정, report 갱신, 검증을 순서대로 직접 수행한다.

## 검증 계획

- `npx.cmd @apidevtools/swagger-cli validate qtai-server/apis/api-v1/openapi.yaml`
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `rg -n "format: uuid|/api/v1/system/ai/generation-jobs|404|assetType|targetType|assetStatus|traceId" qtai-server/apis/api-v1/openapi.yaml`
- `rg -n "inputHash|provider raw|raw response|password|private key|secret|example.*token" qtai-server/apis/api-v1/openapi.yaml`
- Java 코드 변경이 없더라도 PR 안정성 확인이 필요하면 `.\qtai-server\gradlew.bat -p qtai-server build`를 실행한다.

## 후속 작업으로 남길 항목

- 공통 `ApiResponse.ErrorBody`에 `fields`를 추가할지 Lead 또는 공통 API 소유자가 결정한다.
- `fields` 지원을 구현하는 경우 Java `ApiResponse`, `GlobalExceptionHandler`, OpenAPI `ErrorBody`, 관련 테스트를 같은 PR에서 함께 정합화한다.
- 저장소 표준 `.spectral.yaml` ruleset 위치를 확정하거나 ruleset 파일을 추가한다.
- service account 기반 시스템 토큰 검증 필터와 `/api/v1/system/**` 전역 보안 설정을 구현한다.
