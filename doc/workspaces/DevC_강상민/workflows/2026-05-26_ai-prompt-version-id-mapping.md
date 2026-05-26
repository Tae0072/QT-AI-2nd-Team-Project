# Workflow - 2026-05-26 ai-prompt-version-id-mapping

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-prompt-version-id-mapping` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | 시스템 AI 생성 job API가 API 명세의 `promptVersionId`와 다르게 현재 `promptVersion` 문자열을 받고 저장하는 gap 해소 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `02_ERD_문서.md`, `18_코드_품질_게이트.md`, `23_도메인_용어사전.md`, `25_기능_명세서.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**` |

## 작업 목표

`POST /api/v1/system/ai/generation-jobs` 요청 계약과 AI 생성 job 저장 구조를 문서 기준으로 정합화한다. HTTP 요청은 `promptVersion` 문자열이 아니라 `promptVersionId`를 받고, AI 도메인 내부에서 `ai_prompt_versions`를 조회해 활성 프롬프트 버전인지 확인한 뒤 `ai_generation_jobs.prompt_version_id`로 저장한다.

이번 작업은 기존 후속 항목이었던 `promptVersionId -> promptVersion` 임시 매핑을 넘어서, ERD 기준 FK 추적 구조까지 반영한다. `promptVersion` 문자열은 `ai_prompt_versions.version`의 속성으로만 남기고, 생성 job의 중복 차단과 저장 기준은 `promptVersionId`로 바꾼다. `ai_generated_assets`는 별도 프롬프트 버전 스냅샷을 저장하지 않고, `generation_job_id -> ai_generation_jobs.prompt_version_id -> ai_prompt_versions.id` 경로로만 프롬프트 버전을 추적한다.

## 범위

- `SystemAiGenerationJobRequest` 요청 필드를 `promptVersionId`로 변경한다.
- `CreateAiGenerationJobCommand`를 `promptVersionId` 기준으로 변경한다.
- `AiPromptVersion` 내부 Entity와 조회 Repository를 추가한다.
- `ai_prompt_versions.status=ACTIVE`인 프롬프트 버전만 job 생성에 사용할 수 있게 검증한다.
- `ai_prompt_versions.prompt_type`이 요청 job type과 맞는지 검증한다. 예: `DAILY_QT_EXPLANATION -> EXPLANATION`, `DAILY_QT_SIMULATOR -> SIMULATOR`.
- 시스템 generation job API는 기존처럼 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 허용한다.
- `SUMMARY`, `GLOSSARY`는 문서의 `ai_prompt_versions.prompt_type`에 없으므로 이번 promptVersionId 정합화 범위에서 독립 job type으로 지원하지 않는다.
- `AiGenerationJob`의 물리 컬럼을 `prompt_version` 문자열에서 `prompt_version_id`로 변경한다.
- 진행 중 job 중복 차단 기준을 `jobType + targetType + targetId + promptVersionId + activeUniqueKey`로 변경한다.
- `AiService.createAiGenerationJob`와 관리자 재생성 경로의 job 생성이 `promptVersionId`를 사용하도록 정합화한다.
- `RegenerateAiAssetCommand.promptVersionId`는 기존 요청 계약을 유지하되, job 생성 시 실제로 사용한다.
- 없는 `promptVersionId`, retired `promptVersionId`, job type과 prompt type이 맞지 않는 `promptVersionId`는 `INVALID_INPUT`으로 차단한다.
- `AiGeneratedAsset`의 `prompt_version` 문자열 컬럼과 getter를 제거한다.
- `RegisterAiGeneratedAssetCommand`와 `AiLogService.registerGeneratedAsset`에서 `promptVersion` 입력을 제거한다.
- AI 산출물의 프롬프트 버전 추적은 `generationJobId`로 연결된 `AiGenerationJob.promptVersionId`를 기준으로 한다.
- 기존 컨트롤러 단위 `SYSTEM_BATCH` authority 검증은 유지한다.

## 제외 범위

- `service_accounts` 기반 서버 간 인증 필터 구현은 제외한다.
- `/api/v1/system/**` 전역 Spring Security 게이트 구현은 제외한다.
- AI 실제 생성 worker, DeepSeek 호출, 산출물 payload 생성은 제외한다.
- `POST /api/v1/system/ai/assets`, `POST /api/v1/system/ai/validation-logs` 신규 구현은 제외한다.
- `SUMMARY`, `GLOSSARY` 독립 job type 지원은 제외한다. 문서의 `ai_prompt_versions.prompt_type`은 `EXPLANATION`, `SIMULATOR`, `QA` 기준이므로 이번 PR에서는 정책을 확장하지 않는다.
- 운영용 seed 데이터나 실제 프롬프트 원문 저장은 제외한다.
- 프롬프트 원문, provider raw response, secret, token, 민감 정보는 테스트 fixture와 로그에 포함하지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiPromptVersion.java` | `ai_prompt_versions` Entity. `id`, `promptType`, `version`, `contentHash`, `status`, `createdAt` 보유 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiPromptVersionRepository.java` | `promptVersionId` 조회와 활성 버전 검증에 필요한 JPA Repository |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiPromptVersionStatus.java` | `ACTIVE`, `RETIRED` 상태 enum |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiPromptType.java` | ERD 기준 `EXPLANATION`, `SIMULATOR`, `QA` prompt type enum |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobRequest.java` | `promptVersion` 제거, `@NotNull @Positive Long promptVersionId` 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobController.java` | HTTP 요청의 `promptVersionId`를 `CreateAiGenerationJobCommand`에 전달 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/CreateAiGenerationJobCommand.java` | `promptVersion` 제거, `promptVersionId` 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJob.java` | `prompt_version` 컬럼 제거, `prompt_version_id` 컬럼과 getter 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRepository.java` | 중복 조회 메서드를 `PromptVersionId` 기준으로 변경 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | prompt version 조회/상태/type 검증, job 생성/중복 차단 기준 정합화 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogService.java` | job queue와 산출물 등록 helper에서 `promptVersionId`/`generationJobId` 기준으로 계약 정리 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGeneratedAsset.java` | `prompt_version` 스냅샷 제거, `generation_job_id`만 프롬프트 버전 추적 경로로 유지 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/RegisterAiGeneratedAssetCommand.java` | `promptVersion` 필드 제거, 산출물 등록 계약을 `generationJobId` 중심으로 정리 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiGenerationJobControllerTest.java` | HTTP 요청 계약과 command 매핑 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` | prompt version 조회, ACTIVE/RETIRED/type mismatch, 중복 차단 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobTest.java` | `promptVersionId` 필수값과 상태 전이 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGeneratedAssetTest.java` | 산출물 Entity가 `promptVersion` 없이 `generationJobId`로 생성되는지 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogServiceTest.java` | 산출물 등록 helper가 `promptVersion` 없이 저장되는지 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | UseCase DTO 계약 변경 반영 |

## 구현 순서

1. `SystemAiGenerationJobControllerTest`에서 성공 요청 body를 `"promptVersionId": 3` 기준으로 먼저 바꾼다.
2. 컨트롤러 테스트의 command 검증을 `command.promptVersionId() == 3L`로 변경하고, `promptVersion` 검증은 제거한다.
3. `promptVersionId` 누락, 0 이하 값이 `400 INVALID_INPUT`으로 차단되는 테스트를 추가한다. `SUMMARY`, `GLOSSARY`, 알 수 없는 job type도 `400`으로 차단되고 prompt version 조회와 UseCase 호출이 일어나지 않는지 고정한다.
4. `CreateAiGenerationJobCommand`에서 `promptVersion`을 제거하고 `promptVersionId`를 추가한다.
5. `SystemAiGenerationJobRequest`와 `SystemAiGenerationJobController`를 `promptVersionId` 전달 방식으로 수정한다.
6. `AiPromptType`, `AiPromptVersionStatus`, `AiPromptVersion`, `AiPromptVersionRepository`를 추가한다.
7. `AiGenerationJob`의 생성자와 `queue(...)` factory를 `promptVersionId` 기준으로 변경한다.
8. `AiGenerationJob` 테이블 unique constraint column 목록을 `prompt_version_id` 기준으로 변경한다.
9. `AiGenerationJobRepository.existsBy...` 메서드를 `existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionIdAndStatusIn(...)`로 변경한다.
10. `AiService` 생성자에 `AiPromptVersionRepository`를 주입한다.
11. `AiService.createAiGenerationJob`에서 command 기본값 검증 후 `promptVersionId`로 `AiPromptVersion`을 조회한다.
12. 조회 결과가 없거나 `status != ACTIVE`이면 `BusinessException(ErrorCode.INVALID_INPUT, ...)`으로 차단한다.
13. 요청 job type과 prompt type이 맞지 않으면 `INVALID_INPUT`으로 차단한다.
14. 중복 job 조회와 `AiGenerationJob.queue(...)` 호출을 `promptVersionId` 기준으로 바꾼다.
15. `regenerateAiAsset`에서 `command.promptVersionId()`를 job 생성과 중복 조회에 사용한다.
16. 관리자 재생성 경로도 prompt version 조회, ACTIVE 상태, prompt type 정합성을 검증한다.
17. `AiServiceTest`의 mock repository와 captor 검증을 `promptVersionId` 기준으로 수정한다.
18. `AiServiceTest`에 없는 prompt version, retired prompt version, prompt type mismatch 케이스를 추가한다.
19. `AiGenerationJobTest`에서 blank `promptVersion` 검증을 제거하고 null/non-positive `promptVersionId` 검증으로 교체한다.
20. `AiGeneratedAsset`에서 `promptVersion` 필드, 생성자 인자, factory 인자, getter를 제거한다.
21. `RegisterAiGeneratedAssetCommand`에서 `promptVersion` 필드를 제거한다.
22. `AiLogService.registerGeneratedAsset`에서 `promptVersion` 인자를 제거하고 `AiGeneratedAsset.create(...)` 호출을 `generationJobId` 중심으로 맞춘다.
23. `AiLogService.queueGeneration`이 남아 있다면 `promptVersionId` 기준으로 변경한다. 실제 사용처가 없더라도 이번 PR에서는 계약 정합화까지만 수행하고 helper 삭제는 하지 않는다.
24. `AiGeneratedAssetTest`에서 blank `promptVersion` 차단 검증을 제거하고, `generationJobId` 필수 검증과 payload 보호 검증은 유지한다.
25. `AiLogServiceTest`와 산출물 등록 계약 테스트에서 `promptVersion` 인자와 기대값을 제거한다.
26. `AiUseCaseContractTest`에서 DTO 필드 변경을 반영한다.
27. 전체 AI 테스트를 실행하고, 남은 `promptVersion` 문자열 기반 job/asset 생성 코드가 없는지 `rg`로 확인한다.

## 테스트 보강 목록

| 테스트 파일 | 추가/수정 검증 |
| --- | --- |
| `SystemAiGenerationJobControllerTest` | `promptVersionId` 요청은 `CreateAiGenerationJobCommand.promptVersionId`로 전달된다 |
| `SystemAiGenerationJobControllerTest` | `promptVersion` 문자열 요청은 더 이상 정상 계약이 아니며, `promptVersionId` 누락 시 `400`이다 |
| `SystemAiGenerationJobControllerTest` | `promptVersionId <= 0`은 `400`이고 UseCase를 호출하지 않는다 |
| `SystemAiGenerationJobControllerTest` | `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR` 매핑과 `SYSTEM_BATCH` authority 검증은 유지된다 |
| `SystemAiGenerationJobControllerTest` | `SUMMARY`, `GLOSSARY`, 알 수 없는 job type은 독립 job type으로 지원하지 않으며 `400`으로 차단된다 |
| `AiServiceTest` | ACTIVE prompt version이면 queued job이 `promptVersionId`로 저장된다 |
| `AiServiceTest` | 없는 `promptVersionId`는 `INVALID_INPUT`이고 job을 저장하지 않는다 |
| `AiServiceTest` | RETIRED prompt version은 `INVALID_INPUT`이고 job을 저장하지 않는다 |
| `AiServiceTest` | job type과 prompt type이 다르면 `INVALID_INPUT`이고 job을 저장하지 않는다 |
| `AiServiceTest` | 진행 중 중복 job 조회는 `promptVersionId` 기준으로 수행된다 |
| `AiServiceTest` | 관리자 재생성 job도 `RegenerateAiAssetCommand.promptVersionId`를 사용한다 |
| `AiGenerationJobTest` | `promptVersionId`는 null 또는 0 이하일 수 없다 |
| `AiGeneratedAssetTest` | 산출물은 `promptVersion` 스냅샷 없이 `generationJobId`로 생성된다 |
| `AiGeneratedAssetTest` | `generationJobId` null은 차단되고 payload 원문 보호 검증은 유지된다 |
| `AiLogServiceTest` | `registerGeneratedAsset`는 `promptVersion` 없이 `generationJobId` 기준으로 저장한다 |
| `AiUseCaseContractTest` | `CreateAiGenerationJobCommand` 계약이 `promptVersionId` 기준이다 |

## 수용 기준

- [ ] 시스템 AI 생성 job API 요청 body는 `promptVersionId`, `jobType`, `targetType`, `targetId`를 사용한다.
- [ ] `promptVersion` 문자열 요청 필드는 더 이상 시스템 생성 job API의 정상 계약이 아니다.
- [ ] `ai_prompt_versions` Entity/Repository가 AI 도메인 내부에 추가된다.
- [ ] `promptVersionId`가 존재하지 않으면 `400 INVALID_INPUT`으로 차단된다.
- [ ] `promptVersionId`가 `RETIRED` 상태이면 `400 INVALID_INPUT`으로 차단된다.
- [ ] `promptVersionId`의 prompt type이 job type과 맞지 않으면 `400 INVALID_INPUT`으로 차단된다.
- [ ] 시스템 generation job API는 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 허용한다.
- [ ] `SUMMARY`, `GLOSSARY`는 이번 PR에서 독립 job type으로 지원하지 않고 `400 INVALID_INPUT`으로 차단된다.
- [ ] `ai_generation_jobs` Entity는 `prompt_version_id`를 저장한다.
- [ ] 진행 중 job 중복 차단은 `promptVersionId` 기준으로 동작한다.
- [ ] 관리자 재생성 job 생성도 `promptVersionId` 기준으로 동작한다.
- [ ] `ai_generated_assets` Entity와 산출물 등록 계약에는 별도 `promptVersion` 스냅샷이 없다.
- [ ] AI 산출물의 프롬프트 버전은 `generation_job_id -> ai_generation_jobs.prompt_version_id` 경로로 추적한다.
- [ ] 기존 `SYSTEM_BATCH`/`ROLE_SYSTEM_BATCH` 컨트롤러 authority 방어선은 유지된다.
- [ ] 사용자 AI 경로(`/api/v1/ai/**`)에서 사전 생성 job을 만들 수 있는 새 경로가 생기지 않는다.
- [ ] 프롬프트 원문, provider raw response, secret, token이 테스트 데이터나 로그에 포함되지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- UseCase DTO, Entity, Repository, Service, Controller, 산출물 Entity, 테스트가 같은 계약 변경에 강하게 연결되어 있다.
- `promptVersion` 문자열 제거와 `promptVersionId` 저장 기준 변경은 job과 asset 양쪽에서 순차적으로 컴파일 오류를 해소해야 한다.
- 관리자 재생성 경로가 같은 `AiService`와 `AiGenerationJob` 생성자를 공유하므로 병렬 편집 시 충돌 가능성이 높다.

### 위임 가능 작업

| Worker | 역할 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 테스트 계약 변경, Entity/Repository 추가, Service 정합화, Controller 반영, 최종 검증을 순서대로 직접 수행하는 것이 안전하다.

## 검증 계획

- `./gradlew -p qtai-server test --tests "*SystemAiGenerationJobControllerTest"`
- `./gradlew -p qtai-server test --tests "*AiServiceTest"`
- `./gradlew -p qtai-server test --tests "*AiGenerationJobTest"`
- `./gradlew -p qtai-server test --tests "*AiGeneratedAssetTest"`
- `./gradlew -p qtai-server test --tests "*AiLogServiceTest"`
- `./gradlew -p qtai-server test --tests "*AiUseCaseContractTest"`
- `./gradlew -p qtai-server test --tests "*Ai*"`
- `./gradlew -p qtai-server build`
- `rg -n "promptVersion\\(\\)|promptVersion\\b|prompt_version\"" qtai-server/src/main/java/com/qtai/domain/ai`로 job/asset 생성 경로의 문자열 기준 잔존 여부 확인
- `rg -n "^import .*domain\\.[a-z]+\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/ai`로 금지 import 여부 확인
- OpenAPI 파일을 수정한 경우 `npx @stoplight/spectral-cli lint qtai-server/apis/*/openapi.yaml --ruleset .spectral.yaml` 실행
- 환경에 `gitleaks`가 있으면 `gitleaks detect --source . --redact --exit-code 1` 실행

## 후속 작업으로 남길 항목

- `service_accounts` 기반 service account token 검증 필터와 `/api/v1/system/**` 전역 보안 설정
- `POST /api/v1/system/ai/assets` 산출물 등록 API 구현
- `POST /api/v1/system/ai/validation-logs` 검증 로그 등록 API 구현
- 실제 DeepSeek 호출과 batch worker 실행 흐름 구현
- `SUMMARY`, `GLOSSARY` job type과 `ai_prompt_versions.prompt_type` 정책 정합화 Lead 결정
- 감사 로그의 `SYSTEM_BATCH` actor와 service account 연결 정책 구현
