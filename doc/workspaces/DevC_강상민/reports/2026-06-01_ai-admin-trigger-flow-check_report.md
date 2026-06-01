# 관리자/시스템 트리거 연동 확인 리포트

- 작업일: 2026-06-01
- 작업 브랜치: `feature/ai-admin-trigger-flow-check`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-admin-trigger-flow-check.md`
- 관련 기능: F-02, F-14

## 1. 작업 요약

기존 시스템/관리자 트리거가 만든 `QUEUED` AI generation job이 runner 처리까지 이어지는지 JPA 통합 테스트로 고정했다. 새 API, OpenAPI, DB schema, scheduler, retry/backoff는 추가하지 않았다.

## 2. 반영 내용

### 2.1 시스템 트리거 흐름 검증

`CreateAiGenerationJobUseCase`로 `SYSTEM_BATCH` 생성 job을 만든 뒤 `AiGenerationJobRunner.runQueuedBatch(5)`를 실행해 아래 흐름을 검증했다.

- 생성 직후 job 상태는 `QUEUED`
- runner 처리 후 job 상태는 `SUCCEEDED`
- `ai_generated_assets`에 `VALIDATING` 상태의 `EXPLANATION` asset 생성
- `ai_validation_logs`에 `AUTO` reviewer의 `PASSED` log 생성

### 2.2 관리자 재생성 흐름 검증

기존 `REJECTED` asset을 준비한 뒤 `RegenerateAiAssetUseCase`로 재생성 job을 만들고 runner 처리까지 검증했다.

- 기존 asset은 `REJECTED` 상태로 보존
- 재생성 요청은 새 `QUEUED` job 생성
- runner 처리 후 새 asset 생성
- 새 asset에 대한 `PASSED` validation log 생성

### 2.3 duplicate 진행 중 job 차단 회귀 검증

같은 target/prompt version에 이미 `QUEUED` job이 있으면 관리자 재생성 요청이 `INVALID_STATUS_TRANSITION`으로 차단되는지 검증했다.

## 3. 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationTriggerFlowIntegrationTest.java` | 시스템/관리자 트리거 job 생성부터 runner 처리까지 통합 검증 |
| `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-admin-trigger-flow-check.md` | 작업단위 명세서 |
| `doc/workspaces/DevC_강상민/reports/2026-06-01_ai-admin-trigger-flow-check_report.md` | 실행 결과 리포트 |

## 4. 검증 기록

실행 완료:

```powershell
.\gradlew.bat test --tests "*AiGenerationTriggerFlowIntegrationTest"
.\gradlew.bat test --tests "*AdminAiAssetControllerTest" --tests "*SystemAiGenerationJobControllerTest"
.\gradlew.bat test --tests "*AiGenerationJob*"
.\gradlew.bat build
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai
rg -n "providerRawResponse|rawResponse|validationReferenceText|promptText|password|private key|token|secret" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai
```

결과:

- `AiGenerationTriggerFlowIntegrationTest` 통과
- `AdminAiAssetControllerTest`, `SystemAiGenerationJobControllerTest` 통과
- `AiGenerationJob*` 테스트 통과
- 전체 `build` 통과
- `git diff --check` 통과
- 도메인 경계 금지 import 검색 결과 없음
- 민감 키워드 검색 결과는 기존 저장 금지 guard, 금지 검증 테스트, token usage 필드로 확인

## 5. 수용 기준 점검

- 신규 API, OpenAPI, DB schema 변경 없음
- 시스템 트리거 job `QUEUED` 저장 확인
- 시스템 트리거 job runner 처리 후 `SUCCEEDED`, `VALIDATING` asset, `PASSED` validation log 확인
- 관리자 재생성 기존 asset 보존 확인
- 관리자 재생성 새 job/asset/log 생성 확인
- duplicate 진행 중 job 차단 확인
- 사용자 API `/api/v1/ai/**` 생성 경로 추가 없음
- 감사 로그, scheduler, retry/backoff 구현 없음

## 6. 후속 작업 후보

- audit 도메인 계약 확정 후 `AI_REGENERATE_REQUEST` 감사 로그 연결
- 관리자 UI에서 재생성 요청 상태와 실패 job 표시
- 04:00 KST 외부 scheduler 또는 운영 수동 트리거 구성
- retry/backoff와 멈춘 `RUNNING` job 회수 정책 구현
