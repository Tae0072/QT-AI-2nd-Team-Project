# Workflow - 2026-06-01 ai-daily-qt-verse-seed-batch-request-changes

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-daily-verse-seed-batch` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | `ai-daily-qt-verse-seed-batch` REQUEST_CHANGES 대응 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-daily-qt-verse-seed-batch.md` |
| 해당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

코드 리뷰의 REQUEST_CHANGES를 닫기 위해 00:05 KST scheduler 정책을 테스트로 고정하고, verse별 job 생성 실패를 best-effort로 처리한다. 00:05는 Lead 승인된 내부 사전 시딩 배치로 유지하며, 기존 04:00 KST 정책은 Today QT 사용자 노출/cache refresh 기준으로 해석한다.

## 범위

- `AiDailyQtVerseExplanationSeedService.seedToday()` 반환값을 `createdCount`, `failedCount`를 포함하는 내부 result 타입으로 변경한다.
- `createAiGenerationJobUseCase.createAiGenerationJob(...)` 호출은 verse 단위 best-effort로 처리한다.
  - 특정 verse 실패 시 `RuntimeException`만 흡수한다.
  - 실패한 `verseId`, `errorType`, `errorMessage`를 warn 로그로 남긴다.
  - 나머지 eligible verse 생성을 계속한다.
- `AiDailyQtVerseExplanationSeedScheduler` 완료 로그에 `createdCount`, `failedCount`를 함께 남긴다.
- `@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")` 값을 reflection 테스트로 고정한다.
- workflow/report에는 "00:05는 Lead 승인 내부 사전 시딩, 04:00은 사용자 노출/cache refresh 기준, SSoT 표현 정리는 별도 문서 PR"이라고 명시한다.

## 제외 범위

- 00:05 KST scheduler 시간 변경 없음.
- 중앙 SSoT 문서 직접 수정 없음.
- schedlock, DB unique index, retry/backoff, 운영 알림 구현 없음.
- `SYSTEM_BATCH` 공통 Actor enum 추출 없음.
- 신규 API, OpenAPI, DB schema 변경 없음.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-daily-qt-verse-seed-batch-request-changes.md` | REQUEST_CHANGES 대응 workflow |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedService.java` | best-effort 생성, failedCount 집계, 실패 로그 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedResult.java` | `createdCount`, `failedCount` 결과 record |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedScheduler.java` | 완료 로그 result 기반 변경 |
| Modify Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedServiceTest.java` | result 반환과 partial failure 테스트 |
| Modify Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedSchedulerTest.java` | cron/zone reflection 테스트와 완료 로그 갱신 |
| Modify | 기존 daily seed workflow/report 문서 | 00:05 Lead 승인 정책과 SSoT 후속 PR 문구 보강 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-01_ai-daily-qt-verse-seed-batch-request-changes_report.md` | 구현/검증 결과 기록 |

## 구현 순서

1. 이 workflow 문서를 저장한다.
2. `AiDailyQtVerseExplanationSeedServiceTest`를 먼저 수정해 `AiDailyQtVerseExplanationSeedResult` 기대값과 partial failure RED를 만든다.
3. `AiDailyQtVerseExplanationSeedSchedulerTest`에 cron/zone reflection 테스트와 result 로그 기대값을 추가한다.
4. `AiDailyQtVerseExplanationSeedResult` record를 추가한다.
5. service loop를 verse 단위 best-effort로 변경한다.
6. scheduler 완료 로그를 `createdCount`, `failedCount` 기반으로 변경한다.
7. workflow/report 문서에 00:05 Lead 승인 정책과 SSoT 후속 PR 문구를 보강한다.
8. 검증 명령을 실행하고 request-changes report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가/변경 검증 |
| --- | --- |
| `AiDailyQtVerseExplanationSeedServiceTest` | eligible verse 생성 시 `createdCount=1`, `failedCount=0` |
| `AiDailyQtVerseExplanationSeedServiceTest` | 일부 verse 생성 실패 시 나머지 verse를 계속 생성하고 `createdCount=1`, `failedCount=1` |
| `AiDailyQtVerseExplanationSeedServiceTest` | 실패 로그에 `verseId`, `errorType`, `errorMessage` 포함 |
| `AiDailyQtVerseExplanationSeedServiceTest` | 빈 verse, prompt 미존재, invalid id, null 응답 기존 검증 유지 |
| `AiDailyQtVerseExplanationSeedSchedulerTest` | 완료 로그에 `createdCount`, `failedCount` 포함 |
| `AiDailyQtVerseExplanationSeedSchedulerTest` | `@Scheduled` cron `0 5 0 * * *`, zone `Asia/Seoul` 고정 |

## 수용 기준

- [ ] 00:05 KST scheduler 정책이 테스트로 고정된다.
- [ ] verse별 job 생성 실패가 batch 전체 중단으로 이어지지 않는다.
- [ ] 실패 verse 로그와 `failedCount` 집계가 남는다.
- [ ] 기존 skip 기준과 신규 API/OpenAPI/DB schema 없음 조건이 유지된다.
- [ ] PR 본문에 workflow/report 경로, 00:05 Lead 승인 정책, SSoT 후속 문서 PR 필요성이 정리될 수 있다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- service 반환 타입 변경이 scheduler와 테스트에 직접 연결되어 있어 순차 수정이 안전하다.
- REQUEST_CHANGES의 핵심은 정책 결정을 코드와 문서에 일관되게 반영하는 작업이다.
- 병렬 작업보다 한 agent가 RED, 구현, 문서, 검증을 같은 맥락에서 처리하는 편이 충돌 가능성이 낮다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow 저장, 테스트 보강, 구현, 문서 보강, 검증, report 작성을 직접 수행한다.

## 검증 계획

- `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeed*"`
- `.\gradlew.bat test --tests "*AiGenerationJobRepositoryTest"`
- `.\gradlew.bat test --tests "*AiGeneratedAssetRepositoryTest"`
- `.\gradlew.bat test --tests "*AiGenerationJob*"`
- `.\gradlew.bat build`
- `git diff --check`
- `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai`

Unix/CI 환경에서는 `qtai-server` 기준 `./gradlew`로 같은 Gradle task를 실행한다.

## 후속 작업으로 남길 항목

- SSoT 문서의 00:05 내부 사전 시딩과 04:00 사용자 노출/cache refresh 표현 정리.
- schedlock 또는 DB unique constraint 기반 중복 실행 race 보강.
- batch 실패 알림/모니터링 연동.
- `SYSTEM_BATCH` 공통 Actor enum 추출.
