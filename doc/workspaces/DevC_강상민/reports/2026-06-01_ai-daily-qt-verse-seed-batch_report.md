# Report - 2026-06-01 ai-daily-qt-verse-seed-batch

## 작업 정보

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `feature/ai-daily-verse-seed-batch` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-daily-qt-verse-seed-batch.md` |
| 관련 F-ID | F-02, F-14 |

## 실행 요약

매일 00:05 KST에 오늘 QT passage의 verse ids를 기준으로 절 단위 `EXPLANATION` generation job을 시딩하는 scheduler와 service를 추가했다. 생성된 `QUEUED` job은 기존 `AiGenerationJobWorker`가 처리한다.

신규 HTTP API, OpenAPI, DB schema 변경은 없다. SIMULATOR와 QT_PASSAGE 단위 generation은 이번 범위에서 제외했다.

## 변경 내용

- `AiDailyQtVerseExplanationSeedScheduler` 추가
  - cron: `0 5 0 * * *`
  - zone: `Asia/Seoul`
  - toggle: `ai.daily-qt-verse-seed.enabled:true`
  - service 예외는 scheduler 밖으로 전파하지 않고 warn 로그로 남긴다.
- `AiDailyQtVerseExplanationSeedService` 추가
  - `GetTodayQtUseCase.getToday(null)`로 오늘 passage id를 조회한다.
  - `GetQtPassageContentContextUseCase.getContentContext(...)`로 verse ids를 조회한다.
  - verse ids는 입력 순서 유지 + 중복 제거로 처리한다.
  - 최신 active EXPLANATION prompt version을 사용한다.
  - approved explanation, VALIDATING/APPROVED asset, QUEUED/RUNNING job이 있는 verse는 skip한다.
  - eligible verse만 `SYSTEM_BATCH` 주체로 `EXPLANATION + BIBLE_VERSE` job을 만든다.
- repository query 보강
  - active prompt version 조회.
  - asset skip target id 조회.
  - active job skip target id 조회.

## TDD 기록

1. `AiDailyQtVerseExplanationSeedServiceTest`, `AiDailyQtVerseExplanationSeedSchedulerTest`, repository query 테스트를 먼저 추가했다.
2. `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeed*"`에서 `AiDailyQtVerseExplanationSeedService` 미구현 컴파일 실패로 RED를 확인했다.
3. repository query, seeding service, scheduler를 최소 구현했다.
4. seed/scheduler 테스트와 repository query 테스트를 다시 실행해 통과를 확인했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeed*"` | 성공 |
| `.\gradlew.bat test --tests "*AiGenerationJobRepositoryTest"` | 성공 |
| `.\gradlew.bat test --tests "*AiGeneratedAssetRepositoryTest"` | 성공 |
| `.\gradlew.bat test --tests "*AiGenerationJob*"` | 성공 |
| `.\gradlew.bat build` | 성공 |
| `git diff --check` | 성공. CRLF 변환 warning만 출력 |
| `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | 추가 금지 import 없음 |

## 수용 기준 확인

- 00:05 KST scheduler 추가: 충족.
- `ai.daily-qt-verse-seed.enabled` toggle: 충족.
- 오늘 QT passage verse ids 순서 유지 중복 제거: 충족.
- approved active verse explanation skip: 충족.
- VALIDATING/APPROVED EXPLANATION asset skip: 충족.
- QUEUED/RUNNING EXPLANATION job skip: 충족.
- eligible verse만 `SYSTEM_BATCH`, `EXPLANATION`, `BIBLE_VERSE` job 생성: 충족.
- 최신 active EXPLANATION prompt version 사용: 충족.
- 신규 API/OpenAPI/DB schema 변경 없음: 충족.
- SIMULATOR/QT_PASSAGE 단위 generation 제외: 충족.

## 후속 작업

- 생성 job 처리 후 `verse_explanations` 승인본 반영 흐름은 별도 PR로 둔다.
- SIMULATOR 일일 시딩 정책은 별도 결정이 필요하다.
- 운영 모니터링/알림은 배치 운영 정책 확정 후 추가한다.
