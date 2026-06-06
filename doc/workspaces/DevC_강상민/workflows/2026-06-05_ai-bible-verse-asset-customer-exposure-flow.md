# Workflow - 2026-06-05 ai-bible-verse-asset-customer-exposure-flow

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `test/ai-bible-verse-asset-customer-exposure-flow` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | 오늘 QT 범위에서 BIBLE_VERSE 산출물을 실제 생성하고, 검수 통과 시 고객 노출 상태까지 전환되는지 확인해야 한다. |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-layer2-real-index-sample-run.md` |
| 대상 경로 | `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/workflows/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

오늘 QT `1CO 3:1-15`의 기존 `bible_verses`를 `qt_passage_verses`에 연결하고, 실제 DeepSeek 기반 BIBLE_VERSE `EXPLANATION` asset 생성부터 layer 1/2 검수, 승인, `verse_explanations` 고객 노출 row 저장까지 검증한다.

DeepSeek가 `PASSED`까지 도달하지 못하면 검수 계약을 우회하지 않고 고객 노출 전환을 하지 않는다. 이 경우 report에는 실패 지점과 고객 노출 산출물 미생성 상태를 기록한다.

## 범위

- `AiBibleVerseAssetCustomerExposureFlowManualTest`를 추가한다.
- 기본 실행은 비활성화하고 `QTAI_AI_CUSTOMER_EXPOSURE_SAMPLE=true`일 때만 실행한다.
- dev MySQL, 실제 restricted index, 실제 `DeepSeekLlmClient`를 사용한다.
- 누락된 dev DB precondition을 idempotent하게 생성한다.
- 성공 시 `ReviewAiAssetUseCase.reviewAiAsset(..., activateForTarget=true)`를 호출해 `verse_explanations` 고객 노출 row를 저장한다.
- report에는 고객 노출 테이블에 저장된 `summary`와 `explanation`만 싣는다.

## 제외 범위

- 실제 DeepSeek 실패 시 fake fallback 또는 강제 승인
- production DB 접근
- API/OpenAPI/DB schema 변경
- prompt 전문, provider raw response, reference 원문, API key report 수록
- PDF, restricted index, build output 커밋

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiBibleVerseAssetCustomerExposureFlowManualTest.java` | 오늘 QT BIBLE_VERSE 생성, 검수, 승인, 고객 노출 manual 검증 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-bible-verse-asset-customer-exposure-flow.md` | 작업 명세 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-bible-verse-asset-customer-exposure-flow_report.md` | 실행 결과와 고객 노출 산출물 기록 |
| Generate ignored | `qtai-server/build/ai-review-reference/bible-verse-customer-exposure-summary.json` | sanitized manual run summary. Git stage 금지 |

## 구현 순서

1. `dev` 최신 상태에서 `test/ai-bible-verse-asset-customer-exposure-flow` 브랜치를 생성한다.
2. workflow를 저장한다.
3. manual integration test를 추가한다.
4. test 시작 시 DeepSeek key, Docker/MySQL, restricted index 파일을 검증한다.
5. `qt_passages.id=4`, `1CO 3:1-15`, `bible_verses.id=28412..28426`를 확인한다.
6. `qt_passage_verses`가 비어 있으면 15개 link를 생성한다.
7. active explanation prompt/checklist/reference job이 없으면 source name에는 `manual-customer-exposure-20260605`, 30자 제한이 있는 `version` 컬럼에는 `manual-cust-exp-20260605` 식별자로 생성한다.
8. `GetTodayQtUseCase.getToday(null)`와 `GetQtPassageContentContextUseCase.getContentContext(qtPassageId)`를 호출해 `qtPassageId/range/verseIds`를 확인한다.
9. `AiDailyQtVerseExplanationSeedService.seedToday()`로 BIBLE_VERSE generation job을 만든다.
10. 새로 생성된 queued job 중 첫 번째를 `AiGenerationJobRunner.runJob(...)`로 실제 DeepSeek generation까지 실행한다.
11. 생성 asset과 layer 1/2 validation log를 확인한다.
12. layer 1/2가 모두 `PASSED`이면 `ReviewAiAssetUseCase`로 approve + activate를 수행한다.
13. `ai_generated_assets.status=APPROVED`, `verse_explanations.APPROVED ACTIVE`, `GetQtStudyContentUseCase` 응답 노출을 확인한다.
14. DeepSeek 또는 검수 실패 시 approve를 수행하지 않고 sanitized summary/report에 실패 상태를 기록한다.
15. 안전 검증 후 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiBibleVerseAssetCustomerExposureFlowManualTest` | 오늘 QT range와 verseIds 확인 |
| `AiBibleVerseAssetCustomerExposureFlowManualTest` | BIBLE_VERSE generation job과 asset 생성 |
| `AiBibleVerseAssetCustomerExposureFlowManualTest` | layer 1/2 `PASSED`일 때만 approve |
| `AiBibleVerseAssetCustomerExposureFlowManualTest` | 고객 노출 row와 study content 응답 확인 |
| `AiBibleVerseAssetCustomerExposureFlowManualTest` | 실패 시 고객 노출 전환 우회 없음 |

## 수용 기준

- [ ] 오늘 QT `qtPassageId/range`와 `verseIds`가 확인된다.
- [ ] BIBLE_VERSE `EXPLANATION` generation job과 asset이 생성된다.
- [ ] layer 1 `AUTO/PASSED`와 layer 2 `ADVISOR/PASSED`가 저장된다.
- [ ] approve 후 `ai_generated_assets.status=APPROVED`가 된다.
- [ ] `verse_explanations`에 `APPROVED + ACTIVE` row가 저장된다.
- [ ] `GetQtStudyContentUseCase` 응답에 해당 산출물이 포함된다.
- [ ] report에 저장된 고객 노출 `summary`/`explanation`을 포함한다.
- [ ] DeepSeek가 `PASSED`를 만들지 못하면 approve하지 않고 report에 기록한다.
- [ ] 민감 산출물은 staged 대상이 아니다.
- [ ] 커밋이 생성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- dev DB precondition, 실제 DeepSeek 호출, 검수, 승인, 고객 노출 조회가 하나의 순차 흐름에 묶여 있다.
- 실패 시 승인 우회 금지 정책을 같은 실행자가 끝까지 판단해야 한다.
- report에는 실제 저장 산출물을 선별해 싣고 민감 원문은 제외해야 한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, manual test, report, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
docker ps
Test-NetConnection -ComputerName localhost -Port 3306
$env:DEEPSEEK_API_KEY
Get-Item qtai-server\restricted\validation\index\reference-index.json
```

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:QTAI_AI_CUSTOMER_EXPOSURE_SAMPLE='true'
$env:DEEPSEEK_MODEL='deepseek-chat'
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiBibleVerseAssetCustomerExposureFlowManualTest" --rerun-tasks
```

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiDailyQtVerseExplanationSeedServiceTest" --tests "*AiGenerationJobRunner*Test" --tests "*AiAssetReview*Test" --tests "*QtStudyContent*Test" --tests "*AiReviewValidationServiceTest"
```

```powershell
git status --short --ignored -- qtai-server\restricted qtai-server\build\ai-review-reference doc\TalkFile_IVP성경배경주석.pdf.pdf
git diff --cached --name-only -- qtai-server\restricted qtai-server\build doc\TalkFile_IVP성경배경주석.pdf.pdf
rg -n "providerRawResponse|rawResponse|promptText|referenceText|API key|DEEPSEEK_API_KEY" doc\workspaces\DevC_강상민\reports\2026-06-05_ai-bible-verse-asset-customer-exposure-flow_report.md
git diff --cached --check
```

## 후속 작업으로 남길 항목

- 실제 system API/curl 경로에서 approve까지 호출하는 end-to-end 검증
- DeepSeek provider reject가 반복될 경우 provider request/모델 설정 별도 보강
