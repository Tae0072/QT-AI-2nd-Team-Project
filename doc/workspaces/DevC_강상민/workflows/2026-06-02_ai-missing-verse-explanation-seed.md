# Workflow - 2026-06-02 ai-missing-verse-explanation-seed

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-missing-verse-explanation-seed` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-08, F-14 |
| 트리거 | 오늘 QT 절별 승인 해설 누락 시 내부 AI generation job 보강 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `25_기능_명세서.md` |
| workflow 경로 | `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-missing-verse-explanation-seed.md` |
| report 경로 | `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-missing-verse-explanation-seed_report.md` |

## 작업 목표

오늘 QT 본문에 표시할 절별 승인 해설이 없는 경우, 사용자 조회 경로가 아니라 내부 시딩 흐름에서 `EXPLANATION + BIBLE_VERSE` AI generation job을 큐잉한다.

기존 `AiDailyQtVerseExplanationSeedService.seedToday()`를 canonical flow로 고정하고, 오늘 QT가 없거나 cache miss인 경우에는 예외 대신 no-op 결과를 반환해 배치가 안정적으로 종료되도록 한다.

## 범위

- `AiDailyQtVerseExplanationSeedService.seedToday()`에서 오늘 QT의 verseIds를 기준으로 누락 절만 큐잉한다.
- 승인 ACTIVE 해설이 있는 절, `VALIDATING/APPROVED` asset이 있는 절, `QUEUED/RUNNING` job이 있는 절은 큐잉하지 않는다.
- `todayQt.qtPassageId()`가 null이면 `createdCount=0`, `failedCount=0`으로 반환하고 job을 만들지 않는다.
- `GET /api/v1/qt/{qtPassageId}/study-content`는 승인된 `verse_explanations`만 반환하는 read-only 계약을 유지한다.
- 기존 `SYSTEM_BATCH` 요청자, latest active `EXPLANATION` prompt version, active job race skip 정책을 유지한다.

## 제외 범위

- 사용자 조회 시점 AI job 큐잉.
- 과거/미래 QT 또는 게시 본문 전체 backfill.
- study-content 응답에 placeholder, pending 상태, job id 노출.
- 신규 HTTP API, OpenAPI, DB schema/Flyway migration 변경.
- worker, validation, admin approval 흐름 변경.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedService.java` | 오늘 QT 없음 no-op 처리와 누락 절 큐잉 흐름 고정 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedServiceTest.java` | 누락 절 큐잉, 전체 승인 해설 보유, today 없음 no-op 회귀 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/study/internal/QtStudyContentServiceTest.java` | study-content read-only 계약 회귀 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-missing-verse-explanation-seed_report.md` | 구현 내용과 검증 결과 기록 |

## 구현 순서

1. workflow 문서를 저장한다.
2. `AiDailyQtVerseExplanationSeedServiceTest`에 `todayQt.qtPassageId()`가 null이면 job 생성 없이 no-op 결과를 반환하는 RED 테스트를 추가한다.
3. `AiDailyQtVerseExplanationSeedServiceTest`에 모든 verseIds가 승인 해설을 갖고 있으면 job 생성 없이 no-op 결과를 반환하는 회귀 테스트를 추가한다.
4. `QtStudyContentServiceTest`에 해설이 일부 누락돼도 조회가 AI use case를 호출하지 않는 read-only 계약 테스트를 추가한다. 기존 서비스 의존성 구조상 AI use case가 없음을 확인하는 방식으로 검증한다.
5. RED 실패를 확인한다.
6. `AiDailyQtVerseExplanationSeedService.seedToday()`에서 `todayQt.qtPassageId()` null을 먼저 감지해 no-op 결과를 반환한다.
7. 누락 절 큐잉 기존 흐름이 승인 해설, ready asset, active job skip 기준을 유지하는지 테스트로 확인한다.
8. report 문서를 작성한다.
9. 지정 검증 명령을 실행한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiDailyQtVerseExplanationSeedServiceTest` | `todayQt.qtPassageId()`가 null이면 prompt/version/repository/job 생성 호출 없이 `createdCount=0`, `failedCount=0` |
| `AiDailyQtVerseExplanationSeedServiceTest` | 모든 절에 승인 ACTIVE 해설이 있으면 job 생성 호출 없이 `createdCount=0`, `failedCount=0` |
| `AiDailyQtVerseExplanationSeedServiceTest` | 누락 절만 `CreateAiGenerationJobCommand(EXPLANATION, BIBLE_VERSE, verseId, promptVersionId, SYSTEM_BATCH, requestedAt)`로 큐잉되는 기존 동작 유지 |
| `QtStudyContentServiceTest` | 해설이 없는 절이 있어도 승인 해설만 반환하고 AI 큐잉 의존성을 추가하지 않는 read-only 계약 유지 |

## 수용 기준

- [ ] 오늘 QT가 없으면 AI generation job을 만들지 않고 no-op 결과를 반환한다.
- [ ] 오늘 QT verseIds 중 승인 ACTIVE 해설이 없는 절만 큐잉 대상이 된다.
- [ ] 승인 해설, ready asset, active job이 있는 절은 큐잉하지 않는다.
- [ ] study-content 조회 경로에는 AI 큐잉 부작용이 없다.
- [ ] 신규 API/OpenAPI/DB 변경이 없다.
- [ ] AI 도메인은 다른 도메인의 internal/web/repository 타입을 직접 import하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `AiDailyQtVerseExplanationSeedService`와 인접 테스트에 집중되어 있다.
- 테스트와 구현이 같은 흐름을 순차적으로 확인해야 하므로 직접 실행이 충돌과 재작업을 줄인다.
- 문서와 report는 구현 결과에 맞춰 마지막에 정리하는 편이 안전하다.

### 위임 가능 작업

| Worker | 담당 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 테스트 보강, 구현, report 작성, 최종 검증을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeedServiceTest"
.\gradlew.bat test --tests "*QtStudyContentServiceTest"
.\gradlew.bat test --tests "*AiGenerationJob*"
.\gradlew.bat test --tests "*AiAssetReview*"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/domain/study
```

## 후속 작업으로 남긴 항목

- 과거/미래 QT 또는 게시 본문 전체 backfill은 별도 정책과 큐 폭증 제어가 필요하므로 이번 범위에서 제외한다.
- study-content 응답에 pending 상태를 노출하는 UX는 별도 API 계약 변경이 필요하므로 이번 범위에서 제외한다.
