# Workflow - 2026-06-02 ai-verse-explanation-generation-publish-flow-review

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-verse-explanation-generation-publish-flow-review` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-08, F-14 |
| 트리거 | AI 절별 해설 생성-게시 플로우 점검 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `25_기능_명세서.md` |
| workflow 경로 | `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-verse-explanation-generation-publish-flow-review.md` |
| report 경로 | `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-verse-explanation-generation-publish-flow-review_report.md` |

## 작업 목표

오늘 QT 절별 해설이 내부 시딩으로 job 생성되고, worker 생성/자동 검증/관리자 승인/게시를 거쳐 `verse_explanations` 사용자 노출본으로 연결되는 흐름을 코드와 테스트 기준으로 점검한다.

이번 작업은 점검 workflow/report 작성과 검증 명령 실행에 한정한다. 결함이나 테스트 gap이 발견되면 production/test 코드를 수정하지 않고 report의 Findings와 Follow-up에 기록한다.

## 범위

- `AiDailyQtVerseExplanationSeedService.seedToday()`의 누락 절 job 큐잉, skip 조건, no-op 조건, duplicate active job race 처리를 점검한다.
- `AiService.createAiGenerationJob(...)`의 command 검증, active prompt version 검증, active job 중복 차단, `QUEUED` 저장을 점검한다.
- `AiGenerationJobRunner`와 `ExplanationGenerationJobHandler`의 `QUEUED -> RUNNING -> SUCCEEDED/FAILED` 전이, LLM payload sanitization, 실패 시 asset 저장 여부를 점검한다.
- `AiAutoValidationService`의 JSON/schema/verse scope/forbidden field 검증과 `PASSED/REJECTED` validation log, asset 상태 영향을 점검한다.
- `AiAssetReviewService.approve(...)`와 `VerseExplanationService`의 latest validation log `PASSED` gate, 게시 전 payload/targetId 검증, ACTIVE 노출본 교체를 점검한다.
- 사용자 `study-content` 조회는 승인 ACTIVE 해설만 반환하고 생성 큐잉 부작용이 없는지 점검한다.

## 제외 범위

- production 코드 수정.
- 테스트 코드 추가 또는 수정.
- 신규 HTTP API, OpenAPI, DB schema/Flyway migration, Java UseCase 계약 변경.
- 사용자 조회 시점 AI job 큐잉.
- `QT_PASSAGE` 다중 절 게시 연결, SIMULATOR 게시 연결, glossary term 게시 연결 구현.
- 과거/미래 QT backfill 또는 전체 본문 순회 job 생성.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-verse-explanation-generation-publish-flow-review.md` | 점검 범위, 실행 순서, 검증 기준 고정 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-verse-explanation-generation-publish-flow-review_report.md` | 코드/테스트 점검 결과와 검증 명령 결과 기록 |
| Inspect | `qtai-server/src/main/java/com/qtai/domain/ai/internal/**` | 생성 큐잉, job 실행, payload 생성, 자동 검증, 승인 gate 점검 |
| Inspect | `qtai-server/src/main/java/com/qtai/domain/study/internal/**` | 승인 해설 게시와 사용자 조회 노출 조건 점검 |
| Inspect | `qtai-server/src/test/java/com/qtai/domain/ai/internal/**` | AI 생성-검증-승인 회귀 테스트 커버리지 점검 |
| Inspect | `qtai-server/src/test/java/com/qtai/domain/study/internal/**` | `verse_explanations` 게시/조회 회귀 테스트 커버리지 점검 |

## 구현 순서

1. workflow 문서를 저장한다.
2. workflow를 다시 읽고 `workflow-spec-runner` 기준으로 직접 실행 경로를 선택한다.
3. 관련 production 코드의 상태 전이와 guard 조건을 확인한다.
4. 관련 테스트 목록을 확인해 각 점검 시나리오가 어느 테스트로 고정되는지 매핑한다.
5. 지정된 Gradle 테스트와 build를 실행한다.
6. `git diff --check`와 도메인 경계 import 검색을 실행한다.
7. report 문서에 코드 점검 결과, 테스트 매핑, 검증 명령 결과, Findings, Follow-up을 기록한다.
8. workflow/report 외 repo-tracked 변경이 없는지 `git status --short`로 확인한다.

## 점검 시나리오

| 시나리오 | 판정 기준 |
| --- | --- |
| 누락 절만 job 생성 | 승인 ACTIVE 해설, ready asset, active job이 없는 verseId만 `EXPLANATION + BIBLE_VERSE` command로 큐잉 |
| no-op/failureReason | today QT 없음, 빈 verseIds는 no-op이고 active prompt 없음은 failureReason 기록 |
| 중복 active job | service 중복 차단과 repository unique race가 `INVALID_STATUS_TRANSITION`으로 처리되고 seed에서는 skip |
| LLM payload 검증 | invalid JSON, out-of-scope verseId, duplicate/missing explanation, blank text 차단 |
| 자동 검증 | JSON object, explanation schema, source verse scope, forbidden fields 기준으로 `PASSED/REJECTED` log 기록 |
| job 실패 안전성 | LLM/handler 실패 시 job은 `FAILED`, 유효 asset은 저장되지 않음 |
| 승인 gate | active checklist와 latest validation log `PASSED` 없이는 approve/publish 차단 |
| 게시 전 payload guard | `EXPLANATION + BIBLE_VERSE + activateForTarget=true`만 payload/targetId 검증 후 publish |
| 사용자 노출 | `VerseExplanationService`가 `APPROVED + ACTIVE` 해설만 반환하고 study-content는 read-only 유지 |

## 수용 기준

- [ ] workflow 문서가 요청 경로에 저장된다.
- [ ] report 문서가 요청 경로에 저장된다.
- [ ] 생성 큐잉, job 실행, payload 생성, 자동 검증, 승인 게시, 사용자 노출 흐름의 현재 구현 상태가 report에 정리된다.
- [ ] 각 점검 시나리오의 테스트 커버리지 또는 gap이 report에 기록된다.
- [ ] 지정 검증 명령 결과가 PASS/FAIL로 report에 기록된다.
- [ ] production/test 코드, API/OpenAPI/DB/UseCase 계약 변경이 없다.
- [ ] AI와 study 도메인 경계 import 위반이 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 이번 작업은 코드 구현이 아니라 단일 플로우 점검과 문서 기록이 중심이다.
- 점검 대상이 seed, job, worker, validation, approval, study publish로 이어지는 순차 흐름이라 한 agent가 같은 맥락에서 보는 편이 누락 가능성이 낮다.
- 병렬 worker가 독립적으로 편집할 production/test 파일이 없고, 최종 report의 판단 기준을 일관되게 유지해야 한다.

### 위임 가능 작업

| Worker | 해당 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow 저장, 코드/테스트 점검, 검증 명령 실행, report 작성, 최종 확인을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeedServiceTest"
.\gradlew.bat test --tests "*AiServiceTest"
.\gradlew.bat test --tests "*AiGenerationJobRunner*"
.\gradlew.bat test --tests "*ExplanationGenerationJobHandlerTest"
.\gradlew.bat test --tests "*AiAutoValidationServiceTest"
.\gradlew.bat test --tests "*AiAssetReview*"
.\gradlew.bat test --tests "*VerseExplanation*"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/domain/study
```

## 후속 작업으로 남길 항목

- 점검 중 발견한 결함 또는 테스트 gap은 별도 구현 workflow/PR 후보로 분리한다.
- `QT_PASSAGE` 다중 절 게시, SIMULATOR 게시, glossary term 게시 연결은 별도 정책 결정 후 진행한다.
