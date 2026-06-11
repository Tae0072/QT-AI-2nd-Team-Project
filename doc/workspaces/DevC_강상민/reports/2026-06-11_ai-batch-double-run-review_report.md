# Report - 2026-06-11 ai-batch-double-run-review

## Summary

- Branch: `docs/ai-batch-double-run-review`
- Target: `dev`
- Workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-11_ai-batch-double-run-review.md`
- Scope: 코드리뷰 TODO 3, 배치 이중 실행 PR의 AI 시딩 측 검토
- 기준 커밋: `5b6be37 fix(admin): 일일 배치 이중 실행 차단 — admin-server 스케줄러 토글 기본 off (#468)`

## Review Result

결론: PR #468 방향은 AI 도메인 관점에서 타당하다. admin-server 복사본의 00:02 SU 수집과 00:05 AI 해설 시딩은 기본 off로 고정됐고, 일일 배치 소유권은 service-bible/service-ai 쪽으로 남아 있다.

## Checked Items

### 1. service-ai 단독 00:05 시딩

- `service-ai`의 `SchedulingConfig`는 `ai.scheduling.enabled=true`일 때만 `@EnableScheduling`을 켠다.
- `service-ai`의 `AiDailyQtVerseExplanationSeedScheduler`는 `0 5 0 * * *` KST cron을 유지한다.
- 시더 자체 토글은 `ai.daily-qt-verse-seed.enabled:true` 기본값이다.
- 따라서 운영에서 `AI_SCHEDULING_ENABLED=true`를 주입하면 service-ai 단독 00:05 시딩이 실행될 수 있다.
- 반대로 기본 local/test 부팅은 `AI_SCHEDULING_ENABLED=false`라 외부 호출 없이 안정적으로 뜨는 구조다.

### 2. service-bible 단독 00:02 SU 수집

- `service-bible`은 `BibleServiceApplication`에서 `@EnableScheduling`을 직접 활성화한다.
- `SuTodayPassageImportScheduler`는 `0 2 0 * * *` KST cron이며, `qt.today-source.sum.enabled`가 기본 true다.
- 00:02 수집과 00:05 AI 시딩의 시간 순서는 코드상 유지된다.
- 시딩은 오늘 QT 본문이 없으면 실패/스킵 상태를 batch run log로 남기는 구조라, 수집 실패가 조용히 성공으로 처리되지는 않는다.

### 3. admin-server 복사본 비활성화

- `admin-server/src/main/resources/application.yml`
  - `ai.daily-qt-verse-seed.enabled: ${AI_DAILY_QT_VERSE_SEED_ENABLED:false}`
  - `qt.today-source.sum.enabled: ${QT_TODAY_SUM_ENABLED:false}`
- admin-server는 `@EnableScheduling`이 켜져 있지만, 위 두 스케줄러는 각 스케줄러 내부 `enabled=false`로 즉시 return한다.
- 토글 off 테스트가 시딩 서비스/배치 기록 또는 SU 외부 호출/저장/백필을 호출하지 않음을 확인한다.

### 4. service-ai -> admin-server 감사 로그 기록

- service-ai `WriteAuditLogRestClientAdapter`는 `POST /api/v1/system/audit-logs`로 admin-server에 감사 로그를 전송한다.
- `SystemTokenProvider`가 있으면 SYSTEM_BATCH Bearer token을 붙인다.
- 토큰 발급기 미설정 또는 HTTP 5xx는 fire-and-forget으로 예외를 던지지 않는다.
- admin-server `SecurityConfig`는 `/api/v1/system/**`에 `ROLE_SYSTEM_BATCH`를 요구한다.
- admin-server `SystemAuditLogController`는 `/api/v1/system/audit-logs`에서 `WriteAuditLogUseCase.write`를 호출한다.

### 5. AiBatchRunLog와 화면 집계 영향

- `AiBatchRunLog`는 단일 DB 공유 구조상 service-ai가 기록한 행을 admin-server의 AD-08 batch run log/monitoring query가 조회할 수 있다.
- AD-01 대시보드는 `AdminAiMonitoringResponse.validation.waitingAssets`만 `pendingAiValidationCount`로 사용한다. batch run success/failure count를 AD-01 요약 수치로 직접 쓰지 않으므로, 이중 실행 로그가 AD-01 핵심 숫자를 직접 왜곡하지 않는다.
- AD-08 모니터링의 batch run count는 조회 기간의 `AiBatchRunLog`를 status별로 집계한다. PR #468 이후 신규 이중 실행은 차단되지만, fix 이전 날짜를 포함해 조회하면 과거 dual-run 로그는 그대로 집계될 수 있다.

## Verification

| Command | Result |
| --- | --- |
| `.\gradlew.bat :admin-server:test --tests "*AiDailyQtVerseExplanationSeedSchedulerToggleTest" --tests "*SuTodayPassageImportSchedulerToggleTest"` | PASS |
| `.\gradlew.bat :service-ai:test --tests "*WriteAuditLogRestClientAdapterTest"` | PASS |

## Recommendation

- PR #468은 AI 시딩 측에서 approve 가능하다.
- 운영 배포에서는 service-ai에 `AI_SCHEDULING_ENABLED=true`가 주입되는지 별도로 확인해야 한다. 이 값이 없으면 service-ai 00:05 시딩도 실행되지 않는다.
- admin-web AD-08에서 과거 기간을 조회할 때 2026-06-10 이전 dual-run 로그가 보일 수 있다. 실데이터에서 혼동이 크면 화면 설명 또는 날짜 필터 안내로 처리하고, 데이터 삭제는 별도 운영 결정으로 분리한다.

## Follow-up

- 김지민 admin-web 모니터링 화면에서 batch run 조회 기본 기간이 fix 이후 날짜로 잡히는지 확인한다.
- 배포 문서나 compose/env 문서에 service-ai `AI_SCHEDULING_ENABLED=true` 운영 주입 여부가 명시되어 있는지 Lead와 확인한다.
