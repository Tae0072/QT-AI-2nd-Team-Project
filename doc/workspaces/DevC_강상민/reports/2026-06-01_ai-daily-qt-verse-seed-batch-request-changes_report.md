# Report - 2026-06-01 ai-daily-qt-verse-seed-batch-request-changes

## 작업 정보

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `feature/ai-daily-verse-seed-batch` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-daily-qt-verse-seed-batch-request-changes.md` |
| 관련 F-ID | F-02, F-14 |

## 실행 요약

REQUEST_CHANGES의 차단 항목을 코드와 테스트로 반영했다. 00:05 KST scheduler는 Lead 승인된 내부 사전 시딩 정책으로 유지하고, scheduler annotation을 테스트로 고정했다. verse별 generation job 생성은 best-effort로 처리해 특정 verse 실패가 나머지 eligible verse 생성을 막지 않도록 변경했다.

신규 API, OpenAPI, DB schema 변경은 없다.

## 변경 내용

- `AiDailyQtVerseExplanationSeedResult` record를 추가해 `createdCount`, `failedCount`를 반환한다.
- `AiDailyQtVerseExplanationSeedService.seedToday()`가 verse별 job 생성 실패 시 `RuntimeException`만 흡수하고 다음 verse 생성을 계속한다.
- 실패 verse 로그에는 `verseId`, `errorType`, `errorMessage`를 남긴다.
- scheduler 완료 로그에 `createdCount`, `failedCount`를 함께 남긴다.
- scheduler cron/zone reflection 테스트를 추가해 `0 5 0 * * *`, `Asia/Seoul`을 고정했다.
- 기존 workflow/report에는 00:05 Lead 승인 정책, 04:00 사용자 노출/cache refresh 해석, SSoT 표현 정리 후속 PR을 명시했다.

## TDD 기록

1. service/scheduler 테스트를 먼저 수정해 `AiDailyQtVerseExplanationSeedResult`와 partial failure 동작을 요구하도록 만들었다.
2. `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeed*"`에서 `AiDailyQtVerseExplanationSeedResult` 미구현 컴파일 실패로 RED를 확인했다.
3. result record, best-effort loop, scheduler result 로그를 구현했다.
4. seed/scheduler 테스트를 다시 실행해 통과를 확인했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeed*"` | 성공 |
| `.\gradlew.bat test --tests "*AiGenerationJobRepositoryTest"` | 성공 |
| `.\gradlew.bat test --tests "*AiGeneratedAssetRepositoryTest"` | 성공 |
| `.\gradlew.bat test --tests "*AiGenerationJob*"` | 성공 |
| `.\gradlew.bat build` | 성공 |
| `git diff --check` | 성공. CRLF 변환 warning만 출력 |
| `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | 매칭 없음. 추가 금지 import 없음 |

Unix/CI 환경에서는 `qtai-server` 기준 `./gradlew`로 같은 Gradle task를 실행한다.

## 수용 기준 확인

- 00:05 KST scheduler 정책 테스트 고정: 충족.
- verse별 job 생성 실패 best-effort 처리: 충족.
- 실패 verse 로그와 `failedCount` 집계: 충족.
- 기존 skip 기준 유지: 충족.
- 신규 API/OpenAPI/DB schema 변경 없음: 충족.

## 후속 작업

- SSoT 문서의 00:05 내부 사전 시딩과 04:00 사용자 노출/cache refresh 표현 정리는 별도 문서 PR로 남긴다.
- schedlock 또는 DB unique constraint 기반 중복 실행 race 보강은 별도 PR로 남긴다.
- batch 실패 알림/모니터링 연동은 운영 정책 확정 후 별도 PR로 남긴다.
- `SYSTEM_BATCH` 공통 Actor enum 추출은 별도 리팩터링 PR로 남긴다.
