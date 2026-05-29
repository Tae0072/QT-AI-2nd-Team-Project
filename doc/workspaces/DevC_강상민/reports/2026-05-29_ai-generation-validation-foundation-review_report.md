# Report - 2026-05-29 ai-generation-validation-foundation-review

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 실행 경로 | 직접 실행 |
| 실제 작업 브랜치 | `feature/ai-generation-flow-check` |
| workflow 지정 브랜치 | `feature/ai-generation-validation-foundation-review` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-29_ai-generation-validation-foundation-review.md` |

## 작업 결과

기존 AI 생성/검증 기반 구현을 요구사항, 아키텍처, API 명세와 대조했다. 대부분의 job, asset, validation log 흐름은 문서 기준과 맞았고, 이번 작업에서는 `QA_RESPONSE`가 시스템 사전 생성 산출물 등록 경로로 들어갈 수 있는 부분만 최소 수정했다.

`POST /api/v1/system/ai/assets`와 `RegisterAiGeneratedAssetUseCase` 구현체에서 `QA_RESPONSE`를 `400 INVALID_INPUT`으로 차단한다. `QA_RESPONSE` enum 자체와 공용 조회/관리 모델은 유지해 F-15 Q&A 후속 구현 여지를 남겼다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/SystemAiAssetController.java` | system asset 등록 API에서 `QA_RESPONSE` 요청 차단 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiLogUseCaseService.java` | UseCase service 계층에서도 `QA_RESPONSE` 우회 등록 차단 |
| `qtai-server/apis/api-v1/openapi.yaml` | `SystemAiAssetRequest.assetType` enum에서 `QA_RESPONSE` 제외 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemAiAssetControllerTest.java` | `QA_RESPONSE` 요청 400 응답 테스트 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogUseCaseServiceTest.java` | service layer `QA_RESPONSE` 차단 테스트 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobTest.java` | `QUEUED -> RUNNING -> SUCCEEDED` 상태 전이 명시 테스트 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGeneratedAssetTest.java` | `VALIDATING -> APPROVED/REJECTED/HIDDEN` 상태 전이 명시 테스트 추가 |

## 수용 기준 상태

| 기준 | 상태 | 근거 |
| --- | --- | --- |
| job 상태는 `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`만 사용 | 충족 | enum 및 테스트 확인 |
| job 전이는 `QUEUED -> RUNNING -> SUCCEEDED \| FAILED`, `QUEUED -> FAILED` 기준 | 충족 | `AiGenerationJobTest` 보강 |
| terminal job은 `activeUniqueKey` 해제 | 충족 | 기존 구현/테스트 유지 |
| 진행 중 중복 job 차단 | 충족 | `AiServiceTest`, DB unique key, `INVALID_STATUS_TRANSITION` 매핑 |
| system job API는 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`, `QT_PASSAGE`만 허용 | 충족 | controller 및 테스트 확인 |
| `SUMMARY`, `GLOSSARY`는 독립 generation job으로 처리하지 않음 | 충족 | `AiServiceTest` 확인 |
| system asset API는 항상 `VALIDATING` 산출물 생성 | 충족 | controller/service 및 테스트 확인 |
| 요청자가 `APPROVED`, `REJECTED`, `HIDDEN` 직접 생성 불가 | 충족 | `SystemAiAssetControllerTest` 확인 |
| PR 2 범위에서 `QA_RESPONSE`는 system pre-generation asset 등록 대상 아님 | 충족 | controller/service/OpenAPI 수정 및 테스트 추가 |
| validation log는 `reviewerType=AUTO` 자동 검증 결과 저장 가능 | 충족 | 기존 controller/usecase 테스트 확인 |
| `PASSED`, `NEEDS_REVIEW`는 asset을 자동 승인하지 않음 | 충족 | `AiLogServiceTest` 확인 |
| `REJECTED` validation result는 asset을 `REJECTED`로 전환 | 충족 | `AiLogServiceTest` 확인 |
| 사용자 `/api/v1/ai/**`에 생성 job/asset/log 등록 기능 추가 없음 | 충족 | `rg` 확인 |
| prompt 원문, provider raw response, validation reference 원문, secret/token/password 예시 추가 없음 | 충족 | 변경 diff 및 `rg` 확인 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "*SystemAiAssetControllerTest" --tests "*AiLogUseCaseServiceTest" --tests "*AiGenerationJobTest" --tests "*AiGeneratedAssetTest"` | 최초 RED: `QA_RESPONSE` 차단 테스트 2건 실패 확인 후 수정, 재실행 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "*SystemAiGenerationJobControllerTest" --tests "*SystemAiAssetControllerTest" --tests "*SystemAiValidationLogControllerTest" --tests "*AiGenerationJobTest" --tests "*AiGeneratedAssetTest" --tests "*AiValidationLogTest" --tests "*AiLogServiceTest" --tests "*AiLogUseCaseServiceTest" --tests "*AiServiceTest" --tests "*AiUseCaseContractTest"` | 성공 |
| `.\gradlew.bat test --tests "*Ai*"` (`qtai-server` 디렉터리에서 실행) | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server build` | 성공 |
| `git diff --check` | 성공. CRLF 변환 경고만 출력 |
| `rg -n -g '*.java' 'RequestMapping\("/api/v1/ai\|PostMapping.*generation-jobs\|PostMapping.*assets\|PostMapping.*validation-logs' qtai-server/src/main/java/com/qtai/domain/ai/web` | 매치 없음 |
| `rg -n '^import .*domain\.[a-z]+\.(internal\|web\|repository)' qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |
| `rg -n 'raw response\|provider raw\|password\|private key\|token\|secret' ...` | 기존 `ValidationReferenceJobServiceTest`의 차단 검증 문자열만 매치 |

## 미실행 또는 실패한 검증

| 명령 | 사유 |
| --- | --- |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "*Ai*"` | PowerShell/Gradle 인자 해석 문제로 `Task 'qtai-server' not found` 실패. 동일 범위를 `qtai-server` 디렉터리에서 `.\gradlew.bat test --tests "*Ai*"`로 재실행해 성공 확인 |
| `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | PowerShell ps1 실행 정책으로 `npx` shim 실행 실패. `npx.cmd`로 재시도했으나 루트에 `.spectral.yaml`이 없어 `ENOENT` 실패 |
| `gitleaks detect --source . --redact --exit-code 1` | 현재 환경 PATH에서 `gitleaks` 실행 파일을 찾지 못함 |

## 후속 항목

1. PR 3에서 generation job processor, DeepSeek 호출, asset 저장 흐름 연결.
2. PR 4에서 자동 검증 룰 엔진과 실제 validation log 생성 시점 연결.
3. F-15 Q&A workflow에서 `QA_RESPONSE` 등록 경로와 `ai_qa_requests` 모델을 별도로 설계.
4. `service_accounts` 기반 `/api/v1/system/**` 인증 필터와 감사 로그 actor 연결.
5. 저장소 루트의 `.spectral.yaml` 제공 또는 검증 명령 업데이트.
