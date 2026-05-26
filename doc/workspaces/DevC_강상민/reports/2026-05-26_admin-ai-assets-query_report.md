# Report - 2026-05-26 admin-ai-assets-query

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-ai-assets-query` |
| PR 대상 | `dev` |
| 실행 경로 | 직접 실행 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-26_admin-ai-assets-query.md` |
| 관련 F-ID | F-06, F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/reports/2026-05-26_admin-ai-assets-query_report.md` |

## 작업 결과

관리자 웹에서 AI 산출물 검토 대상을 조회할 수 있도록 `GET /api/v1/admin/ai/assets` 목록 API와 `GET /api/v1/admin/ai/assets/{assetId}` 상세 API를 구현했다.

두 API는 기존 관리자 재생성 API와 같은 `/api/v1/admin/ai/assets` 컨트롤러에 추가했으며, `ROLE_ADMIN`과 `ADMIN_ROLE_REVIEWER` 또는 `ADMIN_ROLE_SUPER_ADMIN` authority만 허용한다. `ADMIN_ROLE_OPERATOR`는 이번 PR에서 `403`으로 차단한다.

목록 조회는 `ai_generated_assets`, `ai_generation_jobs`, `ai_prompt_versions`, 최신 `ai_validation_logs`를 조합해 `createdAt,desc` 기준으로 반환한다. 상세 조회는 저장된 `payload_json`을 파싱해 반환하고, 검증 로그는 `createdAt desc`, 같은 시각이면 `id desc` 순서로 제공한다.

## 변경 요약

1. `ListAdminAiAssetsUseCase`, `GetAdminAiAssetUseCase`와 관리자 조회 DTO를 추가했다.
2. `AdminAiAssetController`에 목록/상세 GET endpoint를 추가하고 기존 재생성 endpoint는 유지했다.
3. `AdminAiAssetQueryService`에 관리자 권한 2차 검증, 페이지 응답 변환, 상세 payload 파싱을 구현했다.
4. `AdminAiAssetQueryRepository`를 EntityManager 기반 JPQL 조회로 추가했다.
5. 컨트롤러, 서비스, repository, UseCase 계약 테스트를 보강했다.
6. `qtai-server/apis/api-v1/openapi.yaml`에 관리자 AI 조회 API 2종과 schema를 반영했다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/api/ListAdminAiAssetsUseCase.java` | 관리자 AI 산출물 목록 조회 UseCase 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/api/GetAdminAiAssetUseCase.java` | 관리자 AI 산출물 상세 조회 UseCase 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/*AdminAi*`, `*Query.java` | 목록/상세 query, 응답, 검증 로그 DTO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryService.java` | read-only 조회 서비스, 권한 검증, 응답 조립 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java` | EntityManager 기반 목록/상세/검증 로그 조회 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiAssetController.java` | GET 목록/상세 endpoint 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/**` | 컨트롤러, 서비스, repository, API 계약 테스트 보강 |
| `qtai-server/apis/api-v1/openapi.yaml` | Admin AI path와 schema 추가 |

## 수용 기준 평가

| 수용 기준 | 상태 | 근거 |
| --- | --- | --- |
| `GET /api/v1/admin/ai/assets` 구현 | 충족 | controller, UseCase, service, repository 테스트 |
| `GET /api/v1/admin/ai/assets/{assetId}` 구현 | 충족 | 상세 controller/service/repository 테스트 |
| `ADMIN + REVIEWER/SUPER_ADMIN`만 허용 | 충족 | controller/service 권한 테스트 |
| `OPERATOR`는 `403` 차단 | 충족 | controller/service 권한 테스트 |
| 목록 필터 5종 지원 | 충족 | repository 필터 테스트 |
| 목록 기본 정렬 `createdAt,desc` | 충족 | repository 페이지/정렬 테스트 |
| 목록 페이지 응답 구조 | 충족 | `AdminAiAssetListResponse`, controller 테스트 |
| 목록 item 최신 검증 결과/체크리스트 버전 포함 | 충족 | repository 최신 로그 테스트 |
| 상세 응답에 산출물, job, prompt version, payload, validation logs 포함 | 충족 | controller/service/repository 상세 테스트 |
| 상세 payload는 저장된 payload 외 필드를 새로 조립하지 않음 | 충족 | service/repository 테스트 |
| 없는 asset은 `404 AI_ASSET_NOT_FOUND` | 충족 | controller/service 테스트 |
| OpenAPI 반영 | 충족 | swagger-cli validate 성공 |
| 사용자 앱 경로에 관리자 조회 API 미추가 | 충족 | `rg` 검색 매치 없음 |
| AI 도메인 금지 import 없음 | 충족 | `rg` 검색 매치 없음 |
| 민감/원문성 예시 미포함 | 충족 | `rg` 검색 매치 없음 |
| `checkList.md`의 OPERATOR 확인 항목 유지 | 충족 | 해당 파일 수정 없음 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `Push-Location qtai-server; .\gradlew.bat test --tests "*AdminAiAssetControllerTest" --tests "*AdminAiAssetQueryServiceTest" --tests "*AiUseCaseContractTest"; Pop-Location` | 성공 |
| `Push-Location qtai-server; .\gradlew.bat test --tests "*AdminAiAssetQueryRepositoryTest"; Pop-Location` | 성공 |
| `Push-Location qtai-server; .\gradlew.bat test --tests "*Ai*"; Pop-Location` | 성공 |
| `Push-Location qtai-server; .\gradlew.bat build; Pop-Location` | 성공 |
| `npx.cmd @apidevtools/swagger-cli validate qtai-server/apis/api-v1/openapi.yaml` | 성공 |
| `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | 실패. 저장소 루트에 `.spectral.yaml`이 없어 ruleset 파일을 열 수 없음 |
| `rg -n "^import .*domain\\.[a-z]+\\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |
| `rg -n -g '*.java' 'RequestMapping\\("/api/v1/ai\|GetMapping.*admin/ai' qtai-server/src/main/java/com/qtai/domain/ai/web` | 매치 없음 |
| `rg -n "raw response\|provider raw\|password\|private key\|secret\|example.*token" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai qtai-server/apis/api-v1/openapi.yaml` | 매치 없음 |
| `git diff --check` | 성공. CRLF 변환 경고만 출력 |

## 실행하지 않은 검증

| 명령 | 사유 |
| --- | --- |
| `jacocoTestReport`, `jacocoTestCoverageVerification` | 현재 `qtai-server` Gradle task 목록에 Jacoco task가 등록되어 있지 않음 |
| `gitleaks detect --source . --redact --exit-code 1` | 현재 환경에서 `gitleaks` 실행 파일을 찾지 못함 |

## 제외 범위 준수

| 제외 항목 | 처리 |
| --- | --- |
| 승인, 반려, 숨김 API | 미구현 |
| 평가 후보 등록 API | 미구현 |
| 관리자 재생성 API 기존 동작 변경 | 기존 endpoint 유지 |
| `OPERATOR` 조회 허용 | 미허용, Lead 확인 항목 유지 |
| 검증 참조 작업/체크리스트 관리 API | 미구현 |
| 시스템 토큰 필터와 `/api/v1/system/**` 전역 보안 | 미수정 |
| `inputHash` 컬럼/unique key 확장 | 미수정 |
| 실제 DeepSeek 호출과 batch worker 흐름 | 미수정 |
| 감사 로그 신규 기록 | 조회 API이므로 미구현 |

## 후속 작업

1. Lead가 관리자 AI 산출물 조회에서 `OPERATOR` 조회 허용 여부를 확정
2. 승인/반려/숨김 API 구현
3. 평가 후보 등록 API 구현
4. `validation_reference_jobs`, `ai_validation_checklist_versions` 관리 API 구현
5. 감사 로그 UseCase 계약 확정 후 관리자 AI 변경 작업과 연결
