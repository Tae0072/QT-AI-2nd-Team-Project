# Report - 2026-06-02 ai-daily-qt-verse-seed-race-guard

## 작업 정보

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `feature/ai-daily-verse-seed-race-guard` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-daily-qt-verse-seed-race-guard.md` |
| 관련 F-ID | F-02, F-14 |

## 실행 요약

일일 QT 절 단위 `EXPLANATION + BIBLE_VERSE` 시딩에서 같은 target의 active generation job이 중복 생성되는 race를 DB와 서비스 양쪽에서 보강했다.

신규 HTTP API, OpenAPI, worker 처리 흐름, scheduler 시간 변경은 없다.

## 변경 내용

- `ai_generation_jobs(job_type, target_type, target_id, active_unique_key)` unique key를 추가했다.
- 기존 `job_type + target_type + target_id + prompt_version_id + active_unique_key` unique key는 유지했다.
- `AiService`의 active job 중복 조회 기준을 promptVersion 무관 target 기준으로 변경했다.
- 기존 unique key와 신규 unique key 충돌만 `INVALID_STATUS_TRANSITION`으로 매핑하고, 무관한 DB 오류는 그대로 전파하도록 유지했다.
- 일일 QT 절 시딩에서 `BusinessException(ErrorCode.INVALID_STATUS_TRANSITION)`은 정상 경합 skip으로 처리하고 `failedCount`를 증가시키지 않도록 변경했다.
- `qtai-server/02_ERD_문서.md`에 `ai_generation_jobs.active_unique_key`와 신규 unique key 정책을 반영했다.

## TDD 기록

1. `AiServiceTest`에 promptVersion이 달라도 같은 active target이면 생성/재생성이 차단되는 테스트를 먼저 추가했다.
2. `.\gradlew.bat test --tests "*AiServiceTest"`에서 신규 repository method 부재 컴파일 실패로 RED를 확인했다.
3. `AiGenerationJobRepositoryTest`에 active target unique constraint와 종료 job 후 후속 생성 허용 테스트를 추가했다.
4. `AiDailyQtVerseExplanationSeedServiceTest`에 duplicate active job race skip 테스트를 추가했다.
5. repository method, JPA unique constraint, Flyway migration, service 중복 기준, 시딩 skip 처리를 구현했다.
6. 관련 테스트를 다시 실행해 GREEN을 확인했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AiServiceTest"` | 성공 |
| `.\gradlew.bat test --tests "*AiGenerationJobRepositoryTest"` | 성공 |
| `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeedServiceTest"` | 성공 |
| `.\gradlew.bat test --tests "*MigrationCoverageTest"` | 성공 |
| `.\gradlew.bat test --tests "com.qtai.MysqlMigrationValidationTest"` | Gradle 성공. Docker 부재로 테스트 1건 skip |
| `.\gradlew.bat build` | 성공 |
| `git diff --check` | 성공. CRLF 변환 warning만 출력 |
| `rg -n "^import .*domain\.[a-z]+\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | 매칭 없음 |

## 수용 기준 확인

- promptVersion이 달라도 같은 active target의 `QUEUED/RUNNING` job 중복 생성 차단: 충족.
- DB unique key 기반 race 방어: 충족.
- active job unique race를 일일 시딩 실패가 아닌 skip으로 처리: 충족.
- 종료 job은 `active_unique_key=NULL`이라 같은 target 후속 생성 허용: 충족.
- 신규 API/OpenAPI 변경 없음: 충족.

## 후속 작업

- 운영 배포 전 이미 존재하는 `QUEUED/RUNNING` 중복 active target row가 없는지 확인한다.
- schedlock, 운영 알림, retry/backoff, batch 모니터링은 별도 PR로 남긴다.
