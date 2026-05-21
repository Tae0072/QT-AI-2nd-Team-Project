# Report — 2026-05-21 admin-ai-generation-trigger-review-fixes

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-generation-trigger` |
| PR 대상 | `dev` |
| 실행 경로 | REQUEST_CHANGES 리뷰 대응 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-21_admin-ai-generation-trigger-api.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-21_create-ai-generation-job-usecase-implementation.md` |
| 관련 F-ID | F-02, F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/reports/2026-05-21_admin-ai-generation-trigger-review-fixes_report.md` |

## 작업 결과

PR 리뷰에서 머지 전 보강이 필요하다고 지적된 관리자 인증, AI 생성 job 멱등성, 공통 API envelope 항목을 반영했다.

관리자 재생성 API는 더 이상 `X-Admin-Id`, `X-Member-Role`, `X-Admin-Role` 같은 요청 헤더를 신뢰하지 않는다. Spring Security `Authentication` 또는 `SecurityContextHolder`에서 인증 주체와 권한을 읽고, `ADMIN` 회원 역할과 `REVIEWER` 또는 `SUPER_ADMIN` 세부 관리자 권한이 모두 있을 때만 UseCase를 호출한다.

AI 생성 작업은 service-level `exists -> save` 검사만으로 중복을 막지 않고, `active_unique_key`와 JPA unique constraint로 진행 중 작업의 DB 레벨 중복 방어선을 추가했다. 공통 `ApiResponse`에는 API 명세의 `timestamp`, `traceId` 필드를 추가했다.

## 리뷰 지적별 대응

| 리뷰 지적 | 대응 결과 |
| --- | --- |
| 관리자 식별·권한을 HTTP 헤더로 받는 구조는 인증 우회 가능 | `AdminAiAssetController`에서 권한 헤더 제거. `Authentication`/`SecurityContextHolder` 기반으로 관리자 메타데이터 추출 |
| 위조 헤더 negative test 필요 | 인증 없이 위조 헤더만 보낸 요청은 `401`, 권한이 부족한 SecurityContext에 위조 헤더를 더해도 `403` 반환 테스트 추가 |
| `promptVersion` 문자열과 `String.valueOf(promptVersionId)` 혼용으로 멱등성 키 어긋남 위험 | 관리자 재생성 job은 대상 asset의 canonical `asset.getPromptVersion()`을 중복 조회와 저장에 동일하게 사용 |
| `exists -> save` 비원자성 race condition | `active_unique_key`와 unique constraint 추가, `saveAndFlush`로 DB 충돌을 UseCase 내부에서 `INVALID_STATUS_TRANSITION`으로 매핑 |
| API envelope 표준의 `timestamp`, `traceId` 누락 | `ApiResponse`에 `timestamp`, `traceId` 필드 추가. MDC `traceId` 우선 사용, 없으면 UUID 생성 |

## 변경 요약

1. `AdminAiAssetController`의 관리자 메타데이터 입력을 요청 헤더에서 SecurityContext 기반으로 변경했다.
2. `ROLE_ADMIN` 또는 `MEMBER_ROLE_ADMIN`이 없으면 `FORBIDDEN`, `ADMIN_ROLE_REVIEWER` 또는 `ADMIN_ROLE_SUPER_ADMIN`이 없으면 `FORBIDDEN`으로 차단한다.
3. 인증이 없거나 principal id를 파싱할 수 없으면 `UNAUTHORIZED`로 차단한다.
4. `AiService.regenerateAiAsset`에서 `String.valueOf(command.promptVersionId())`를 제거하고 `asset.getPromptVersion()`을 사용한다.
5. `AiGenerationJob`에 `active_unique_key`를 추가하고, 진행 중 job은 `ACTIVE`, 종료 상태는 `NULL`로 관리한다.
6. `ai_generation_jobs`에 `job_type + target_type + target_id + prompt_version + active_unique_key` unique constraint를 선언했다.
7. `AiService` 저장 경로를 `saveAndFlush`로 바꿔 unique 충돌을 즉시 감지하고 공통 비즈니스 예외로 변환한다.
8. `ApiResponse`에 `timestamp`, `traceId`를 추가해 공통 응답 envelope 문서 기준에 맞췄다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiAssetController.java` | 관리자 권한 추출을 헤더 기반에서 SecurityContext 기반으로 변경 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | 인증 기반 정상 요청, 위조 헤더 차단, envelope 필드 검증 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | canonical promptVersion 사용, `saveAndFlush`와 unique 충돌 예외 매핑 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` | promptVersion 기대값 수정, unique 충돌 매핑 테스트 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJob.java` | `active_unique_key` 필드와 진행 중 job unique constraint 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobTest.java` | active unique key 상태 전이 테스트 추가 |
| `qtai-server/src/main/java/com/qtai/common/dto/ApiResponse.java` | `timestamp`, `traceId` 공통 envelope 필드 추가 |

## 테스트 보강

| 테스트 파일 | 검증 내용 |
| --- | --- |
| `AdminAiAssetControllerTest` | 인증된 `ROLE_ADMIN + ADMIN_ROLE_REVIEWER` 요청이 UseCase command로 매핑되는지 검증 |
| `AdminAiAssetControllerTest` | 위조 권한 헤더만 있는 요청은 `UNAUTHORIZED`로 차단하고 UseCase를 호출하지 않는지 검증 |
| `AdminAiAssetControllerTest` | SecurityContext 권한이 부족하면 위조 헤더가 있어도 `FORBIDDEN`으로 차단하는지 검증 |
| `AdminAiAssetControllerTest` | 성공/오류 응답 envelope에 `timestamp`, `traceId`가 포함되는지 검증 |
| `AiServiceTest` | 관리자 재생성 job의 중복 키와 저장값이 기존 asset의 `promptVersion`을 사용하는지 검증 |
| `AiServiceTest` | DB unique 충돌이 `INVALID_STATUS_TRANSITION`으로 매핑되는지 검증 |
| `AiGenerationJobTest` | `QUEUED/RUNNING` 동안 `activeUniqueKey=ACTIVE`, `SUCCEEDED/FAILED`에서 `NULL`로 바뀌는지 검증 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `gradle -p qtai-server test --tests "*AdminAiAssetControllerTest"` | 통과 |
| `gradle -p qtai-server test --tests "*AiServiceTest" --tests "*AiGenerationJobTest" --tests "*AdminAiAssetControllerTest"` | 통과 |
| `gradle -p qtai-server clean build --no-build-cache` | 통과 |
| `git diff --check` | 공백 오류 없음. CRLF 변환 경고만 출력됨 |
| `Get-ChildItem ... | Select-String 'X-Admin-Id','X-Member-Role','X-Admin-Role','@RequestHeader(\"X-'` | main AI 코드에서 권한 헤더 기반 인증 잔존 없음 |
| `rg -n "String\\.valueOf\\(command\\.promptVersionId\\)" qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |

저장소 루트와 `qtai-server`에 `gradlew`가 없고 시스템 `gradle` 명령도 설치되어 있지 않아, `%TEMP%/codex-gradle/gradle-8.10.2` 임시 Gradle 배포본으로 검증했다.

## 남은 후속 작업

1. `promptVersionId`와 `ai_prompt_versions` 조회/매핑 구조를 후속 PR에서 확정한다.
2. 실제 DB migration 도입 시 `ai_generation_jobs.active_unique_key` 컬럼과 unique index를 migration 파일로 명시한다.
3. 프로젝트 전체 예외 처리를 `@ControllerAdvice` 기반 GlobalExceptionHandler로 일원화한다.
4. `traceId` 발급·전파 필터를 공통 web/filter 계층에서 확정한다.
5. 관리자 인증 principal 구조가 확정되면 `AdminAuthentication` 추출 로직을 공통 security adapter로 분리한다.
