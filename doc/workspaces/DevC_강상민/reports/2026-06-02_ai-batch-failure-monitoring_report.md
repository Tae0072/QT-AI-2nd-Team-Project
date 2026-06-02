# AI Batch 실패 알림/모니터링 연동 Report

## Summary

- 브랜치: `feature/ai-batch-failure-monitoring`
- PR 대상: `dev`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-batch-failure-monitoring.md`
- 목표: 외부 webhook 없이 `DB 실행 요약 + 구조화 로그`로 AI batch 실패를 추적 가능하게 만든다.

## 구현 내용

- `ai_batch_run_logs` 테이블을 추가했다.
  - batch name: `AI_DAILY_QT_VERSE_EXPLANATION_SEED`, `AI_GENERATION_WORKER_POLL`
  - status: `SUCCEEDED`, `PARTIAL_FAILED`, `FAILED`
  - count: `createdCount`, `failedCount`, `processedCount`
  - failure: `errorType`, `errorMessage`
- `AiBatchRunLog`, `AiBatchRunLogRepository`, `AiBatchMonitoringService`를 `domain.ai.internal`에 추가했다.
- `AiDailyQtVerseExplanationSeedScheduler`가 시딩 실행 결과를 batch run log로 남기도록 연결했다.
  - `failedCount=0`: `SUCCEEDED`
  - `failedCount>0`: `PARTIAL_FAILED`
  - `failureReason` 또는 service 예외: `FAILED`
- active prompt 미존재는 `AiDailyQtVerseExplanationSeedResult.failureReason`으로 반환해 정상 0건과 구분했다.
- `AiGenerationJobWorker`는 polling 실패만 `FAILED` batch run log로 저장한다.
- monitoring write 실패는 scheduler/worker 밖으로 전파하지 않고 warn 로그만 남긴다.

## 제외한 내용

- 신규 HTTP API/OpenAPI 변경 없음.
- 사용자 앱 알림(`domain.notification`) 사용 없음.
- Slack/Discord/webhook 연동 없음.
- Actuator/Micrometer 의존성 추가 없음.
- worker 성공 poll DB 저장 없음.
- 개별 generation job 실패는 기존 `ai_generation_jobs.status/error_message`에 계속 남긴다.
- 관리자 조회 API, retention/정리 배치, 운영 dashboard는 후속 작업으로 둔다.

## TDD 기록

- RED:
  - `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeedSchedulerTest" --tests "*AiGenerationJobWorkerTest" --tests "*AiBatchRunLogRepositoryTest"`
  - 신규 `AiBatchRunLog*`, `AiBatchMonitoringService` 타입 미존재로 compile fail 확인.
- GREEN:
  - 신규 migration/entity/repository/service 추가 후 동일 테스트 통과.
  - `failureReason` 변경 후 `*AiDailyQtVerseExplanationSeed*` 통과 확인.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AiBatchRun*"` | PASS |
| `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeedSchedulerTest"` | PASS |
| `.\gradlew.bat test --tests "*AiGenerationJobWorkerTest"` | PASS |
| `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeed*"` | PASS |
| `.\gradlew.bat test --tests "*MigrationCoverageTest"` | PASS |
| `.\gradlew.bat test --tests "com.qtai.MysqlMigrationValidationTest"` | Gradle PASS, Docker 부재로 1건 skip |
| `.\gradlew.bat build` | PASS |
| `git diff --check` | PASS, CRLF 변환 경고만 출력 |
| `rg -n "^import .*domain\.[a-z]+\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | match 없음 |

## Assumptions

- 이번 PR의 "알림"은 외부 발송이 아니라 운영자가 추적 가능한 DB 실행 요약과 warn 로그를 의미한다.
- `ai_batch_run_logs`는 append-only 성격으로 두고, 보존 기간/정리 정책은 후속 PR에서 정한다.
- DB 기록 실패는 원 batch 흐름을 실패시키지 않는 것이 운영 안정성에 더 적합하다.
