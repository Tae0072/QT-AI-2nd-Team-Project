# Report - 2026-05-26 ai-system-openapi-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-system-openapi-contract` |
| PR 대상 | `dev` |
| 실행 경로 | 직접 실행 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-26_ai-system-openapi-contract.md` |
| 관련 F-ID | 해당 없음 |
| 작성 위치 | `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-openapi-contract_report.md` |

## 작업 결과

구현된 시스템 AI API 3종을 `qtai-server/apis/api-v1/openapi.yaml`에 반영했다.

- `POST /api/v1/system/ai/generation-jobs`
- `POST /api/v1/system/ai/assets`
- `POST /api/v1/system/ai/validation-logs`

이번 작업은 OpenAPI 계약 정합화에 한정했다. 서버 Java 코드, Spring Security 설정, service account 기반 검증 필터, 관리자 조회 API, 검증 참조 작업 API는 수정하지 않았다.

## 변경 요약

1. 기존 `paths: {}`를 실제 path map으로 전환했다.
2. 세 operation에 `operationId`, `requestBody`, `202` 성공 응답, 오류 응답을 추가했다. 단, review-fix 반영 후 `generation-jobs`는 실제 구현의 예외 매핑에 맞춰 `404` 응답을 제거했고 `assets`, `validation-logs`만 `404` 응답을 유지한다.
3. 세 operation 모두 `bearerAuth` security와 `SYSTEM_BATCH`/`ROLE_SYSTEM_BATCH` authority 요구를 description에 명시했다.
4. `generation-jobs` 요청 schema에 `promptVersionId`, `jobType`, `targetType`, `targetId` required를 반영했다.
5. `generation-jobs.jobType` enum은 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 문서화했다.
6. `generation-jobs.targetType` enum은 `QT_PASSAGE`만 문서화했다.
7. `assets` 요청 schema에 `generationJobId`, `assetType`, `targetType`, `targetId`, `payloadJson` required를 반영했다.
8. `assets.status`는 optional nullable이며 제공 시 `VALIDATING`만 허용하도록 문서화했다.
9. `validation-logs` 요청 schema에 `aiAssetId`, `checklistVersionId`, `layer`, `result`, `checklistJson`, `reviewerType` required를 반영했다.
10. `validationReferenceJobId`는 required 목록에서 제외하고 `nullable: true`로 문서화했다.
11. 성공 응답 wrapper schema는 현재 `ApiResponse` 구현의 `success`, `data`, `error`, `timestamp`, `traceId` 구조에 맞췄다.
12. 민감 데이터 또는 원문성 payload 예시는 추가하지 않았다.
13. `traceId`는 MDC 기반 문자열이므로 `format: uuid`를 제거하고 `type: string`만 유지했다.
14. `SystemAiAssetRequest.assetType`, `SystemAiAssetRequest.targetType`, `SystemAiGenerationJobResponse.status`, `SystemAiAssetResponse.status`, `SystemAiValidationLogResponse.result`, `SystemAiValidationLogResponse.assetStatus`에 구현 enum 값을 문서화했다.
15. `ErrorBody.fields`는 현재 `ApiResponse.ErrorBody` 구현에 없으므로 OpenAPI에 추가하지 않고 후속 작업으로 남겼다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/apis/api-v1/openapi.yaml` | 시스템 AI API 3종 path, 요청/응답 schema, 공통 오류 응답, bearer security, review-fix enum/traceId/404 계약 반영 |
| `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-openapi-contract_report.md` | 작업 내용, review-fix 결과, 검증 결과, 제외 범위, 후속 작업 기록 |

## 수용 기준 평가

| 수용 기준 | 상태 | 근거 |
| --- | --- | --- |
| `POST /api/v1/system/ai/generation-jobs`가 OpenAPI에 추가된다 | 충족 | path와 `createSystemAiGenerationJob` operation 추가 |
| `POST /api/v1/system/ai/assets`가 OpenAPI에 추가된다 | 충족 | path와 `registerSystemAiAsset` operation 추가 |
| `POST /api/v1/system/ai/validation-logs`가 OpenAPI에 추가된다 | 충족 | path와 `registerSystemAiValidationLog` operation 추가 |
| 세 API 모두 `SYSTEM_BATCH` 권한 요구가 명시된다 | 충족 | operation description과 `bearerAuth` security에 반영 |
| `generation-jobs.jobType`은 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 문서화된다 | 충족 | request schema enum 확인 |
| `validationReferenceJobId`는 nullable optional로 문서화된다 | 충족 | required 목록에서 제외, `nullable: true` 지정 |
| `inputHash` 요청/응답 필드는 추가하지 않는다 | 충족 | 금지어 검색에서 매치 없음 |
| `SUMMARY`/`GLOSSARY`는 generation job type으로 문서화하지 않는다 | 충족 | `SystemAiGenerationJobRequest.jobType`은 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 유지. `SUMMARY`/`GLOSSARY`는 구현 enum에 맞춰 `assetType`에만 문서화 |
| prompt 원문, provider raw response, secret, token, password 예시는 추가하지 않는다 | 충족 | example을 추가하지 않았고 금지어 검색에서 매치 없음 |
| Gradle 테스트 미실행 사유를 report에 남긴다 | 충족 | 문서 계약 변경만 수행해 서버 테스트는 생략 |
| `POST /api/v1/system/ai/generation-jobs` operation에 `404` response가 없다 | 충족 | 해당 operation은 `202/400/401/403/409/500` 응답만 유지 |
| `traceId` schema는 `type: string`이며 `format: uuid`를 사용하지 않는다 | 충족 | `format: uuid` 검색 매치 없음 |
| `SystemAiAssetRequest.assetType`은 구현 enum 값을 문서화한다 | 충족 | `EXPLANATION`, `SUMMARY`, `GLOSSARY`, `SIMULATOR`, `QA_RESPONSE` 반영 |
| `SystemAiAssetRequest.targetType`은 구현 enum 값을 문서화한다 | 충족 | `BIBLE_VERSE`, `QT_PASSAGE`, `QA_REQUEST` 반영 |
| `SystemAiGenerationJobResponse.status`는 생성 job 상태 enum 값을 문서화한다 | 충족 | `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED` 반영 |
| `SystemAiAssetResponse.status`와 `SystemAiValidationLogResponse.assetStatus`는 산출물 상태 enum 값을 문서화한다 | 충족 | `VALIDATING`, `APPROVED`, `REJECTED`, `HIDDEN` 반영 |
| `SystemAiValidationLogResponse.result`는 검증 결과 enum 값을 문서화한다 | 충족 | `PASSED`, `REJECTED`, `NEEDS_REVIEW` 반영 |
| `ErrorBody.fields`는 이번 OpenAPI에 추가하지 않고 report 후속 작업에 명시한다 | 충족 | 현재 구현과 맞지 않는 `fields`를 추가하지 않았고 후속 작업에 공통 API 결정 항목으로 기록 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `npx.cmd @apidevtools/swagger-cli validate qtai-server/apis/api-v1/openapi.yaml` | 성공. `qtai-server/apis/api-v1/openapi.yaml is valid` |
| `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | 실패. 저장소 루트에 `.spectral.yaml`이 없어 ruleset 파일을 열 수 없음 |
| `rg -n "format: uuid\|/api/v1/system/ai/generation-jobs\|404\|assetType\|targetType\|assetStatus\|traceId" qtai-server/apis/api-v1/openapi.yaml` | 성공. `format: uuid` 매치 없음, `generation-jobs` path와 enum 대상 필드 확인. `404`는 `assets`, `validation-logs` 응답에만 남음 |
| `rg -n "inputHash\|provider raw\|raw response\|password\|private key\|secret\|example.*token" qtai-server/apis/api-v1/openapi.yaml` | 매치 없음. 금지 필드/원문/민감 예시 미포함 확인 |
| `git diff --check -- qtai-server/apis/api-v1/openapi.yaml doc/workspaces/DevC_강상민/reports/2026-05-26_ai-system-openapi-contract_report.md` | 성공. 공백 오류 없음. LF가 CRLF로 바뀔 수 있다는 Git 경고만 출력 |

## 실행하지 않은 검증

| 명령 | 사유 |
| --- | --- |
| `./gradlew -p qtai-server build` | Java 서버 코드 변경 없이 OpenAPI 문서만 변경했으므로 생략 |
| `./gradlew -p qtai-server test jacocoTestReport` | Java 서버 코드 변경 없이 OpenAPI 문서만 변경했으므로 생략 |
| `./gradlew -p qtai-server jacocoTestCoverageVerification` | Java 서버 코드 변경 없이 OpenAPI 문서만 변경했으므로 생략 |
| `gitleaks detect --source . --redact --exit-code 1` | 이번 workflow 검증 계획에 포함되지 않았고, OpenAPI 예시에는 민감 값을 추가하지 않음 |

## 제외 범위 준수

| 제외 항목 | 처리 |
| --- | --- |
| service account 기반 검증 필터 구현 | 미수정, description에 후속 범위로 명시 |
| `/api/v1/system/**` 전역 Spring Security 설정 | 미수정 |
| `validation_reference_jobs` 생성/조회/만료 API | 미문서화 |
| `ai_validation_checklist_versions` 관리 API | 미문서화 |
| 관리자 AI 로그 조회 API | 미문서화 |
| `inputHash` 요청/응답 필드와 저장 컬럼 | 미문서화 |
| `SUMMARY`/`GLOSSARY` generation job type | 미문서화 |
| DeepSeek 호출, batch worker, 산출물 생성 로직 | 미수정 |
| prompt 원문, provider raw response, 검증 참조 원문 전체, secret, token, password 예시 | 미포함 |

## 후속 작업

1. 공통 `ApiResponse.ErrorBody`에 `fields`를 추가할지 Lead 또는 공통 API 소유자가 결정
2. `fields` 지원 시 Java `ApiResponse`, `GlobalExceptionHandler`, OpenAPI `ErrorBody`, 관련 테스트를 같은 PR에서 정합화
3. 저장소 표준 `.spectral.yaml` ruleset 위치 확정 또는 추가
4. `service_accounts` 기반 시스템 인증 필터 구현
5. `/api/v1/system/**` Spring Security 전역 보호 설정 구현
6. `validation_reference_jobs` 생성, 조회, 만료 API 확정 후 OpenAPI와 구현 반영
7. `ai_validation_checklist_versions` 관리 API 확정 후 OpenAPI와 구현 반영
8. 관리자 AI 로그 조회 API 별도 PR 작성
9. `inputHash` 저장 위치와 unique key 반영 여부 Lead 결정
10. `SUMMARY`/`GLOSSARY` generation job type 지원 여부 Lead 결정
