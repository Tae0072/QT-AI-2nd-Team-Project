# Report - 2026-06-01 ai-admin-regenerate-audit-log

## 작업 정보

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `feature/ai-admin-regenerate-audit-log` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-admin-regenerate-audit-log.md` |
| 관련 F-ID | F-02, F-14 |

## 실행 요약

관리자 AI 산출물 재생성 성공 흐름에 `AI_REGENERATE_REQUEST` 감사 로그 연결을 추가했다. 신규 HTTP API, OpenAPI, DB schema 변경은 없다.

감사 로그는 `RegenerateAiAssetUseCase` 성공 흐름에서 새 generation job 저장이 성공한 뒤 1회 기록된다. 실패/차단 흐름에서는 audit write가 호출되지 않도록 테스트로 고정했다.

## 변경 내용

- `AiService`에 `WriteAuditLogUseCase`와 `ObjectMapper`를 주입했다.
- `regenerateAiAsset(...)`에서 `saveQueuedJob(job)` 성공 직후 audit write를 호출하도록 연결했다.
- audit actor/target 값은 다음 기준으로 고정했다.
  - `actorType=ADMIN`
  - `actorId=adminId`
  - `actorLabel=ADMIN:{adminId}`
  - `actionType=AI_REGENERATE_REQUEST`
  - `targetType=AI_GENERATED_ASSET`
  - `targetId=기존 assetId`
- `beforeJson`에는 기존 asset의 `id`, `assetType`, `status`, `targetType`, `targetId`만 저장한다.
- `afterJson`에는 새 job의 `id`, `status`, `jobType`, `targetType`, `targetId`, `promptVersionId`, `requestedAt`만 저장한다.
- 관리자 `reason` 원문, payload/content 원문, prompt 원문, provider raw response, validation reference 원문, secret/token/password/private key 계열 값은 audit snapshot에 저장하지 않는다.

## 테스트 보강

- `AiServiceTest`
  - 재생성 성공 시 audit write 1회 호출 검증.
  - audit request actor/action/target 및 before/after snapshot 최소 필드 검증.
  - raw/prompt/provider/reference/secret 계열 값 미포함 검증.
  - 권한 실패, 상태 실패, duplicate job, unique constraint 실패 시 audit write 미호출 검증.
- `AiGenerationTriggerFlowIntegrationTest`
  - 관리자 재생성 통합 흐름에서 새 job 생성 직후 audit write 1회 호출 검증.
  - duplicate active job 차단 시 audit write 미호출 검증.

## TDD 기록

1. `AiServiceTest`에 audit write 검증을 먼저 추가한 뒤 실패를 확인했다.
2. `AiService` 생성자에 audit 의존성을 추가하고 테스트 fixture를 맞췄다.
3. audit write 구현 전 `Wanted but not invoked` 실패를 확인했다.
4. audit write 구현 후 `requestedAt` 포맷 차이를 확인했고, `DateTimeFormatter.ISO_OFFSET_DATE_TIME`으로 고정했다.
5. 관련 단위/통합 테스트와 전체 build를 재실행했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AiServiceTest"` | 성공 |
| `.\gradlew.bat test --tests "*AiGenerationTriggerFlowIntegrationTest"` | 성공 |
| `.\gradlew.bat test --tests "*AuditServiceTest"` | 성공 |
| `.\gradlew.bat test --tests "*AiGenerationJob*"` | 성공 |
| `.\gradlew.bat build` | 성공 |
| `git diff --check` | 성공. CRLF 변환 warning만 출력 |
| `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | 추가 금지 import 없음 |
| `rg -n "providerRawResponse|rawResponse|validationReferenceText|promptText|password|private key|token|secret" ...` | 기존 guard/test와 이번 negative assertion만 확인 |

참고: 테스트를 병렬로 한 번 실행했을 때 Gradle test result 파일 충돌이 발생했으나, 동일 명령을 순차 재실행해 모두 성공했다.

## 수용 기준 확인

- 관리자 재생성 성공 시 `AI_REGENERATE_REQUEST` audit request 생성: 충족.
- actor/target 필드 고정: 충족.
- before/after snapshot 최소 필드 저장: 충족.
- reason/raw payload/prompt/provider/reference/secret 계열 값 미저장: 충족.
- 실패/차단 흐름 audit write 미호출: 충족.
- 신규 API, OpenAPI, DB schema 변경 없음: 충족.

## 후속 작업

- AI 산출물 승인/반려/숨김 audit 연결은 별도 PR로 분리한다.
- audit 조회 API의 `AI_REGENERATE_REQUEST` 필터 표시는 별도 확인 대상이다.
- 관리자 UI의 재생성 요청 이력 노출은 이번 범위에서 제외했다.
