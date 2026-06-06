# Workflow - 2026-06-02 ai-admin-monitoring-summary-api

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-monitoring-summary-api` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 저장 대상 | `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-admin-monitoring-summary-api.md` |
| 리포트 대상 | `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-admin-monitoring-summary-api_report.md` |

## 작업 목표

관리자 웹에서 AI 운영 상태를 한 번에 볼 수 있도록 `GET /api/v1/admin/ai/monitoring` 집계 API를 추가한다. 신규 DB schema/migration은 추가하지 않고 기존 AI generation job, generated asset, validation log, checklist version, batch run log 데이터를 집계한다.

## 공개 인터페이스

- 신규 endpoint: `GET /api/v1/admin/ai/monitoring`
- 권한: `ADMIN + OPERATOR/REVIEWER/SUPER_ADMIN`
- query parameter:
  - `from`, `to`: 선택값, `yyyy-MM-dd`, KST 기준
  - 둘 다 없으면 KST 오늘 날짜를 기본 기간으로 사용
  - `from` inclusive, `to`는 해당 날짜의 다음 날 00:00 exclusive
- 응답:
  - `period`: `from`, `to`, `timezone`
  - `generationJobs`: `queued`, `running`, `succeeded`, `failed`
  - `validation`: `waitingAssets`, `passCount`, `failCount`, `needsReviewCount`, `failureReasons`
  - `batchRuns`: `succeeded`, `partialFailed`, `failed`, `latestFailures`
  - `qa`: Q&A 미구현 상태이므로 0값과 빈 사유 목록
  - `checklists`: active checklist별 `checklistType`, `activeVersion`, `passRate`

## 구현 기준

- `queued/running`은 현재 active backlog 기준으로 기간과 무관하게 카운트한다.
- `succeeded/failed`는 `finishedAt`이 기간 내인 job만 카운트한다.
- `waitingAssets`는 현재 `VALIDATING` asset 카운트다.
- validation 집계는 기간 내 `ai_validation_logs.createdAt` 기준이다.
- `failureReasons`는 기간 내 `REJECTED` validation log의 `errorMessage` 기준으로 그룹핑하고, null/blank는 `REJECTED`로 표시한다. 상위 10개만 반환한다.
- checklist `passRate`는 active checklist version별 기간 내 `PASSED / (PASSED + REJECTED + NEEDS_REVIEW)`로 계산하고, 분모 0이면 `0.0`을 반환한다.
- `batchRuns`는 기간 내 `ai_batch_run_logs.createdAt` 기준 status별 카운트와 최신 실패/부분 실패 최대 5건을 반환한다.
- raw prompt, provider response, asset content, secret/token 계열 값은 응답에 포함하지 않는다.

## 구현 순서

1. workflow 문서를 저장한다.
2. `AdminAiMonitoringControllerTest`, `AdminAiMonitoringQueryServiceTest`, `AdminAiMonitoringQueryRepositoryTest`, `AiUseCaseContractTest`를 먼저 추가/수정해 RED를 확인한다.
3. `domain.ai.api`에 `GetAdminAiMonitoringUseCase`와 DTO record들을 추가한다.
4. `domain.ai.web`에 `AdminAiMonitoringController`를 추가한다.
5. `domain.ai.internal`에 `AdminAiMonitoringQueryService`, `AdminAiMonitoringQueryRepository`를 추가한다.
6. OpenAPI와 `04_API_명세서.md`를 갱신한다.
7. 관련 테스트와 build를 실행한다.
8. report 문서를 작성한다.

## 테스트 계획

- `AdminAiMonitoringControllerTest`
  - monitoring 권한 role 성공/차단
  - query parameter mapping
  - 기본 기간 요청
- `AdminAiMonitoringQueryServiceTest`
  - KST 오늘 기본 기간 계산
  - `from > to`, 잘못된 날짜 형식, 권한 실패
  - Q&A 0값 응답
- `AdminAiMonitoringQueryRepositoryTest`
  - generation job active/terminal 집계
  - validation count와 failure reason grouping/order/limit
  - batch run status count와 latest failures order/limit
  - active checklist version과 passRate 계산
- `AiUseCaseContractTest`
  - 신규 UseCase/DTO 계약 추가

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AdminAiMonitoring*"
.\gradlew.bat test --tests "*AiUseCaseContractTest"
.\gradlew.bat test --tests "*AdminAiBatchRunLog*"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai
```

OpenAPI 변경 후 `.spectral.yaml`이 있으면 Spectral lint를 실행하고, 없으면 YAML parse 검증으로 대체한다.

## 제외 범위

- 신규 DB schema/migration 없음
- batch 동작 변경 없음
- 관리자 조회 감사 로그 write 없음
- `/api/v1/admin/ai/batch-run-logs` 상세 목록 API 변경 없음
- Q&A 실제 집계 구현 없음

## Subagent Decision

Subagent 사용은 권장하지 않는다. API 응답 계약, aggregate query, OpenAPI/문서 정합성이 같은 맥락에 묶여 있어 메인 agent가 TDD 순서로 직접 진행한다.
