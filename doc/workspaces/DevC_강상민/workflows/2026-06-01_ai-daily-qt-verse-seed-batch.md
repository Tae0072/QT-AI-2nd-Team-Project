# Workflow - 2026-06-01 ai-daily-qt-verse-seed-batch

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-daily-verse-seed-batch` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | 오늘 QT passage 기준 절 단위 EXPLANATION generation job을 00:05 KST에 자동 시딩 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-worker-execution-policy.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-20_commentary-validation-flow-policy.md` |
| 해당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

매일 00:05 KST에 오늘 QT passage를 조회하고, passage의 `bibleVerseIds`를 절 단위로 펼쳐 필요한 verse에만 `SYSTEM_BATCH` 주체의 `EXPLANATION` generation job을 생성한다. 생성된 `QUEUED` job은 기존 `AiGenerationJobWorker`가 처리한다.

이번 작업은 job 시딩 배치만 추가한다. 신규 HTTP API, OpenAPI, DB schema 변경은 하지 않는다.

## 범위

- `domain.ai.internal`에 00:05 KST scheduler와 seeding service를 추가한다.
- scheduler cron은 `0 5 0 * * *`, zone은 `Asia/Seoul`로 고정한다.
- `ai.daily-qt-verse-seed.enabled:true` 설정으로 scheduler 실행 여부를 제어한다.
- 오늘 passage 조회는 `GetTodayQtUseCase.getToday(null)`와 `GetQtPassageContentContextUseCase.getContentContext(qtPassageId)`를 사용한다.
- prompt version은 `EXPLANATION + ACTIVE` 중 `createdAt desc, id desc` 최신 1건을 사용한다.
- skip 기준은 verse 단위로 적용한다.
  - approved active `verse_explanations`가 있으면 skip
  - `ai_generated_assets`에 `EXPLANATION + BIBLE_VERSE + VALIDATING/APPROVED` asset이 있으면 skip
  - `ai_generation_jobs`에 `EXPLANATION + BIBLE_VERSE + QUEUED/RUNNING` job이 있으면 skip
- 생성 대상 verse는 기존 `CreateAiGenerationJobUseCase`로 `SYSTEM_BATCH` 요청을 만든다.

## 제외 범위

- 신규 API, OpenAPI, DB migration.
- SIMULATOR generation job 시딩.
- QT_PASSAGE 단위 generation job 시딩.
- 사용자 요청 기반 즉시 생성.
- 기존 worker 실행 정책 변경.
- raw prompt, provider raw response, validation reference 원문, secret/token/password 저장.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedService.java` | 오늘 QT verse별 skip 판단과 generation job 생성 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedScheduler.java` | 00:05 KST scheduled trigger와 enabled toggle |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiPromptVersionRepository.java` | 최신 active EXPLANATION prompt 조회 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGeneratedAssetRepository.java` | 검수 대기/승인 EXPLANATION asset target id 조회 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRepository.java` | 진행 중 EXPLANATION job target id 조회 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedServiceTest.java` | seeding service 단위 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedSchedulerTest.java` | scheduler enabled/disabled/예외 흡수 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGeneratedAssetRepositoryTest.java` | asset skip query 검증 |
| Modify Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRepositoryTest.java` | active job skip query 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-01_ai-daily-qt-verse-seed-batch_report.md` | 구현/검증 결과 기록 |

## 구현 순서

1. `AiDailyQtVerseExplanationSeedServiceTest`를 먼저 추가한다.
   - verse ids `[101, 102, 103, 104, 104]`에서 approved explanation, VALIDATING/APPROVED asset, QUEUED/RUNNING job이 있는 verse를 제외하고 eligible verse만 job 생성되는지 검증한다.
   - 생성 command가 `SYSTEM_BATCH`, `EXPLANATION`, `BIBLE_VERSE`, 최신 active promptVersionId, clock 기반 requestedAt을 사용하는지 검증한다.
   - active EXPLANATION prompt가 없으면 job을 생성하지 않는지 검증한다.
2. `AiGeneratedAssetRepositoryTest`를 추가하고 `AiGenerationJobRepositoryTest`를 보강해 skip target id 조회가 상태와 target type을 정확히 필터링하는지 검증한다.
3. `AiDailyQtVerseExplanationSeedSchedulerTest`를 추가해 enabled면 service를 호출하고, disabled면 호출하지 않으며, service 예외는 scheduler 밖으로 전파하지 않는지 검증한다.
4. 위 테스트들이 컴파일 또는 실패하는 것을 확인한다.
5. repository query method를 추가한다.
6. `AiDailyQtVerseExplanationSeedService`를 구현한다.
7. `AiDailyQtVerseExplanationSeedScheduler`를 구현한다.
8. 관련 테스트와 전체 build를 실행한다.
9. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiDailyQtVerseExplanationSeedServiceTest` | 오늘 QT verse 중 eligible verse만 `CreateAiGenerationJobUseCase`로 생성 |
| `AiDailyQtVerseExplanationSeedServiceTest` | 중복 verse id는 한 번만 처리 |
| `AiDailyQtVerseExplanationSeedServiceTest` | 최신 active EXPLANATION prompt version 선택 |
| `AiDailyQtVerseExplanationSeedServiceTest` | active EXPLANATION prompt version이 없으면 job 생성 없음 |
| `AiDailyQtVerseExplanationSeedSchedulerTest` | enabled/disabled와 예외 흡수 |
| `AiGeneratedAssetRepositoryTest` | `EXPLANATION + BIBLE_VERSE + VALIDATING/APPROVED` asset target id만 조회 |
| `AiGenerationJobRepositoryTest` | `EXPLANATION + BIBLE_VERSE + QUEUED/RUNNING` job target id만 조회 |

## 수용 기준

- [ ] 00:05 KST scheduler가 추가된다.
- [ ] scheduler는 `ai.daily-qt-verse-seed.enabled`로 끌 수 있다.
- [ ] 오늘 QT passage의 verse ids를 순서 유지 중복 제거로 처리한다.
- [ ] approved active verse explanation이 있는 verse는 skip한다.
- [ ] 검수 대기 또는 승인된 EXPLANATION asset이 있는 verse는 skip한다.
- [ ] QUEUED/RUNNING EXPLANATION job이 있는 verse는 skip한다.
- [ ] eligible verse만 `SYSTEM_BATCH` 주체로 `EXPLANATION + BIBLE_VERSE` job을 만든다.
- [ ] 최신 active EXPLANATION prompt version을 사용한다.
- [ ] 신규 API, OpenAPI, DB schema 변경이 없다.
- [ ] SIMULATOR와 QT_PASSAGE 단위 generation은 포함하지 않는다.

## 시간 정책 정합성

- 00:05 KST scheduler는 오늘 QT passage가 준비된 직후 내부 `EXPLANATION` generation job을 미리 시딩하는 배치다.
- 기존 문서의 04:00 KST 정책은 사용자 노출/cache refresh 기준이며, 이번 00:05 내부 시딩 배치와 역할이 다르다.
- 이번 workflow에서는 중앙 요구사항 문서를 수정하지 않고, PR 문서와 report에 두 시간 정책의 관계를 명시한다.

## REQUEST_CHANGES 반영 정책

- 00:05 KST scheduler는 Lead 승인된 내부 사전 시딩 정책으로 유지한다.
- 기존 04:00 KST 정책은 Today QT 사용자 노출/cache refresh 기준으로 해석한다.
- SSoT 문서의 시간 정책 표현 정리는 별도 문서 갱신 PR로 남긴다.
- verse별 job 생성은 best-effort로 처리한다. 특정 verse 생성 실패는 `failedCount`로 집계하고, 나머지 eligible verse 생성을 계속한다.
- 실패 로그에는 `verseId`, `errorType`, `errorMessage`를 남긴다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- scheduler, service, repository query, 테스트 fixture가 같은 AI generation 흐름에 묶여 있다.
- 테스트를 먼저 실패시키고 그 실패를 기준으로 최소 구현을 맞춰야 하므로 한 맥락에서 순차 실행하는 편이 안전하다.
- worker edit path가 모두 `domain.ai.internal`에 집중되어 병렬 편집 충돌 가능성이 있다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow 작성, TDD 테스트 추가, 구현, 검증, report 작성을 직접 수행한다.

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

- 생성 job 처리 후 `verse_explanations` 승인본 반영 흐름.
- SIMULATOR 일일 시딩 정책.
- 00:05 배치 운영 모니터링/알림.
