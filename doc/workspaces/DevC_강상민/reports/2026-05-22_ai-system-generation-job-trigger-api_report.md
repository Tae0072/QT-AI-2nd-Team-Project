# Report — 2026-05-22 ai-system-generation-job-trigger-api

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-system-generation-job-trigger` |
| PR 대상 | `dev` |
| 실행 경로 | 직접 실행 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-22_ai-system-generation-job-trigger-api.md` |
| 관련 F-ID | F-02, F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/reports/2026-05-22_ai-system-generation-job-trigger-api_report.md` |

## 경로 확인

기존 DevC 작업 리포트는 `doc/workspaces/DevC_강상민/reports` 아래에 관리되고 있다. 이번 리포트도 동일한 폴더 관례와 기존 `2026-05-21_admin-ai-generation-trigger-api_report.md` 형식을 따라 작성했다.

## 작업 결과

시스템 배치가 AI 사전 생성 작업을 HTTP로 접수할 수 있는 API를 구현했다. 대상 API는 `POST /api/v1/system/ai/generation-jobs`이며, 요청 성공 시 실제 DeepSeek 호출이나 산출물 생성을 수행하지 않고 기존 `CreateAiGenerationJobUseCase`를 호출해 `ai_generation_jobs`에 `QUEUED` 상태의 작업을 등록한다.

이번 작업은 시스템 배치 경로에서만 F-02 대상 생성 job을 만들 수 있게 하는 트리거 구현에 한정했다. 사용자 앱 경로 `/api/v1/ai/**` 생성 API, 전역 `/api/v1/system/**` 보안 설정, service account token 검증, `promptVersionId -> promptVersion` 매핑, 산출물 등록, 검증 로그 등록, 실제 batch worker 실행은 workflow의 제외 범위에 따라 구현하지 않았다.

## 변경 요약

1. `SystemAiGenerationJobController`를 추가해 `/api/v1/system/ai/generation-jobs`를 `202 Accepted`로 노출했다.
2. HTTP 요청 DTO `SystemAiGenerationJobRequest`를 추가했다.
3. HTTP 응답 DTO `SystemAiGenerationJobResponse`를 추가했다.
4. `Authentication` authority에 `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH`가 있는 요청만 허용했다.
5. 인증 정보가 없거나 anonymous면 `UNAUTHORIZED`, 권한이 부족하면 `FORBIDDEN`으로 차단했다.
6. HTTP `jobType` 중 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 허용했다.
7. 내부 UseCase command에는 각각 `EXPLANATION`, `SIMULATOR`로 매핑했다.
8. `targetType`은 `QT_PASSAGE`만 허용했다.
9. `requestedBy`는 요청 바디나 헤더를 사용하지 않고 서버에서 `"SYSTEM_BATCH"`로 고정했다.
10. UseCase가 `INVALID_STATUS_TRANSITION`을 던지면 진행 중 job 중복 차단으로 보고 `409 Conflict`로 응답했다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobController.java` | 시스템 AI 생성 job 접수 API, 인증 검증, jobType/targetType 매핑, 오류 응답 처리 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobRequest.java` | `jobType`, `targetType`, `targetId`, `promptVersion` 요청 DTO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobResponse.java` | `generationJobId`, `status`, `createdAt` 응답 DTO 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | 인증, 매핑, 202 응답, 400/401/403/409 오류 응답 검증 추가 |

## 정책 반영

| 정책 | 반영 결과 |
| --- | --- |
| API 경로 | `POST /api/v1/system/ai/generation-jobs`로 노출 |
| 인증 | `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH` authority만 허용 |
| 인증 없음 | `401 Unauthorized` 반환 |
| 권한 부족 | `403 Forbidden` 반환 |
| 요청 필드 | `jobType`, `targetType`, `targetId`, `promptVersion` 사용 |
| 성공 응답 | `generationJobId`, `status`, `createdAt` 반환 |
| 성공 상태 코드 | 비동기 접수 기준에 따라 `202 Accepted` 반환 |
| 허용 jobType | `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR` |
| 내부 jobType 매핑 | `DAILY_QT_EXPLANATION -> EXPLANATION`, `DAILY_QT_SIMULATOR -> SIMULATOR` |
| 허용 targetType | `QT_PASSAGE` |
| requestedBy | 서버에서 `"SYSTEM_BATCH"`로 고정 |
| 중복 차단 | 기존 `CreateAiGenerationJobUseCase`의 `QUEUED`/`RUNNING` 중복 차단 정책 사용 |
| 민감 데이터 | prompt 원문, provider raw response, 검증 참조 원문, secret, token을 요청/응답/로그에 포함하지 않음 |

## 테스트 보강

| 테스트 파일 | 검증 내용 |
| --- | --- |
| `SystemAiGenerationJobControllerTest` | `ROLE_SYSTEM_BATCH` 요청은 `202 Accepted`와 `generationJobId`, `status`, `createdAt`을 반환하는지 검증 |
| `SystemAiGenerationJobControllerTest` | `DAILY_QT_EXPLANATION`이 UseCase command의 `jobType="EXPLANATION"`으로 매핑되는지 검증 |
| `SystemAiGenerationJobControllerTest` | `DAILY_QT_SIMULATOR`가 UseCase command의 `jobType="SIMULATOR"`로 매핑되는지 검증 |
| `SystemAiGenerationJobControllerTest` | UseCase command의 `requestedBy`가 `"SYSTEM_BATCH"`로 고정되는지 검증 |
| `SystemAiGenerationJobControllerTest` | UseCase command의 `requestedAt`과 응답 `createdAt`이 fixed clock 시간과 같은지 검증 |
| `SystemAiGenerationJobControllerTest` | `SUMMARY`, `GLOSSARY`, 알 수 없는 `jobType`이 `400`으로 차단되고 UseCase를 호출하지 않는지 검증 |
| `SystemAiGenerationJobControllerTest` | `targetType`이 `QT_PASSAGE`가 아니면 `400`으로 차단되고 UseCase를 호출하지 않는지 검증 |
| `SystemAiGenerationJobControllerTest` | `targetId` 양수 조건과 `promptVersion` non-blank 조건을 검증 |
| `SystemAiGenerationJobControllerTest` | 인증 없음은 `401`, `ROLE_USER`는 `403`으로 응답하고 UseCase를 호출하지 않는지 검증 |
| `SystemAiGenerationJobControllerTest` | `SYSTEM_BATCH`와 `ROLE_SYSTEM_BATCH` authority를 모두 허용하는지 검증 |
| `SystemAiGenerationJobControllerTest` | UseCase가 `INVALID_STATUS_TRANSITION`을 던지면 `409`로 응답하는지 검증 |

## 수용 기준 점검

| 수용 기준 | 상태 | 근거 |
| --- | --- | --- |
| 시스템 배치 API가 `POST /api/v1/system/ai/generation-jobs` 경로로 노출된다 | 충족 | `SystemAiGenerationJobController`에 `@RequestMapping("/api/v1/system/ai/generation-jobs")` 추가 |
| 성공 응답은 `202 Accepted`이며 `generationJobId`, `status`, `createdAt`만 반환한다 | 충족 | `SystemAiGenerationJobResponse`와 컨트롤러 테스트로 확인 |
| `DAILY_QT_EXPLANATION`은 내부 `EXPLANATION` job으로 생성된다 | 충족 | command 매핑 테스트로 확인 |
| `DAILY_QT_SIMULATOR`는 내부 `SIMULATOR` job으로 생성된다 | 충족 | command 매핑 테스트로 확인 |
| `SUMMARY`, `GLOSSARY`, 알 수 없는 `jobType`은 이번 F-02 범위에서 차단된다 | 충족 | `unsupportedJobTypeReturnsBadRequest` 테스트로 확인 |
| `targetType`은 `QT_PASSAGE`만 허용된다 | 충족 | `nonQtPassageTargetTypeReturnsBadRequest` 테스트로 확인 |
| 인증 없음은 `401`, `SYSTEM_BATCH` 권한 부족은 `403`으로 응답한다 | 충족 | 인증/권한 테스트로 확인 |
| `requestedBy`는 서버에서 `"SYSTEM_BATCH"`로 고정된다 | 충족 | forged header를 보내도 command 값이 고정되는 테스트로 확인 |
| 진행 중인 동일 job이 있으면 기존 UseCase 정책에 따라 중복 생성하지 않고 `409`로 응답한다 | 충족 | UseCase의 `INVALID_STATUS_TRANSITION`을 `409`로 매핑하는 테스트로 확인 |
| 사용자 앱 경로 또는 `/api/v1/ai/**`에서 해설·시뮬레이터 생성이 시작되지 않는다 | 충족 | 새 컨트롤러는 `/api/v1/system/ai/**` 경로에만 추가됨 |
| prompt 원문, provider raw response, 검증 참조 원문, secret, token은 요청·응답·로그에 남지 않는다 | 충족 | 요청/응답 DTO에 원문성 필드와 민감 필드가 없음 |
| `promptVersionId`와 `promptVersion` 불일치는 workflow의 gap과 후속 작업으로 명확히 남는다 | 충족 | 이번 구현은 `promptVersion` 문자열만 사용하고 후속 작업으로 분리 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "*SystemAiGenerationJobControllerTest"` | 통과 |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "*AiServiceTest"` | 통과 |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "*AiUseCaseContractTest"` | 통과 |
| `.\gradlew.bat test --tests "*Ai*"` in `qtai-server` | 통과 |
| `.\gradlew.bat build` in `qtai-server` | 통과 |
| `.\qtai-server\gradlew.bat -p qtai-server build` | 통과 |
| `rg -n "^import .*domain\\.[a-z]+\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |
| `rg -n "/api/v1/ai/.+generation\|generation-jobs" qtai-server/src/main/java/com/qtai/domain/ai/web` | 새 시스템 경로만 확인됨 |
| `git diff --check` | 공백 오류 없음. 기존 문서 CRLF 변환 경고만 출력됨 |

초기 TDD 확인으로 `SystemAiGenerationJobController` 구현 전 `SystemAiGenerationJobControllerTest`가 컴파일 실패하는 것도 확인했다. 이후 컨트롤러와 DTO를 구현해 동일 테스트를 통과시켰다.

## 생략한 검증

| 명령 | 사유 |
| --- | --- |
| `npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml` | OpenAPI 파일을 수정하지 않아 workflow 기준에 따라 생략 |
| `gitleaks detect --source . --redact --exit-code 1` | 현재 환경에서 `gitleaks` 실행 파일이 발견되지 않음 |
| `jacocoTestReport`, `jacocoTestCoverageVerification` | 현재 `qtai-server` Gradle 프로젝트에 Jacoco 관련 task가 등록되어 있지 않음 |

## 남은 후속 작업

1. `service_accounts` 기반 서버 간 인증과 전역 `/api/v1/system/**` 보안 설정 구현
2. API 명세의 `promptVersionId`를 기준으로 `ai_prompt_versions` 조회와 `promptVersionId -> promptVersion` 매핑 구현
3. `POST /api/v1/system/ai/assets` 산출물 등록 API 구현
4. `POST /api/v1/system/ai/validation-logs` 검증 로그 등록 API 구현
5. 실제 DeepSeek 호출과 batch worker 실행 흐름 구현
6. `inputHash` 저장 위치와 중복 방지 키 확장 정책 확정
7. `SUMMARY`, `GLOSSARY` job type과 asset type 정합화
8. 시스템 배치 트리거와 감사 로그 연결 정책 확정
