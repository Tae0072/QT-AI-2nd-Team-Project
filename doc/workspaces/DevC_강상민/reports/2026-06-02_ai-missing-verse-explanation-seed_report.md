# Report - 2026-06-02 ai-missing-verse-explanation-seed

## Summary

- 브랜치: `feature/ai-missing-verse-explanation-seed`
- PR 대상: `dev`
- 관련 F-ID: F-02, F-08, F-14
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-missing-verse-explanation-seed.md`
- 목표: 오늘 QT 본문에 승인 ACTIVE 절별 해설이 없는 절만 내부 시딩 흐름에서 `EXPLANATION + BIBLE_VERSE` AI generation job으로 큐잉한다.

## Changes

- `AiDailyQtVerseExplanationSeedService.seedToday()`에서 `todayQt.qtPassageId()`가 null이면 예외 대신 `createdCount=0`, `failedCount=0`으로 no-op 반환하도록 변경했다.
- 기존 canonical seed 흐름은 유지했다.
  - 승인 ACTIVE 해설이 있는 절은 skip
  - `VALIDATING/APPROVED` AI asset이 있는 절은 skip
  - `QUEUED/RUNNING` active job이 있는 절은 skip
  - 남은 절만 `CreateAiGenerationJobCommand(EXPLANATION, BIBLE_VERSE, verseId, promptVersionId, SYSTEM_BATCH, requestedAt)`로 큐잉
- 실제 생성 대상 verseId가 있을 때만 latest active `EXPLANATION` prompt version을 조회하도록 순서를 고정했다.
- `QtStudyContentService` production 코드는 변경하지 않았다. study-content 조회는 승인 해설만 반환하는 read-only 계약을 유지한다.
- 신규 HTTP API, OpenAPI, DB schema/Flyway migration, `study.api` 계약 변경은 없다.

## Tests Added

- `AiDailyQtVerseExplanationSeedServiceTest`
  - `todayQt.qtPassageId()`가 null이면 content context, prompt version, asset/job repository, job 생성 use case를 호출하지 않고 no-op 결과를 반환하는 케이스를 추가했다.
  - 오늘 QT의 모든 verseId에 승인 해설이 있으면 prompt version 조회와 job 생성 없이 `createdCount=0`, `failedCount=0`을 반환하는 케이스를 추가했다.
  - null passage는 MISS/EMPTY no-op 정책으로 분리하고, invalid id 회귀는 `0`, `-1`로 유지했다.
- `QtStudyContentServiceTest`
  - 일부 절에 해설이 없어도 승인된 해설만 응답에 포함하고, 조회 경로가 생성 큐잉 부작용 없이 read-only로 동작하는 회귀 테스트를 추가했다.

## Verification

| Command | Result |
| --- | --- |
| `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeedServiceTest"` | PASS |
| `.\gradlew.bat test --tests "*QtStudyContentServiceTest"` | PASS |
| `.\gradlew.bat test --tests "*AiGenerationJob*"` | PASS |
| `.\gradlew.bat test --tests "*AiAssetReview*"` | PASS |
| `.\gradlew.bat build` | PASS |
| `git diff --check` | PASS |
| `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/domain/study` | PASS - forbidden import 없음 |

## Acceptance

- [x] 오늘 QT가 없거나 `qtPassageId`가 null이면 AI generation job을 만들지 않고 no-op 결과를 반환한다.
- [x] 오늘 QT verseIds 중 승인 ACTIVE 해설이 없는 절만 큐잉 대상이 된다.
- [x] 승인 해설, ready asset, active job이 있는 절은 큐잉하지 않는다.
- [x] study-content 조회 경로는 AI 큐잉, pending 상태, placeholder, job id 노출 없이 read-only로 유지된다.
- [x] API/OpenAPI/DB 변경이 없다.
- [x] AI와 study 도메인의 internal/web/repository 직접 import가 새로 발생하지 않았다.

## Notes

- `AiDailyQtVerseExplanationSeedServiceTest`에서 `todayQt.qtPassageId() == null` 케이스를 먼저 추가해 RED를 확인했고, 이후 no-op guard를 추가해 GREEN을 확인했다.
- 생성된 job은 기존 worker, validation, admin approval 흐름을 거친 뒤에만 `verse_explanations` 사용자 노출본으로 연결된다.
