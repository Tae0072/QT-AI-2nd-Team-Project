# Workflow - 2026-06-11 ai-batch-double-run-review

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-batch-double-run-review` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | 코드리뷰 후속 TODO 3: 배치 이중 실행 PR을 AI 시딩 측에서 검증 |
| 기준 문서 | `doc/workspaces/DevC_강상민/2026-06-10_코드리뷰_TODO_강상민.md`, `doc/2026-06-10_서버_코드리뷰.md`, PR #468 커밋 `5b6be37` |
| 담당 경로 | `doc/workspaces/DevC_강상민/workflows/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

admin-server 복사본의 일일 배치가 기본 off로 전환된 뒤, AI 도메인 관점에서 00:05 해설 시딩 소유권과 배치/감사 로그 흐름이 깨지지 않는지 코드와 테스트로 검토한다.

## 범위

- service-ai 단독 실행 시 00:05 AI 해설 시딩이 동작할 수 있는 조건을 확인한다.
- service-bible 단독 실행 시 00:02 성서유니온 수집과 시간 순서가 유지되는지 확인한다.
- admin-server의 `ai.daily-qt-verse-seed.enabled=false`, `qt.today-source.sum.enabled=false` 기본값과 토글 off 테스트를 확인한다.
- service-ai에서 admin-server로 감사 로그를 전송하는 `WriteAuditLogRestClientAdapter`와 admin-server 시스템 수신 경로를 확인한다.
- `AiBatchRunLog` 기반 AD-08 모니터링/AD-01 대시보드 영향 여부를 정리한다.

## 제외 범위

- 코드 변경.
- 운영 배포 설정 변경.
- 실제 운영 DB의 과거 중복 로그 정리.
- admin-web 화면 문구/필터 변경.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-11_ai-batch-double-run-review_report.md` | TODO 3 검토 결과와 검증 로그 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-11_ai-batch-double-run-review.md` | 문서 작업 workflow |

## 구현 순서

1. TODO 3과 PR #468 변경 범위를 확인한다.
2. admin-server `application.yml`과 토글 테스트를 확인한다.
3. service-ai/service-bible 스케줄링 설정과 cron을 확인한다.
4. 감사 로그 RestClient와 admin-server system endpoint 보안 경로를 확인한다.
5. AD-01/AD-08 batch run log 집계 영향을 확인한다.
6. 관련 테스트를 실행하고 report를 작성한다.

## 테스트 보강 목록

문서 검토 작업이므로 테스트 파일 추가는 없다. 기존 토글/RestClient 테스트를 실행해 검증한다.

## 수용 기준

- [ ] admin-server 복사본의 00:02/00:05 일일 배치가 기본 off임을 확인한다.
- [ ] service-ai/service-bible 도메인 서비스 소유 기준의 00:02/00:05 순서가 유지됨을 확인한다.
- [ ] service-ai 감사 로그 전송과 admin-server 시스템 수신 경로가 유지됨을 확인한다.
- [ ] AD-01/AD-08에 남는 영향과 후속 필요 여부를 명시한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 코드 변경 없이 한 흐름의 검토 결과를 문서화하는 작업이다.
- 스케줄러, 감사 로그, 모니터링 집계를 함께 읽어야 하므로 단일 작업자가 직접 정리하는 편이 정확하다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 코드 확인, 테스트 실행, report 작성을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :admin-server:test --tests "*AiDailyQtVerseExplanationSeedSchedulerToggleTest" --tests "*SuTodayPassageImportSchedulerToggleTest"
.\gradlew.bat :service-ai:test --tests "*WriteAuditLogRestClientAdapterTest"
cd ..
git diff --check
git status --short
```

## 후속 작업으로 남길 항목

- admin-web AD-08에서 2026-06-10 이전 기간의 이중 batch run 로그가 운영자에게 혼동을 주는지 실데이터로 확인한다.
