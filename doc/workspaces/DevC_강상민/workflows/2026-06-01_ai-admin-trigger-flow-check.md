# Workflow - 2026-06-01 ai-admin-trigger-flow-check

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-trigger-flow-check` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | AI 구현 순서 11번: 관리자 트리거 연동 확인 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-28_ai-implementation-order.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

기존 시스템/관리자 트리거 경로가 만든 `QUEUED` AI generation job이 worker/runner 처리까지 이어지는지 통합 테스트로 고정한다. 이번 작업은 새 API나 새 실행기를 추가하지 않고, 이미 구현된 `CreateAiGenerationJobUseCase`, `RegenerateAiAssetUseCase`, `AiGenerationJobRunner`의 연결 흐름을 실제 JPA repository 기반으로 검증한다.

관리자 재생성 경로는 기존 asset을 덮어쓰지 않고 새 job과 새 asset을 추가해야 한다. 시스템 트리거 경로는 `SYSTEM_BATCH`가 만든 job이 runner 처리 후 asset과 자동 검증 log까지 이어져야 한다.

## 범위

- `AiGenerationTriggerFlowIntegrationTest`를 추가한다.
- 시스템 트리거가 만든 job이 `QUEUED -> SUCCEEDED`로 처리되고 `VALIDATING` asset과 `PASSED` validation log를 남기는지 검증한다.
- 관리자 재생성 트리거가 기존 `REJECTED` asset을 보존하고 새 job/asset/log를 만드는지 검증한다.
- 같은 target/prompt version에 진행 중 job이 있으면 관리자 재생성 job 생성이 차단되는지 회귀 검증한다.
- 실행 결과와 제외 범위를 report로 남긴다.

## 제외 범위

- 신규 API, OpenAPI, DB schema 변경.
- full HTTP E2E 테스트 또는 관리자 UI 구현.
- 감사 로그 연결.
- 04:00 KST scheduler, Spring Batch, retry/backoff 구현.
- 멈춘 `RUNNING` job 회수 정책.
- 사용자 요청 경로 `/api/v1/ai/**` 생성 API.
- provider raw response, prompt 원문, validation reference 원문, secret 저장.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationTriggerFlowIntegrationTest.java` | 시스템/관리자 트리거가 만든 job이 runner 처리까지 이어지는지 통합 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-01_ai-admin-trigger-flow-check_report.md` | 구현 결과, 검증 결과, 제외 범위, 후속 작업 기록 |

## 구현 순서

1. `AiGenerationTriggerFlowIntegrationTest`를 `@DataJpaTest` 기반으로 추가한다.
2. 실제 `AiService`, `AiGenerationJobRunner`, `ExplanationGenerationJobHandler`, `AiAutoValidationService`를 테스트 안에서 조립한다.
3. mock `GetQtPassageContentContextUseCase`, `GetBibleVerseUseCase`, `LlmClient`를 사용해 외부 호출 없이 성공 응답을 만든다.
4. 시스템 트리거 케이스를 추가한다.
   - `CreateAiGenerationJobUseCase.createAiGenerationJob` 호출
   - job `QUEUED` 확인
   - `runQueuedBatch(5)` 호출
   - job `SUCCEEDED`, asset `VALIDATING`, validation log `PASSED` 확인
5. 관리자 재생성 케이스를 추가한다.
   - 기존 `REJECTED` asset 저장
   - `RegenerateAiAssetUseCase.regenerateAiAsset` 호출
   - 기존 asset 보존과 새 job `QUEUED` 확인
   - `runQueuedBatch(5)` 호출
   - 새 asset과 validation log 생성 확인
6. duplicate 진행 중 job 차단 케이스를 추가한다.
   - 같은 target/prompt version의 `QUEUED` job을 미리 저장
   - 관리자 재생성 호출 시 `INVALID_STATUS_TRANSITION` 확인
7. 지정 테스트와 build를 실행하고 report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiGenerationTriggerFlowIntegrationTest` | 시스템 생성 job이 runner 처리로 asset/log까지 이어진다 |
| `AiGenerationTriggerFlowIntegrationTest` | 관리자 재생성 job이 기존 asset 보존 + 새 job/asset/log 생성으로 이어진다 |
| `AiGenerationTriggerFlowIntegrationTest` | 진행 중 duplicate job이 있으면 재생성 job 생성이 차단된다 |

## 수용 기준

- [ ] 신규 API, OpenAPI, DB schema 변경이 없다.
- [ ] 시스템 트리거가 만든 job이 `QUEUED`로 저장된다.
- [ ] runner 처리 후 시스템 트리거 job이 `SUCCEEDED`가 되고 `VALIDATING` asset과 `PASSED` validation log가 생성된다.
- [ ] 관리자 재생성은 기존 asset을 덮어쓰지 않는다.
- [ ] 관리자 재생성은 새 `QUEUED` job을 만들고 runner 처리 후 새 asset/log를 생성한다.
- [ ] 진행 중 duplicate job이 있으면 관리자 재생성을 차단한다.
- [ ] 사용자 요청 경로 `/api/v1/ai/**`를 추가하지 않는다.
- [ ] 감사 로그, scheduler, retry/backoff는 구현하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 테스트 fixture, service 조립, runner 조립이 같은 테스트 파일 안에서 강하게 연결된다.
- 구현 범위가 신규 통합 테스트와 report에 집중되어 병렬화 이점이 작다.
- main agent가 직접 실행해야 DB fixture와 수용 기준을 한 흐름으로 검증하기 쉽다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow 저장, 통합 테스트 추가, 검증 명령 실행, report 작성을 직접 순서대로 수행한다.

## 검증 계획

- `.\gradlew.bat test --tests "*AiGenerationTriggerFlowIntegrationTest"` in `qtai-server`
- `.\gradlew.bat test --tests "*AdminAiAssetControllerTest" --tests "*SystemAiGenerationJobControllerTest"` in `qtai-server`
- `.\gradlew.bat test --tests "*AiGenerationJob*"` in `qtai-server`
- `.\gradlew.bat build` in `qtai-server`
- `git diff --check`
- `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai`
- `rg -n "providerRawResponse|rawResponse|validationReferenceText|promptText|password|private key|token|secret" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai`

## 후속 작업으로 남길 항목

- 감사 로그 도메인 계약 확정 후 `AI_REGENERATE_REQUEST` 기록 연결.
- 관리자 UI에서 재생성 요청 상태와 실패 job 표시.
- 04:00 KST 외부 scheduler 또는 운영 수동 트리거 구성.
- retry/backoff와 멈춘 `RUNNING` job 회수 정책.
