# Workflow - 2026-06-05 ai-bible-verse-assets-customer-exposure-full-flow

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `test/ai-bible-verse-assets-customer-exposure-full-flow` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | 이전 manual 검증은 오늘 QT `1CO 3:1-15` 중 1개 BIBLE_VERSE 산출물만 고객 노출까지 확인했다. 오늘 QT 화면 완성 기준으로는 15개 verse 전체 노출 검증이 필요하다. |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-bible-verse-asset-customer-exposure-flow.md` |
| 대상 경로 | `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/workflows/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

오늘 QT `1CO 3:1-15`의 15개 BIBLE_VERSE `EXPLANATION` 산출물이 모두 고객 노출 상태인지 검증한다. 이미 `verse_explanations`에 고객 노출된 verse는 `ALREADY_VISIBLE`로 인정하고, 누락된 verse만 실제 DeepSeek generation, layer 1/2 검수, approve, activate를 수행한다.

최종 report에는 고객이 볼 수 있는 15개 `summary`와 `explanation`을 모두 기록한다. prompt 전문, provider 응답 원문, 참조자료 원문, DeepSeek 키 값은 기록하지 않는다.

## 범위

- `AiBibleVerseAssetCustomerExposureFlowManualTest`를 전체 15개 verse loop로 수정한다.
- 기본 실행은 계속 비활성화하고 `QTAI_AI_CUSTOMER_EXPOSURE_SAMPLE=true`일 때만 실행한다.
- dev MySQL, 실제 restricted index, 실제 `DeepSeekLlmClient`를 사용한다.
- 기존 고객 노출 row가 있는 verse는 재생성하지 않고 최종 커버리지에 포함한다.
- 누락 verse는 이번 `seedToday()` 이후 새로 생성된 `QUEUED` job을 우선 실행한다.
- dev DB에 이미 남아 있던 `QUEUED` job을 사용할 수밖에 없는 경우에는 `PREEXISTING_QUEUED`로 provenance를 summary에 기록한다.
- 새 산출물은 layer 1 `AUTO/PASSED`와 layer 2 `ADVISOR/PASSED`일 때만 approve + activate한다.

## 제외 범위

- 실제 DeepSeek 실패 시 fake fallback 또는 강제 승인
- production DB 접근
- API/OpenAPI/DB schema 변경
- prompt 전문, provider raw response, reference 원문, API key report 수록
- PDF, restricted index, build output 커밋
- 오늘 QT 이외 날짜/range 산출물 생성

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiBibleVerseAssetCustomerExposureFlowManualTest.java` | 오늘 QT 15개 BIBLE_VERSE 전체 generation/validation/approval/exposure loop 검증 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-bible-verse-assets-customer-exposure-full-flow.md` | 작업 명세 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-bible-verse-assets-customer-exposure-full-flow_report.md` | 실행 결과와 15개 고객 노출 산출물 기록 |
| Generate ignored | `qtai-server/build/ai-review-reference/bible-verse-customer-exposure-full-summary.json` | sanitized manual run summary. Git stage 금지 |

## 구현 순서

1. `dev` 최신 상태에서 `test/ai-bible-verse-assets-customer-exposure-full-flow` 브랜치를 생성한다.
2. workflow를 저장한다.
3. 기존 manual test의 단일 `jobId` 처리 흐름을 전체 verse loop로 바꾼다.
4. test 시작 시 DeepSeek key, restricted index 파일, 오늘 QT range, `verseIds=28412..28426`를 검증한다.
5. `qt_passage_verses`, active prompt/checklist/reference job precondition을 기존처럼 idempotent하게 보강한다.
6. loop 전 `GetQtStudyContentUseCase.getStudyContent(qtPassageId)`로 이미 노출된 verse를 기록한다.
7. `AiDailyQtVerseExplanationSeedService.seedToday()`를 실행한다.
8. 이번 seed 이후 새로 생성된 `QUEUED` job을 verse별로 매핑한다.
9. 새 job이 없고 기존 `QUEUED` job만 있으면 `PREEXISTING_QUEUED`로 표시하고 해당 job을 실행한다.
10. 노출이 없는 verse마다 선택된 job을 `AiGenerationJobRunner.runJob(...)`로 실행한다.
11. asset과 layer 1/2 validation log를 확인한다.
12. layer 1/2가 모두 `PASSED`인 asset만 `ReviewAiAssetUseCase`로 approve + activate한다.
13. 최종 `GetQtStudyContentUseCase` 응답에서 15개 verse 노출을 확인한다.
14. ignored full summary JSON과 report에 15개 고객 노출 `summary`/`explanation`을 기록한다.
15. 안전 검증 후 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiBibleVerseAssetCustomerExposureFlowManualTest` | 오늘 QT range와 15개 verseIds 확인 |
| `AiBibleVerseAssetCustomerExposureFlowManualTest` | 이미 visible인 verse는 `ALREADY_VISIBLE`로 기록 |
| `AiBibleVerseAssetCustomerExposureFlowManualTest` | 누락 verse만 `QUEUED` job 실행, 기존 job 사용 시 `PREEXISTING_QUEUED` 기록 |
| `AiBibleVerseAssetCustomerExposureFlowManualTest` | 새 산출물은 layer 1/2 `PASSED`일 때만 approve |
| `AiBibleVerseAssetCustomerExposureFlowManualTest` | 최종 study content 응답에 15개 노출 확인 |

## 수용 기준

- [ ] 오늘 QT `1CO 3:1-15`의 15개 verse가 모두 확인된다.
- [ ] 최종 고객 노출 응답에 15개 BIBLE_VERSE `EXPLANATION`이 포함된다.
- [ ] 새로 생성된 산출물은 layer 1/2 `PASSED` 후에만 approve된다.
- [ ] report에 15개 고객 노출 `summary`/`explanation`이 모두 포함된다.
- [ ] prompt 전문, provider 응답 원문, 참조자료 원문, DeepSeek 키 값은 기록하지 않는다.
- [ ] PDF, `restricted/**`, `build/**` 산출물은 stage하지 않는다.
- [ ] 커밋 메시지는 `test(ai): 오늘 QT BIBLE_VERSE 전체 고객 노출 흐름을 검증`이다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- dev DB 상태, 실제 DeepSeek 호출, 검수, 승인, 최종 노출 확인이 하나의 순차 흐름으로 묶여 있다.
- 실패 시 approve 우회 금지 판단을 같은 실행 컨텍스트에서 내려야 한다.
- report에는 실제 저장된 고객 노출 산출물을 선별해 싣고 민감 원문은 제외해야 하므로 주 실행자가 직접 확인하는 편이 안전하다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, manual test, report, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:QTAI_AI_CUSTOMER_EXPOSURE_SAMPLE='true'
$env:DEEPSEEK_MODEL='deepseek-chat'
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiBibleVerseAssetCustomerExposureFlowManualTest" --rerun-tasks
```

실제 dev DB를 공유하는 외부 `qtai-server` 앱 컨테이너가 떠 있으면 background worker가 동일 `QUEUED` job을 먼저 claim할 수 있다. manual test 소유권을 확보하기 위해 MySQL/Redis는 유지하되 앱 컨테이너 worker는 중지한 상태에서 실행한다.

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiDailyQtVerseExplanationSeedServiceTest" --tests "*AiGenerationJobRunner*Test" --tests "*AiAssetReview*Test" --tests "*QtStudyContent*Test" --tests "*AiReviewValidationServiceTest" --tests "*DomainBoundaryArchTest" --tests "*ArchitectureBoundaryTest"
```

```powershell
git status --short --ignored -- qtai-server\restricted qtai-server\build\ai-review-reference doc\TalkFile_IVP성경배경주석.pdf.pdf
git diff --cached --name-only -- qtai-server\restricted qtai-server\build doc\TalkFile_IVP성경배경주석.pdf.pdf
rg -n "providerRawResponse|rawResponse|promptText|referenceText|API key|DEEPSEEK_API_KEY" doc\workspaces\DevC_강상민\reports\2026-06-05_ai-bible-verse-assets-customer-exposure-full-flow_report.md
git diff --cached --check
```

## 후속 작업으로 남길 항목

- 오늘 QT 전체 15건이 안정화된 뒤 system API/curl 기반 운영자 승인 흐름 end-to-end 검증
- DeepSeek 비용/실패율 관찰용 별도 운영 리포트 정리
