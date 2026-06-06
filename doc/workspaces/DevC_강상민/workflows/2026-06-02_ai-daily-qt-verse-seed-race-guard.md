# Workflow - 2026-06-02 ai-daily-qt-verse-seed-race-guard

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-daily-verse-seed-race-guard` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 저장 대상 | `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-daily-qt-verse-seed-race-guard.md` |
| 리포트 대상 | `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-daily-qt-verse-seed-race-guard_report.md` |
| 작업 유형 | AI 일일 QT 절 시딩 중복 실행 race 보강 |

## 작업 목표

일일 QT 절 단위 `EXPLANATION + BIBLE_VERSE` 시딩이 중복 scheduler 실행이나 수동 트리거 경합 상황에서도 같은 verse에 active generation job을 중복 생성하지 못하게 고정한다.

## 범위

- DB와 서비스 양쪽에서 promptVersion 무관 active target 중복을 막는다.
- 신규 Flyway migration `V21__add_ai_generation_job_active_target_unique.sql`을 추가한다.
- `ai_generation_jobs(job_type, target_type, target_id, active_unique_key)` unique key를 추가한다.
- 기존 `job_type + target_type + target_id + prompt_version_id + active_unique_key` unique key는 유지한다.
- `AiGenerationJob` JPA `@Table` unique constraint에도 신규 key를 반영한다.
- `AiService`의 중복 판단을 promptVersion 무관 기준으로 변경한다.
- 일일 시딩 서비스는 active job unique race loser를 실패가 아니라 skip으로 처리한다.
- `qtai-server/02_ERD_문서.md`의 `ai_generation_jobs.active_unique_key`와 unique key 설명을 정합화한다.

## 제외 범위

- 신규 HTTP API, OpenAPI 변경 없음.
- worker 처리 흐름 변경 없음.
- scheduler 시간 변경 없음.
- retry/backoff, schedlock, 운영 알림, batch 모니터링 구현 없음.
- `AiDailyQtVerseExplanationSeedResult` shape 변경 없음.

## 구현 순서

1. 이 workflow 문서를 저장한다.
2. `AiServiceTest`에 promptVersion이 달라도 같은 active target이면 생성/재생성이 차단되는 RED 테스트를 추가한다.
3. `AiGenerationJobRepositoryTest`에 다른 promptVersion의 동일 active target insert 실패와 종료 job 이후 재생성 허용 RED 테스트를 추가한다.
4. `AiDailyQtVerseExplanationSeedServiceTest`에 duplicate active job `BusinessException(INVALID_STATUS_TRANSITION)`이 skip 처리되는 RED 테스트를 추가한다.
5. RED 실패를 확인한다.
6. 신규 Flyway migration과 JPA unique constraint를 추가한다.
7. `AiGenerationJobRepository`에 promptVersion 무관 active target exists method를 추가하고 `AiService` 호출부를 변경한다.
8. `DataIntegrityViolationException` 매핑에 신규 unique key 이름을 포함한다.
9. 시딩 서비스에서 duplicate active job race만 skip 처리한다.
10. ERD 문서를 갱신한다.
11. 관련 테스트와 build를 실행한다.
12. report 문서를 작성한다.

## 테스트 계획

- `AiServiceTest`
  - promptVersion이 달라도 같은 active target의 `QUEUED/RUNNING` job이 있으면 생성 job 생성 차단.
  - promptVersion이 달라도 같은 active target의 `QUEUED/RUNNING` job이 있으면 관리자 재생성 차단.
  - 신규 unique key 충돌도 `INVALID_STATUS_TRANSITION`으로 매핑.
  - 무관한 integrity 오류는 그대로 전파.
- `AiGenerationJobRepositoryTest`
  - 다른 promptVersion이라도 같은 `jobType + targetType + targetId + activeUniqueKey=ACTIVE` job insert는 DB unique로 실패.
  - 기존 job이 `SUCCEEDED/FAILED`로 종료되어 `activeUniqueKey=NULL`이면 같은 target의 새 job 생성 허용.
- `AiDailyQtVerseExplanationSeedServiceTest`
  - duplicate active job `BusinessException(INVALID_STATUS_TRANSITION)`은 실패가 아니라 skip으로 처리되어 `failedCount=0`.
  - 그 외 `RuntimeException`은 기존처럼 `failedCount`로 집계.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AiServiceTest"
.\gradlew.bat test --tests "*AiGenerationJobRepositoryTest"
.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeedServiceTest"
.\gradlew.bat test --tests "*MigrationCoverageTest"
.\gradlew.bat test --tests "com.qtai.MysqlMigrationValidationTest"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai
```

`MysqlMigrationValidationTest`는 Docker가 없는 환경에서는 Testcontainers 설정에 따라 자동 skip될 수 있다.

## 수용 기준

- promptVersion이 달라도 같은 active target의 `QUEUED/RUNNING` job 중복 생성이 서비스와 DB에서 모두 차단된다.
- active job unique race는 일일 시딩 결과에서 실패가 아니라 skip으로 해석된다.
- 종료 job은 `active_unique_key=NULL`을 유지해 같은 target의 후속 생성이 가능하다.
- 신규 API/OpenAPI 변경이 없다.
- 관련 테스트와 build가 통과한다.

## Subagent Decision

Subagent 사용은 권장하지 않는다.

변경 범위가 `AiGenerationJob`, `AiService`, 일일 시딩 서비스, repository 테스트, migration에 강하게 연결되어 있고 RED-GREEN 순서를 한 맥락에서 유지해야 한다. 메인 agent가 직접 실행한다.
