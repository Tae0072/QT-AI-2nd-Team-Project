# Report - 2026-05-26 ai-prompt-version-id-mapping

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-prompt-version-id-mapping` |
| PR 대상 | `dev` |
| 실행 경로 | 직접 실행 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-26_ai-prompt-version-id-mapping.md` |
| 관련 F-ID | F-02, F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/reports/2026-05-26_ai-prompt-version-id-mapping_report.md` |

## 작업 결과

시스템 AI 생성 job API와 AI 생성 job 저장 구조를 API 명세/ERD 기준의 `promptVersionId` 중심으로 정합화했다. 기존 `promptVersion` 문자열 입력과 `ai_generation_jobs.prompt_version` 저장 기준을 제거하고, `ai_prompt_versions`를 조회한 뒤 `ai_generation_jobs.prompt_version_id`로 추적하도록 바꿨다.

AI 산출물은 별도 프롬프트 버전 스냅샷을 저장하지 않고 `generation_job_id -> ai_generation_jobs.prompt_version_id -> ai_prompt_versions.id` 경로로만 프롬프트 버전을 추적하도록 정리했다. `SUMMARY`, `GLOSSARY`는 문서의 `ai_prompt_versions.prompt_type`에 없으므로 이번 PR에서는 독립 job type으로 지원하지 않고, 시스템 generation job API는 기존처럼 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 허용한다.

## 변경 요약

1. `CreateAiGenerationJobCommand`와 `SystemAiGenerationJobRequest`를 `promptVersionId` 기준으로 변경했다.
2. `AiPromptVersion`, `AiPromptType`, `AiPromptVersionStatus`, `AiPromptVersionRepository`를 추가했다.
3. `AiService`에서 prompt version 존재 여부, `ACTIVE` 상태, job type과 prompt type 정합성을 검증하도록 했다.
4. `AiGenerationJob` 저장 기준과 중복 차단 기준을 `prompt_version_id`로 변경했다.
5. `AiGeneratedAsset`, `RegisterAiGeneratedAssetCommand`, `AiLogService.registerGeneratedAsset`에서 별도 `promptVersion` 스냅샷을 제거했다.
6. 컨트롤러/서비스/엔티티/계약 테스트를 `promptVersionId` 기준으로 수정하고, retired/missing/type mismatch/SUMMARY/GLOSSARY 차단 케이스를 보강했다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/CreateAiGenerationJobCommand.java` | `promptVersion` 제거, `promptVersionId` 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/api/dto/RegisterAiGeneratedAssetCommand.java` | 산출물 등록 command에서 `promptVersion` 제거 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiPromptType.java` | `EXPLANATION`, `SIMULATOR`, `QA` prompt type enum 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiPromptVersionStatus.java` | `ACTIVE`, `RETIRED` 상태 enum 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiPromptVersion.java` | `ai_prompt_versions` Entity 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiPromptVersionRepository.java` | prompt version 조회 Repository 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJob.java` | `prompt_version` 제거, `prompt_version_id` 저장 기준 반영 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRepository.java` | 진행 중 job 중복 조회를 `promptVersionId` 기준으로 변경 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGeneratedAsset.java` | 별도 `promptVersion` 스냅샷 제거 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogService.java` | job queue와 asset 등록 helper 계약 정리 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | prompt version 조회/상태/type 검증 및 생성 job 저장 기준 정합화 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobController.java` | HTTP 요청의 `promptVersionId`를 UseCase command로 전달 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiGenerationJobRequest.java` | 요청 DTO를 `promptVersionId` 기준으로 변경 |
| `qtai-server/src/test/java/com/qtai/domain/ai/**` | controller/service/entity/API 계약 테스트 갱신 및 보강 |
| `doc/workspaces/DevC_강상민/workflows/2026-05-26_ai-prompt-version-id-mapping.md` | 실행 기준 workflow 작성 및 보정 |

## 수용 기준 평가

| 수용 기준 | 상태 | 근거 |
| --- | --- | --- |
| 시스템 AI 생성 job API 요청 body가 `promptVersionId`, `jobType`, `targetType`, `targetId`를 사용한다 | 충족 | `SystemAiGenerationJobRequest`, 컨트롤러 테스트 반영 |
| `promptVersion` 문자열 요청 필드는 정상 계약이 아니다 | 충족 | DTO에서 제거, 누락/invalid 필드 테스트 갱신 |
| `ai_prompt_versions` Entity/Repository가 AI 도메인 내부에 추가된다 | 충족 | `AiPromptVersion`, `AiPromptVersionRepository` 추가 |
| 없는 `promptVersionId`는 `400 INVALID_INPUT` 성격의 도메인 예외로 차단된다 | 충족 | `AiServiceTest.missingPromptVersionIsBlocked` |
| `RETIRED` prompt version은 차단된다 | 충족 | `AiServiceTest.retiredPromptVersionIsBlocked` |
| prompt type과 job type이 맞지 않으면 차단된다 | 충족 | `AiServiceTest.promptTypeMismatchIsBlocked` |
| 시스템 generation job API는 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 허용한다 | 충족 | 컨트롤러 매핑/unsupported jobType 테스트 유지 |
| `SUMMARY`, `GLOSSARY`는 독립 job type으로 지원하지 않는다 | 충족 | 컨트롤러와 서비스 테스트에 차단 케이스 반영 |
| `ai_generation_jobs` Entity는 `prompt_version_id`를 저장한다 | 충족 | `AiGenerationJob` 컬럼/unique constraint 변경 |
| 진행 중 job 중복 차단은 `promptVersionId` 기준으로 동작한다 | 충족 | Repository 메서드와 `AiServiceTest` 반영 |
| 관리자 재생성 job 생성도 `promptVersionId` 기준으로 동작한다 | 충족 | `RegenerateAiAssetCommand.promptVersionId` 사용 테스트 반영 |
| `ai_generated_assets` Entity와 산출물 등록 계약에는 별도 `promptVersion` 스냅샷이 없다 | 충족 | `AiGeneratedAsset`, `RegisterAiGeneratedAssetCommand`, `AiLogService`에서 제거 |
| AI 산출물 프롬프트 버전은 `generation_job_id -> ai_generation_jobs.prompt_version_id`로 추적한다 | 충족 | 산출물 Entity는 `generationJobId`만 보유 |
| 기존 `SYSTEM_BATCH`/`ROLE_SYSTEM_BATCH` 컨트롤러 authority 방어선은 유지된다 | 충족 | `SystemAiGenerationJobControllerTest` 유지 |
| 사용자 AI 경로(`/api/v1/ai/**`)에서 사전 생성 job을 만들 수 있는 새 경로가 생기지 않는다 | 충족 | 변경은 system/admin AI 경로와 internal/api DTO에 한정 |
| 프롬프트 원문, provider raw response, secret, token이 테스트 데이터나 로그에 포함되지 않는다 | 충족 | 저장 차단 테스트만 유지, 실제 저장 데이터에는 포함하지 않음 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "*AiServiceTest"` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "*SystemAiGenerationJobControllerTest"` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "*AiGenerationJobTest" --tests "*AiGeneratedAssetTest" --tests "*AiLogServiceTest" --tests "*AiUseCaseContractTest"` | 성공 |
| `.\gradlew.bat test --tests "*Ai*"` in `qtai-server` | 성공 |
| `.\gradlew.bat build` in `qtai-server` | 성공 |
| `rg -n "promptVersion\(\)" qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |
| `rg -n "promptVersion\b" qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |
| `rg -n 'prompt_version"' qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |
| `rg -n "^import .*domain\.[a-z]+\.(internal\|web)" qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |
| `git diff --check` | 성공 |

`.\qtai-server\gradlew.bat -p qtai-server test --tests "*Ai*"`는 Windows/Gradle 인자 해석 문제로 `-p qtai-server`가 테스트 필터에 섞여 실패했다. 같은 검증은 `qtai-server` 디렉터리에서 `.\gradlew.bat test --tests "*Ai*"`로 재실행해 성공 확인했다.

## 실행하지 않은 검증

| 명령 | 사유 |
| --- | --- |
| `npx @stoplight/spectral-cli lint ...` | OpenAPI YAML을 수정하지 않았고, 이번 변경은 Java 코드/테스트와 workflow/report 문서에 한정됨 |
| `gitleaks detect --source . --redact --exit-code 1` | 현재 환경에서 `gitleaks` 실행 파일을 찾지 못함 |
| `jacocoTestReport`, `jacocoTestCoverageVerification` | 이번 workflow 검증 계획에 직접 포함되지 않았고, 현재 변경 범위 검증은 대상 테스트와 `build`로 수행 |

## 후속 작업

1. `service_accounts` 기반 service account token 검증 필터와 `/api/v1/system/**` 전역 보안 설정
2. `POST /api/v1/system/ai/assets` 산출물 등록 API 구현
3. `POST /api/v1/system/ai/validation-logs` 검증 로그 등록 API 구현
4. 실제 DeepSeek 호출과 batch worker 실행 흐름 구현
5. `SUMMARY`, `GLOSSARY` job type과 `ai_prompt_versions.prompt_type` 정책 정합화 Lead 결정
6. 감사 로그의 `SYSTEM_BATCH` actor와 service account 연결 정책 구현
