# Workflow — 2026-05-22 ai-system-generation-job-trigger-api

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-system-generation-job-trigger` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | W2 "실제로 관리자/배치 경로에서만 AI 생성 Job을 만들 수 있게 한다" 작업 중 시스템 배치 AI 생성 Job 트리거 API 구현 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**` |

## 기준 문서

- `doc/프로젝트 문서/07_요구사항_정의서.md`
- `doc/프로젝트 문서/03_아키텍처_정의서.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/05_시퀀스_다이어그램.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `doc/프로젝트 문서/23_도메인_용어사전.md`
- `doc/프로젝트 문서/25_기능_명세서.md`
- `CODE_CONVENTION.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-20_ai-usecase-contracts.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-20_ai-generation-log-model.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-21_ai-failure-retry-policy.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-21_create-ai-generation-job-usecase-implementation.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-21_admin-ai-generation-trigger-api.md`

## 작업 목표

시스템 배치가 AI 사전 생성 작업을 HTTP로 접수할 수 있는 `POST /api/v1/system/ai/generation-jobs` API를 구현한다. 이 API는 실제 DeepSeek 호출이나 산출물 생성을 수행하지 않고, 기존 `CreateAiGenerationJobUseCase`를 호출해 `ai_generation_jobs`에 `QUEUED` 상태의 작업을 등록하는 트리거 역할만 담당한다.

이번 작업의 핵심은 사용자 앱 경로에서 해설·시뮬레이터 생성이 시작되지 않도록 유지하면서, 시스템 배치 경로에서만 F-02 대상 생성 job을 만들 수 있게 하는 것이다. F-02 범위의 HTTP `jobType`은 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 허용하고, 내부 UseCase에는 각각 `EXPLANATION`, `SIMULATOR`로 매핑한다.

## 범위

- `POST /api/v1/system/ai/generation-jobs` 시스템 API를 추가한다.
- 요청 DTO는 실제 구현 가능성을 우선해 `jobType`, `targetType`, `targetId`, `promptVersion`을 받는다.
- `jobType`은 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 허용한다.
- `DAILY_QT_EXPLANATION`은 내부 `CreateAiGenerationJobCommand.jobType="EXPLANATION"`으로 매핑한다.
- `DAILY_QT_SIMULATOR`는 내부 `CreateAiGenerationJobCommand.jobType="SIMULATOR"`로 매핑한다.
- `targetType`은 이번 작업에서 `QT_PASSAGE`만 허용한다.
- `targetId`는 양수만 허용한다.
- `promptVersion`은 blank가 아닌 문자열만 허용한다.
- `requestedBy`는 요청 바디나 헤더를 믿지 않고 컨트롤러에서 `"SYSTEM_BATCH"`로 고정한다.
- `requestedAt`과 응답의 `createdAt`은 서버 시간으로 생성한다.
- 성공 응답은 `202 Accepted`와 `generationJobId`, `status`, `createdAt`을 반환한다.
- 인증은 이번 작업에서 최소 검증으로 구현한다. `Authentication` authority에 `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH`가 있는 요청만 허용한다.
- 인증 정보가 없거나 anonymous면 `401`, authority가 부족하면 `403`으로 응답한다.
- `CreateAiGenerationJobUseCase`가 던지는 `INVALID_STATUS_TRANSITION`은 진행 중 job 중복 차단으로 보고 `409`로 응답한다.
- `domain.ai` 내부 구현은 다른 도메인의 `internal`, `web`, `repository` 타입을 직접 import하지 않는다.

## 제외 범위

- `SecurityConfig` 또는 전역 `/api/v1/system/**` 보안 설정 구현은 제외한다.
- `service_accounts` 기반 서버 간 인증, service account token 검증 필터, 토큰 발급·회전 정책은 제외한다.
- `ai_prompt_versions` 조회와 `promptVersionId -> promptVersion` 매핑은 제외한다.
- API 명세의 `promptVersionId` 요청 필드 반영은 제외하고, 이번 구현은 `promptVersion` 문자열을 받는다.
- `SUMMARY`, `GLOSSARY` job 생성은 제외한다. Java enum에는 존재하지만 F-02 시스템 트리거와 ERD/API 정합화 대상이 아니므로 asset type 정합화 후속 작업으로 남긴다.
- `targetType= BIBLE_VERSE`, `QA_REQUEST` 등 `QT_PASSAGE` 외 대상 생성은 제외한다.
- 실제 DeepSeek 호출, 배치 실행기, 산출물 payload 생성은 제외한다.
- `POST /api/v1/system/ai/assets`, `POST /api/v1/system/ai/validation-logs` 구현은 제외한다.
- AI 산출물 승인, 반려, 숨김, 관리자 로그 조회 API는 제외한다.
- 감사 로그 저장 구현은 제외한다.
- prompt 원문, provider raw response, 검증 참조 원문, secret, token은 요청·응답·로그에 포함하지 않는다.

## 정책 결정

| 구분 | 기준 |
| --- | --- |
| API | `POST /api/v1/system/ai/generation-jobs` |
| 인증 | 최소 구현 기준으로 `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH` authority 허용 |
| 실제 인증 후속 | `service_accounts` 기반 서버 간 인증과 전역 system path 보안 설정 |
| 요청 | `{ "jobType": "DAILY_QT_EXPLANATION", "targetType": "QT_PASSAGE", "targetId": 35, "promptVersion": "2026.05.1" }` |
| 성공 응답 | `202 Accepted`, `{ "generationJobId": 101, "status": "QUEUED", "createdAt": "..." }` |
| 허용 jobType | `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR` |
| 내부 jobType 매핑 | `DAILY_QT_EXPLANATION -> EXPLANATION`, `DAILY_QT_SIMULATOR -> SIMULATOR` |
| 허용 targetType | `QT_PASSAGE` |
| requestedBy | 서버에서 `"SYSTEM_BATCH"`로 고정 |
| 중복 차단 | 기존 `CreateAiGenerationJobUseCase`의 `QUEUED`/`RUNNING` 중복 차단 정책 사용 |
| promptVersion gap | API 명세는 `promptVersionId`지만 현재 UseCase는 `promptVersion` 문자열을 사용한다. 이번 작업은 문자열 기준으로 구현하고 매핑은 후속 분리 |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobController.java` | `/api/v1/system/ai/generation-jobs` 요청 인증, DTO 검증, HTTP jobType 매핑, UseCase 호출, 202 응답 처리 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobRequest.java` | 시스템 job 생성 요청 DTO. `jobType`, `targetType`, `targetId`, `promptVersion` 검증 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobResponse.java` | 시스템 job 접수 응답 DTO. `generationJobId`, `status`, `createdAt` 반환 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | 인증, 매핑, 202 응답, 400/401/403/409 오류 응답 검증 |
| Reuse | `qtai-server/src/main/java/com/qtai/domain/ai/api/CreateAiGenerationJobUseCase.java` | 실제 job 생성 UseCase 계약 재사용 |
| Reuse | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | `CreateAiGenerationJobUseCase` 구현체 재사용. 이번 작업에서 변경하지 않는 것을 우선한다 |

## 구현 순서

1. `CreateAiGenerationJobUseCase`, `CreateAiGenerationJobCommand`, `CreateAiGenerationJobResult`의 현재 필드와 예외 정책을 확인한다.
2. `AdminAiAssetController`의 `Authentication` 처리, `ApiResponse` 응답, `BusinessException` 매핑 방식을 확인한다.
3. `SystemAiGenerationJobRequest`를 추가한다. 필드는 `jobType`, `targetType`, `targetId`, `promptVersion`으로 제한한다.
4. `SystemAiGenerationJobResponse`를 추가한다. 필드는 `generationJobId`, `status`, `createdAt`으로 제한한다.
5. `SystemAiGenerationJobControllerTest`에 성공 케이스를 먼저 작성한다. `ROLE_SYSTEM_BATCH` principal로 `DAILY_QT_EXPLANATION` 요청을 보내면 UseCase에는 `EXPLANATION`, `QT_PASSAGE`, `targetId`, `promptVersion`, `SYSTEM_BATCH`, fixed clock 시간이 전달되어야 한다.
6. 테스트를 실행해 컨트롤러가 없어서 실패하는 것을 확인한다.
7. `SystemAiGenerationJobController`를 추가하고 `/api/v1/system/ai/generation-jobs`를 노출한다.
8. 컨트롤러에 테스트용 `Clock` 주입 생성자를 둔다. 운영 생성자는 `Clock.systemDefaultZone()`을 사용한다.
9. `Authentication` 검증을 구현한다. 인증 없음 또는 anonymous는 `UNAUTHORIZED`, authority 부족은 `FORBIDDEN`으로 처리한다.
10. HTTP `jobType` 매핑을 구현한다. `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR` 외 값은 `INVALID_INPUT`으로 차단한다.
11. `targetType` 검증을 구현한다. `QT_PASSAGE` 외 값은 `INVALID_INPUT`으로 차단한다.
12. `CreateAiGenerationJobCommand`를 만들 때 `requestedBy`는 `"SYSTEM_BATCH"`, `requestedAt`은 fixed clock 기반 현재 시간으로 채운다.
13. UseCase 결과와 컨트롤러에서 생성한 `requestedAt`을 사용해 `SystemAiGenerationJobResponse`를 만들고 `202 Accepted`로 반환한다.
14. 오류 매핑을 구현한다. `UNAUTHORIZED -> 401`, `FORBIDDEN -> 403`, `INVALID_INPUT -> 400`, `INVALID_STATUS_TRANSITION -> 409`, 그 외는 `500`으로 처리한다.
15. `DAILY_QT_SIMULATOR` 매핑 테스트를 추가하고 통과시킨다.
16. `SUMMARY`, `GLOSSARY`, 알 수 없는 `jobType` 차단 테스트를 추가하고 통과시킨다.
17. `targetType`이 `QT_PASSAGE`가 아닌 요청 차단 테스트를 추가하고 통과시킨다.
18. 인증 없음, `ROLE_USER`, `SYSTEM_BATCH`, `ROLE_SYSTEM_BATCH` 케이스를 테스트로 고정한다.
19. UseCase가 `INVALID_STATUS_TRANSITION`을 던질 때 `409`로 응답하는 테스트를 추가한다.
20. AI 도메인 금지 import와 사용자 앱 경로에서 생성 API가 추가되지 않았는지 확인한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | `ROLE_SYSTEM_BATCH` 요청은 `202 Accepted`와 `generationJobId`, `status`, `createdAt`을 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | `DAILY_QT_EXPLANATION`은 UseCase command의 `jobType="EXPLANATION"`으로 매핑된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | `DAILY_QT_SIMULATOR`는 UseCase command의 `jobType="SIMULATOR"`로 매핑된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | UseCase command의 `requestedBy`는 요청 값이 아니라 `"SYSTEM_BATCH"`로 고정된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | UseCase command의 `requestedAt`과 응답 `createdAt`은 fixed clock 시간과 같다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | `SUMMARY`, `GLOSSARY`, 알 수 없는 `jobType`은 `400`으로 차단하고 UseCase를 호출하지 않는다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | `targetType`이 `QT_PASSAGE`가 아니면 `400`으로 차단하고 UseCase를 호출하지 않는다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | 인증 없음은 `401`로 응답하고 UseCase를 호출하지 않는다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | `ROLE_USER` 등 권한 부족은 `403`으로 응답하고 UseCase를 호출하지 않는다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | `SYSTEM_BATCH`와 `ROLE_SYSTEM_BATCH` authority는 모두 허용한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | UseCase가 `INVALID_STATUS_TRANSITION`을 던지면 `409`로 응답한다 |

## 수용 기준

- [ ] 시스템 배치 API가 `POST /api/v1/system/ai/generation-jobs` 경로로 노출된다.
- [ ] 성공 응답은 `202 Accepted`이며 `generationJobId`, `status`, `createdAt`만 반환한다.
- [ ] `DAILY_QT_EXPLANATION`은 내부 `EXPLANATION` job으로 생성된다.
- [ ] `DAILY_QT_SIMULATOR`는 내부 `SIMULATOR` job으로 생성된다.
- [ ] `SUMMARY`, `GLOSSARY`, 알 수 없는 `jobType`은 이번 F-02 범위에서 차단된다.
- [ ] `targetType`은 `QT_PASSAGE`만 허용된다.
- [ ] 인증 없음은 `401`, `SYSTEM_BATCH` 권한 부족은 `403`으로 응답한다.
- [ ] `requestedBy`는 서버에서 `"SYSTEM_BATCH"`로 고정된다.
- [ ] 진행 중인 동일 job이 있으면 기존 UseCase 정책에 따라 중복 생성하지 않고 `409`로 응답한다.
- [ ] 사용자 앱 경로 또는 `/api/v1/ai/**`에서 해설·시뮬레이터 생성이 시작되지 않는다.
- [ ] prompt 원문, provider raw response, 검증 참조 원문, secret, token은 요청·응답·로그에 남지 않는다.
- [ ] `promptVersionId`와 `promptVersion` 불일치는 workflow의 gap과 후속 작업으로 명확히 남는다.

## Known Gap

| 항목 | 현재 처리 | 후속 기준 |
| --- | --- | --- |
| `promptVersionId` | 이번 API는 `promptVersion` 문자열을 직접 받는다 | `ai_prompt_versions` 조회 구조가 확정되면 `promptVersionId -> promptVersion` 매핑을 별도 작업으로 구현 |
| `service_accounts` 인증 | 이번 API는 `Authentication` authority의 `SYSTEM_BATCH`/`ROLE_SYSTEM_BATCH`만 확인한다 | service account token 검증 필터와 전역 `/api/v1/system/**` 보안 설정 구현 |
| `inputHash` | 현재 `CreateAiGenerationJobUseCase`와 Entity에 물리 필드가 없다 | 중복 방지 키를 `targetType + targetId + assetType + promptVersion + inputHash`로 확장할지 Lead 검토 후 결정 |
| `SUMMARY`, `GLOSSARY` | 이번 F-02 시스템 트리거에서 제외한다 | asset type과 ERD/API job type 정합화 후 생성 대상 여부 결정 |

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 시스템 컨트롤러, DTO, 컨트롤러 테스트에 집중되어 병렬화 이점이 작다.
- 인증 처리, HTTP jobType 매핑, UseCase command 생성이 같은 정책 결정을 공유하므로 한 흐름에서 직접 구현하는 편이 안전하다.
- 실제 배치 실행기, 산출물 등록, 검증 로그 등록, 전역 보안 설정은 제외되어 독립 위임할 큰 하위 작업이 없다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 Controller 테스트 작성, Controller/DTO 구현, 경계 검증, 최종 테스트 실행을 직접 수행한다.

## 검증 계획

- `./gradlew -p qtai-server test --tests "*SystemAiGenerationJobControllerTest"`
- `./gradlew -p qtai-server test --tests "*AiServiceTest"`
- `./gradlew -p qtai-server test --tests "*AiUseCaseContractTest"`
- `./gradlew -p qtai-server test --tests "*Ai*"`
- `./gradlew -p qtai-server build`
- `rg -n "^import .*domain\\.[a-z]+\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/ai`로 금지 import 여부 확인
- `rg -n "/api/v1/ai/.+generation|generation-jobs" qtai-server/src/main/java/com/qtai/domain/ai/web`로 사용자 AI 경로에 생성 트리거가 추가되지 않았는지 확인
- OpenAPI 파일을 수정하지 않으면 Spectral lint는 생략한다. OpenAPI를 수정한 경우 `npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml`을 실행한다.
- 환경에 `gitleaks`가 있으면 `gitleaks detect --source . --redact --exit-code 1`을 실행한다. 실행 파일이 없으면 최종 리포트에 생략 사유를 적는다.

## 후속 작업으로 남길 항목

- `service_accounts` 기반 서버 간 인증과 전역 `/api/v1/system/**` 보안 설정 구현
- API 명세의 `promptVersionId`를 기준으로 `ai_prompt_versions` 조회와 `promptVersionId -> promptVersion` 매핑 구현
- `POST /api/v1/system/ai/assets` 산출물 등록 API 구현
- `POST /api/v1/system/ai/validation-logs` 검증 로그 등록 API 구현
- 실제 DeepSeek 호출과 batch worker 실행 흐름 구현
- `inputHash` 저장 위치와 중복 방지 키 확장 정책 확정
- `SUMMARY`, `GLOSSARY` job type과 asset type 정합화
- 시스템 배치 트리거와 감사 로그 연결 정책 확정
