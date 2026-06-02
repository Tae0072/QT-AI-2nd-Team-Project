# AI Batch 실패 알림/모니터링 연동 Workflow

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-batch-failure-monitoring` |
| PR 대상 | `dev` |
| 저장 대상 | `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-batch-failure-monitoring.md` |
| 리포트 대상 | `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-batch-failure-monitoring_report.md` |
| 트리거 | AI 일일 QT 절 시딩 후속: batch 실패 알림/모니터링 연동 |

## 작업 목표

외부 webhook 없이 `DB 실행 요약 + 구조화 로그`로 AI batch 실패를 추적 가능하게 만든다. 범위는 00:05 일일 QT 절 해설 시딩 scheduler와 `AiGenerationJobWorker` polling 실패까지로 제한한다.

## 공개 인터페이스

- 신규 HTTP API 없음.
- OpenAPI 변경 없음.
- 사용자 앱 알림(`domain.notification`) 사용 없음.
- Slack/Discord/webhook 연동 없음.
- Actuator/Micrometer 의존성 추가 없음.
- 내부 모니터링용 DB 테이블 1개만 추가한다.

## 구현 범위

- 신규 migration `V22__create_ai_batch_run_logs.sql`로 `ai_batch_run_logs`를 추가한다.
- `domain.ai.internal`에 batch run log entity, enum, repository, monitoring service를 추가한다.
- monitoring write는 best-effort로 처리한다. DB 기록 실패가 scheduler/worker 동작을 중단시키지 않는다.
- `AiDailyQtVerseExplanationSeedScheduler`는 실행 요약을 저장한다.
  - `failedCount=0`이면 `SUCCEEDED`
  - `failedCount>0`이면 `PARTIAL_FAILED`
  - `failureReason`이 있거나 service 예외가 있으면 `FAILED`
- `AiGenerationJobWorker`는 polling 실패만 `FAILED`로 저장한다.
- worker 성공 poll은 10초 주기 테이블 증가를 피하기 위해 DB에 저장하지 않는다.
- 개별 generation job 실패는 기존 `ai_generation_jobs.status/error_message`에 남기고 별도 batch log로 중복 기록하지 않는다.

## 구현 순서

1. workflow 문서를 저장한다.
2. scheduler/worker/monitoring service 테스트를 먼저 추가하고 실패를 확인한다.
3. `ai_batch_run_logs` migration과 entity/repository/service를 추가한다.
4. `AiDailyQtVerseExplanationSeedResult`에 nullable `failureReason`을 추가하되 기존 2-arg 생성자를 유지한다.
5. scheduler와 worker에 monitoring service를 주입하고 best-effort 기록을 연결한다.
6. 관련 테스트와 build를 실행한다.
7. report 문서를 작성한다.

## 테스트 계획

- `AiDailyQtVerseExplanationSeedSchedulerTest`
  - 성공 결과가 `SUCCEEDED` batch run log로 기록된다.
  - `failedCount>0` 결과가 `PARTIAL_FAILED`로 기록되고 warn 로그를 남긴다.
  - `failureReason`이 있으면 `FAILED`로 기록된다.
  - service 예외 발생 시 `FAILED` 기록 후 scheduler 예외 전파 없음.
  - monitoring write 실패도 scheduler 예외로 전파되지 않는다.
- `AiGenerationJobWorkerTest`
  - polling 실패 시 `AI_GENERATION_WORKER_POLL + FAILED` 기록.
  - worker 성공/0건 처리 시 DB log 미기록.
  - monitoring write 실패가 worker 예외로 전파되지 않음.
- `AiBatchRunLogRepositoryTest`
  - entity 저장과 batchName/status 조회용 repository 동작을 검증한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AiBatchRun*"
.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeedSchedulerTest"
.\gradlew.bat test --tests "*AiGenerationJobWorkerTest"
.\gradlew.bat test --tests "*MigrationCoverageTest"
.\gradlew.bat test --tests "com.qtai.MysqlMigrationValidationTest"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai
```

## 제외 범위

- 관리자 조회 API는 다음 PR로 분리한다.
- 외부 알림 채널 연동은 하지 않는다.
- retention/정리 배치, 운영 dashboard, Actuator metric은 후속 작업으로 둔다.
