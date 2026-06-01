# Workflow - 2026-06-01 ai-admin-regenerate-audit-log

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-regenerate-audit-log` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | PR 7 후속 작업. 관리자 재생성 요청을 `AI_REGENERATE_REQUEST` 감사 로그로 남긴다. |
| 주요 코드 | `AiService`, `AiServiceTest`, `AiGenerationTriggerFlowIntegrationTest` |

## 작업 목표

기존 관리자 AI 산출물 재생성 흐름에서 새 `QUEUED` generation job 저장이 성공한 직후 감사 로그를 남긴다. 신규 API, OpenAPI, DB schema 변경은 하지 않고, 기존 `domain.audit.api.WriteAuditLogUseCase` 계약만 사용한다.

감사 로그 snapshot에는 운영 추적에 필요한 최소 식별자만 저장한다. 관리자 입력 `reason` 원문, 산출물 payload/content 원문, prompt 원문, provider raw response, validation reference 원문, secret/token/password/private key 계열 값은 저장하지 않는다.

## 구현 범위

- `RegenerateAiAssetUseCase` 성공 흐름 1개만 audit에 연결한다.
- `AiService`에 `WriteAuditLogUseCase`와 `ObjectMapper`를 주입한다.
- 재생성 job 저장 성공 후 `AI_REGENERATE_REQUEST` audit write를 호출한다.
- `beforeJson`에는 기존 asset의 `id`, `assetType`, `status`, `targetType`, `targetId`만 포함한다.
- `afterJson`에는 새 generation job의 `id`, `status`, `jobType`, `targetType`, `targetId`, `promptVersionId`, `requestedAt`만 포함한다.
- audit snapshot serialization 실패는 기존 write service 흐름처럼 트랜잭션 실패로 처리한다.

## 제외 범위

- 신규 HTTP API, OpenAPI, DB migration.
- audit 조회 API 확장.
- 승인/반려/숨김 audit 연결.
- 시스템 생성 job, validation reference job audit 정책 변경.
- 관리자 UI, notification, 운영 dashboard 변경.

## 감사 로그 필드

| 필드 | 값 |
| --- | --- |
| `actorType` | `ADMIN` |
| `actorId` | `adminId` |
| `actorLabel` | `ADMIN:{adminId}` |
| `actionType` | `AI_REGENERATE_REQUEST` |
| `targetType` | `AI_GENERATED_ASSET` |
| `targetId` | 기존 `assetId` |

## TDD 순서

1. `AiServiceTest`에 재생성 성공 시 audit write 호출 검증 테스트를 먼저 추가하고 실패를 확인한다.
2. 실패/차단 흐름에서는 audit write가 호출되지 않는 회귀 테스트를 보강한다.
3. `AiService` 생성자에 audit 의존성을 추가하고, 재생성 job 저장 성공 직후 audit write를 연결한다.
4. `AiGenerationTriggerFlowIntegrationTest`의 관리자 재생성 흐름에서 audit write 1회 호출을 검증한다.
5. 기존 `AuditServiceTest`를 유지해 audit write 계약 회귀를 확인한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiServiceTest` | 재생성 성공 시 `AI_REGENERATE_REQUEST` audit write 호출 |
| `AiServiceTest` | actor/action/target 필드와 before/after snapshot 최소 필드 검증 |
| `AiServiceTest` | reason, raw payload/content, prompt/provider/reference/secret 계열 값 미포함 검증 |
| `AiServiceTest` | 권한 실패, 상태 실패, duplicate job, unique constraint 실패 시 audit write 미호출 |
| `AiGenerationTriggerFlowIntegrationTest` | 관리자 재생성 통합 흐름에서 audit write 1회 호출 |
| `AuditServiceTest` | 기존 audit 저장 계약 회귀 확인 |

## 수용 기준

- [ ] 관리자 재생성 성공 시 `AI_REGENERATE_REQUEST` audit request가 생성된다.
- [ ] audit actor는 `ADMIN`, `actorId=adminId`, `actorLabel=ADMIN:{adminId}`로 기록된다.
- [ ] audit target은 기존 asset 기준 `AI_GENERATED_ASSET`, `targetId=assetId`로 기록된다.
- [ ] `beforeJson`과 `afterJson`에는 최소 식별자와 상태만 포함된다.
- [ ] 관리자 `reason` 원문과 AI 원문 데이터는 audit snapshot에 포함되지 않는다.
- [ ] 실패/차단 흐름에서는 audit write가 호출되지 않는다.
- [ ] 신규 API, OpenAPI, DB schema 변경이 없다.

## Subagent Decision

Subagent 사용은 권장하지 않는다. 변경 중심이 `AiService` 생성자, 재생성 흐름, 단위 테스트, 통합 테스트 fixture에 묶여 있어 직접 순차 실행이 더 안전하다.

## 검증 계획

- `.\gradlew.bat test --tests "*AiServiceTest"` in `qtai-server`
- `.\gradlew.bat test --tests "*AiGenerationTriggerFlowIntegrationTest"` in `qtai-server`
- `.\gradlew.bat test --tests "*AuditServiceTest"` in `qtai-server`
- `.\gradlew.bat test --tests "*AiGenerationJob*"` in `qtai-server`
- `.\gradlew.bat build` in `qtai-server`
- `git diff --check`
- `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai`
- `rg -n "providerRawResponse|rawResponse|validationReferenceText|promptText|password|private key|token|secret" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai`

## 후속 작업 후보

- AI 산출물 승인/반려/숨김 audit 연결.
- audit 조회 API에서 `AI_REGENERATE_REQUEST` 필터 표시 검증.
- 관리자 UI에서 재생성 요청 이력 표시.
- 시스템 배치/service account actorId 정책 정리.
