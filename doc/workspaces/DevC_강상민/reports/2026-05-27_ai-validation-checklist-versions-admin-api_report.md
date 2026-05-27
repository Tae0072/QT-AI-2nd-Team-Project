# 2026-05-27 AI 검증 체크리스트 버전 관리자 API 구현 리포트

## 기준

- Workflow: `doc/workspaces/DevC_강상민/workflows/2026-05-27_ai-validation-checklist-versions-admin-api.md`
- 관련 F-ID: F-14
- 실행 경로: 직접 실행
- PR 대상: `dev`

## 구현 내용

- `GET /api/v1/admin/ai/validation-checklists` 목록 조회 API를 추가했다.
- `POST /api/v1/admin/ai/validation-checklists` 생성 API를 추가했다.
- `POST /api/v1/admin/ai/validation-checklists/{id}/activate` 활성화 API를 추가했다.
- `POST /api/v1/admin/ai/validation-checklists/{id}/retire` 폐기 API를 추가했다.
- 네 API 모두 `ROLE_ADMIN + ADMIN_ROLE_REVIEWER` 또는 `ROLE_ADMIN + ADMIN_ROLE_SUPER_ADMIN` 권한만 허용한다.
- `ai_validation_checklist_versions` entity, enum, repository, service, UseCase/DTO 계약을 추가했다.
- 생성 시 `status`는 생략 또는 `DRAFT`만 허용하고, `ACTIVE`, `RETIRED` 직접 생성은 `INVALID_INPUT`으로 차단한다.
- 활성화 시 같은 `checklistType`의 기존 `ACTIVE`는 자동 `RETIRED` 처리한다.
- 폐기는 `ACTIVE` 상태만 허용한다.
- `created_by_admin_id`에는 현재 principal id를 저장하지 않고 `null`을 허용한다.
- 서버는 checklist 원문을 요청으로 받거나 저장하지 않고, `contentHash` 기반 registry만 저장한다.
- `audit_logs` 최소 append-only 저장 구현과 `CHECKLIST_CREATE`, `CHECKLIST_ACTIVATE`, `CHECKLIST_RETIRE` 기록을 추가했다.
- 신규 에러 코드 `CHECKLIST_NOT_FOUND`, `DUPLICATE_CHECKLIST_VERSION`을 Java/OpenAPI/API 명세서에 반영했다.
- `qtai-server/apis/api-v1/openapi.yaml`에 API 4종의 path, request, response, error schema를 반영했다.
- `doc/프로젝트 문서/04_API_명세서.md` 7.2에 목록 페이징 응답, nullable `createdByAdminId`, hash registry 정책, 자동 retire 정책을 반영했다.

## 테스트 보강

- `AdminAiValidationChecklistControllerTest`
- `AdminAiValidationChecklistServiceTest`
- `AiValidationChecklistVersionTest`
- `AiValidationChecklistVersionRepositoryTest`
- `AuditServiceTest`
- `AiUseCaseContractTest`

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AdminAiValidationChecklistControllerTest"` | PASS |
| `.\gradlew.bat test --tests "*AdminAiValidationChecklistServiceTest"` | PASS |
| `.\gradlew.bat test --tests "*AiValidationChecklistVersionTest"` | PASS |
| `.\gradlew.bat test --tests "*AiValidationChecklistVersionRepositoryTest"` | PASS |
| `.\gradlew.bat test --tests "*AuditServiceTest"` | PASS |
| `.\gradlew.bat test --tests "*AiUseCaseContractTest"` | PASS |
| `.\gradlew.bat test --tests "*Ai*"` | PASS |
| `.\gradlew.bat build` | PASS |
| `npx.cmd @apidevtools/swagger-cli validate qtai-server/apis/api-v1/openapi.yaml` | PASS |
| `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | FAIL: 루트에 `.spectral.yaml` 없음 |
| `.\gradlew.bat test jacocoTestReport` | FAIL: `jacocoTestReport` 태스크 없음 |
| `.\gradlew.bat jacocoTestCoverageVerification` | FAIL: `jacocoTestCoverageVerification` 태스크 없음 |
| `gitleaks detect --source . --redact --exit-code 1` | FAIL: `gitleaks` 실행 파일 없음 |
| `rg -n "^import .*domain\\.[a-z]+\\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | PASS: 매칭 없음 |
| `rg -n "validation-checklists" qtai-server/src/main/java/com/qtai/domain/ai/web` | PASS: 관리자 경로만 추가됨 |
| `rg -n "raw response\|provider raw\|password\|private key\|secret\|example.*token" ...` | PASS: 매칭 없음 |

> 참고: Gradle 테스트를 병렬 실행했을 때 `build/test-results` 파일 충돌이 1회 발생했으나, 동일 테스트를 순차 재실행해 모두 통과했다.

## 제외 범위 준수

- `validation_reference_jobs` API는 구현하지 않았다.
- AI 평가 셋/평가 케이스 API는 구현하지 않았다.
- checklist 원문 저장소, 원문 저장 컬럼, 별도 item 테이블은 추가하지 않았다.
- `contentHash` 서버 측 canonical hash 재계산은 구현하지 않았다.
- 사용자 앱 경로(`/api/v1/ai/**`)에는 체크리스트 관리 API를 추가하지 않았다.
- DeepSeek 호출, AI 산출물 생성, 검증 실행 로직은 변경하지 않았다.
- prompt 원문, provider raw response, 검증 참조 원문 전체, secret/token/password 예시는 추가하지 않았다.

## 잔여 리스크와 후속 작업

- `.spectral.yaml`, Jacoco 태스크, gitleaks 실행 파일이 현재 로컬 환경에 없어 해당 품질 게이트는 CI 또는 도구 설정 후 재실행이 필요하다.
- `admin_users` 테이블과 관리자 권한 매핑 저장소가 구현되면 `created_by_admin_id` 저장 정책을 다시 연결해야 한다.
- 감사 로그 목록 API와 비동기 감사 로그 처리 인프라는 후속 작업으로 남긴다.
- 체크리스트 원문 조회가 필요해질 경우 요구사항/ERD/API 변경 검토가 필요하다.
