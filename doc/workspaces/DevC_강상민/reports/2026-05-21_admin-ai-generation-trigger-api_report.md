# Report — 2026-05-21 admin-ai-generation-trigger-api

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-generation-trigger` |
| PR 대상 | `dev` |
| 실행 경로 | 직접 실행 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-21_admin-ai-generation-trigger-api.md` |
| 관련 F-ID | F-02, F-06, F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/reports/2026-05-21_admin-ai-generation-trigger-api_report.md` |

## 경로 확인

요청 경로 `doc/Dev_C/강상민/reports`는 현재 저장소에 존재하지 않는다. 기존 DevC 작업 문서와 리포트는 `doc/workspaces/DevC_강상민/**` 아래에 관리되고 있으므로, 프로젝트의 실제 리포트 경로인 `doc/workspaces/DevC_강상민/reports`에 작성했다.

## 작업 결과

관리자 웹에서 AI 산출물 재생성을 요청하는 API를 구현했다. 대상 API는 `POST /api/v1/admin/ai/assets/{assetId}/regenerate`이며, 요청 성공 시 기존 `ai_generated_assets`와 `ai_validation_logs`를 덮어쓰지 않고 새 `ai_generation_jobs` 작업을 `QUEUED` 상태로 등록한다.

이번 작업은 재생성 요청 접수와 job 큐잉까지만 구현했다. 실제 DeepSeek 호출, 시스템 AI 생성 API, 산출물 payload 생성, 자동 검증 로그 등록, 관리자 UI 구현은 workflow의 제외 범위에 따라 구현하지 않았다.

## 변경 요약

1. `domain.ai.api`에 `RegenerateAiAssetUseCase`를 추가했다.
2. UseCase DTO로 `RegenerateAiAssetCommand`, `RegenerateAiAssetResult` record를 추가했다.
3. HTTP DTO로 `RegenerateAiAssetRequest`, `RegenerateAiAssetResponse`를 `domain.ai.web`에 추가했다.
4. `AdminAiAssetController`를 추가해 `/api/v1/admin/ai/assets/{assetId}/regenerate`를 `202 Accepted`로 노출했다.
5. `AiService`가 `RegenerateAiAssetUseCase`를 구현하도록 변경했다.
6. `REJECTED`, `HIDDEN` 상태만 재생성 가능하도록 제한했다.
7. `VALIDATING`, `APPROVED` 상태는 `INVALID_STATUS_TRANSITION`으로 차단했다.
8. 같은 대상, job type, prompt version에 `QUEUED` 또는 `RUNNING` job이 있으면 중복 재생성을 차단했다.
9. 감사 로그는 이번 PR에서 `domain.audit.api` 계약을 변경하지 않고, AI 코드에 `AI_REGENERATE_REQUEST` 연결 필요성만 남겼다.
10. `ErrorCode.INVALID_STATUS_TRANSITION`을 추가했다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/api/RegenerateAiAssetUseCase.java` | 관리자 재생성 UseCase 계약 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/RegenerateAiAssetCommand.java` | 재생성 command DTO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/RegenerateAiAssetResult.java` | 재생성 result DTO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiAssetController.java` | 관리자 재생성 HTTP API 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/RegenerateAiAssetRequest.java` | HTTP 요청 DTO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/RegenerateAiAssetResponse.java` | HTTP 응답 DTO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | 상태 검증, 중복 job 차단, 새 job 생성, 감사 로그 후속 연결 TODO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRepository.java` | 진행 중 job 존재 여부 조회 메서드 추가 |
| `qtai-server/src/main/java/com/qtai/common/exception/ErrorCode.java` | `INVALID_STATUS_TRANSITION` 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` | 재생성 상태 정책, 중복 차단, 권한 검증 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | HTTP 요청/응답 매핑과 오류 응답 검증 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | 새 UseCase와 DTO 계약 검증에 포함 |

## 정책 반영

| 정책 | 반영 결과 |
| --- | --- |
| API 경로 | `POST /api/v1/admin/ai/assets/{assetId}/regenerate`로 노출 |
| 권한 | `memberRole=ADMIN`과 `adminRole=REVIEWER` 또는 `SUPER_ADMIN` 모두 요구 |
| 요청 필드 | `reason`, `promptVersionId` 사용 |
| 성공 응답 | `generationJobId`, `status`, `createdAt` 반환 |
| 성공 상태 코드 | 비동기 접수 기준에 따라 `202 Accepted` 반환 |
| 허용 상태 | `REJECTED`, `HIDDEN` |
| 차단 상태 | `VALIDATING`, `APPROVED` |
| 중복 차단 | 같은 target, job type, prompt version의 `QUEUED`/`RUNNING` job 차단 |
| 이력 보존 | 기존 asset/log를 수정하지 않고 새 job만 생성 |
| 감사 로그 | `domain.audit.api` 계약 변경은 제외하고, AI 코드에 `AI_REGENERATE_REQUEST` 연결 TODO를 남김 |
| 민감 데이터 | prompt 원문, provider raw response, 검증 참조 원문을 요청/응답/감사 로그에 포함하지 않음 |

## 테스트 보강

| 테스트 파일 | 검증 내용 |
| --- | --- |
| `AiServiceTest` | `REJECTED` 산출물이 `QUEUED` 재생성 job을 만드는지 검증 |
| `AiServiceTest` | `HIDDEN` 산출물은 `SUPER_ADMIN`도 재생성할 수 있는지 검증 |
| `AiServiceTest` | `VALIDATING`, `APPROVED` 산출물은 재생성을 차단하는지 검증 |
| `AiServiceTest` | 기존 `QUEUED`/`RUNNING` job이 있으면 중복 생성하지 않는지 검증 |
| `AiServiceTest` | `ADMIN` 역할과 `REVIEWER`/`SUPER_ADMIN` 세부 권한을 모두 요구하는지 검증 |
| `AdminAiAssetControllerTest` | 재생성 요청 DTO를 UseCase command로 매핑하고 `202 Accepted` 응답을 반환하는지 검증 |
| `AdminAiAssetControllerTest` | 권한 부족은 `403`, 상태 전이 불가는 `409`로 응답하는지 검증 |
| `AiUseCaseContractTest` | 새 UseCase와 DTO가 command/result record 계약을 유지하는지 검증 |

## 수용 기준 점검

| 수용 기준 | 상태 | 근거 |
| --- | --- | --- |
| 관리자 재생성 API가 지정 경로로 노출된다 | 충족 | `AdminAiAssetController`에 `POST /api/v1/admin/ai/assets/{assetId}/regenerate` 추가 |
| 성공 시 새 `ai_generation_jobs`가 `QUEUED` 상태로 생성된다 | 충족 | `AiService.regenerateAiAsset`에서 `AiGenerationJob.queue(...)` 저장 |
| 기존 `ai_generated_assets`와 `ai_validation_logs`는 덮어쓰지 않는다 | 충족 | 기존 asset은 조회만 하고 validation log repository를 사용하지 않음 |
| `REJECTED`, `HIDDEN` 상태만 재생성 요청을 허용한다 | 충족 | `requireRegeneratableStatus`와 서비스 테스트로 확인 |
| `VALIDATING`, `APPROVED` 상태는 명확한 오류로 차단한다 | 충족 | `INVALID_STATUS_TRANSITION`으로 차단하고 테스트 추가 |
| 같은 대상의 진행 중 생성 작업이 있으면 중복 요청을 차단한다 | 충족 | `existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn` 조회로 차단 |
| 관리자 권한은 `ADMIN` 역할과 세부 관리자 권한을 모두 확인한다 | 충족 | `ADMIN + REVIEWER/SUPER_ADMIN`만 허용 |
| 재생성 요청은 감사 로그로 남는다 | 후속 분리 | audit 소유자가 `domain.audit.api` 계약을 확정한 뒤 `AI_REGENERATE_REQUEST` 연결 필요 |
| prompt 원문, provider raw response, 검증 참조 원문, secret, token은 요청·응답·로그에 남지 않는다 | 충족 | 요청/응답 DTO에 원문성 필드가 없고, 감사 metadata 구현은 이번 PR에서 제외 |
| 사용자 앱 경로 또는 `/api/v1/ai/**`에서 해설·시뮬레이터 생성이 시작되지 않는다 | 충족 | 관리자 `/api/v1/admin/ai/**` 경로에만 API 추가 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `gradle test --tests "*Ai*"` | 통과 |
| `gradle test` | 통과, `44 tests, 0 failures, 0 errors` |
| `gradle build` | 통과 |
| `rg -n "^import .*domain\\.[a-z]+\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |
| `git diff --check` | 공백 오류 없음. CRLF 변환 경고만 출력됨 |

저장소 루트와 `qtai-server`에 `gradlew`가 없어 사용자 Gradle 캐시에 있는 Gradle 8.14 배포본으로 검증했다.

## 생략한 검증

| 명령 | 사유 |
| --- | --- |
| `npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml` | OpenAPI 파일을 수정하지 않았고, 저장소 루트와 `qtai-server`에 `.spectral.yaml`이 없음 |
| `gitleaks detect --source . --redact --exit-code 1` | 현재 환경에 `gitleaks` 실행 파일이 설치되어 있지 않음 |
| `jacocoTestReport`, `jacocoTestCoverageVerification` | 현재 Gradle task 목록에서 Jacoco 관련 task가 확인되지 않음 |

## 남은 후속 작업

1. `POST /api/v1/system/ai/generation-jobs` 시스템 API 구현
2. 실제 DeepSeek 호출과 산출물 생성 파이프라인 연결
3. AI 검증 로그 자동 등록 및 검증 체크리스트 연동
4. `APPROVED` 산출물의 조건부 재생성 정책과 기존 노출본 교체 정책 확정
5. 관리자 웹 화면에서 재생성 버튼과 실패/대기 상태 표시
6. `promptVersionId`와 `ai_prompt_versions` 저장 구조 정합화
7. Lead/audit 소유자와 `WriteAuditLogUseCase` 계약 확정 후 `AI_REGENERATE_REQUEST` 기록 연결
