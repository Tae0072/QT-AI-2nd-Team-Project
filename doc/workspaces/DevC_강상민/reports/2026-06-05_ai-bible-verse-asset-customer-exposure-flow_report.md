# Report - 2026-06-05 ai-bible-verse-asset-customer-exposure-flow

## 요약

- 브랜치: `test/ai-bible-verse-asset-customer-exposure-flow`
- 기준 브랜치: `dev`
- 목적: 오늘 QT BIBLE_VERSE 산출물 생성, layer 1/2 검수, 승인, 고객 노출 row 저장까지 실제 흐름을 검증
- 재실행 결과: DeepSeek 충전 후 실제 호출이 성공했고, `BIBLE_VERSE` `EXPLANATION` asset이 생성/검수/승인되어 고객 노출 row까지 저장됨

## 사전 조건 보강

| 항목 | 결과 |
| --- | --- |
| 오늘 QT | `qtPassageId=4`, `1CO 3:1-15` |
| 오늘 QT verse link | `qt_passage_verses` 15건 생성/확인 |
| active prompt version | `id=1`, `version=manual-cust-exp-20260605` |
| active validation checklist | `id=1`, `version=manual-cust-exp-20260605` |
| active reference job | `id=1`, `indexStorageUri=restricted://validation/index/reference-index.json` |
| reference index hash | `sha256:d50811d18c1d109a1ce0dc8331f25bb7daf249be1892d9ca742cbb64c20eca8b` |

## 실행 결과

| 항목 | 결과 |
| --- | --- |
| flow status | `APPROVED_AND_VISIBLE` |
| seed created count | `15` |
| seed failed count | `0` |
| 실행 대상 job | `id=16`, `targetType=BIBLE_VERSE`, `targetId=28412` |
| generation job status | `SUCCEEDED` |
| 생성 asset | `id=1`, `status=APPROVED`, `assetType=EXPLANATION` |
| layer 1 log | `id=1`, `reviewerType=AUTO`, `result=PASSED` |
| layer 2 log | `id=2`, `reviewerType=ADVISOR`, `result=PASSED` |
| layer 2 reference job | `validationReferenceJobId=1` |
| LLM call count | `2` |
| LLM call status | `COMPLETED`, `COMPLETED` |
| selected reference count | `1` |
| selected reference hash | `sha256:8a20c5e10a0d09f141e40b43e574555b4693df4b25ac0873743f07dc5adfaf58` |
| selected reference range | `고린도전서 2:16-3:10` |
| approve 실행 | 완료 |
| 고객 노출 row | 저장됨 |

## 저장된 고객 노출 산출물

아래 내용은 `verse_explanations` 고객 노출 row에 저장된 `summary`와 `explanation` 값이다.

```text
summary: 바울은 고린도 교인들이 아직 성숙하지 못해 육신에 속한 자처럼 대우했다고 말합니다.

explanation: 바울은 고린도 교인들을 '신령한 자'가 아닌 '육신에 속한 자', 즉 그리스도 안에서 어린 아이들처럼 대했다고 고백합니다. 이는 그들의 신앙이 아직 성숙하지 못했음을 의미하며, 바울이 그들에게 더 깊은 영적 진리를 가르칠 수 없었던 이유를 설명합니다.
```

## 리뷰 대응

- `CapturingLlmClient`는 더 이상 광범위한 `RuntimeException`을 잡지 않는다. DeepSeek 호출 계약에서 의미 있는 `BusinessException`과 직접 전달될 수 있는 `RestClientException`만 metadata로 기록하고, 그 외 런타임 버그는 fail-fast로 남긴다.
- 실행 대상 job은 이번 `seedToday()` 이후 새로 생성된 `QUEUED` job만 선택한다. 기존 dev DB에 남아 있던 오래된 `QUEUED` job으로 fallback하지 않는다.
- `AiBibleVerseAssetCustomerExposureFlowManualTest`는 `ai.internal` 패키지 내부 manual integration test라서 `ai` 내부 entity/repository를 직접 확인한다. 운영 코드 경계와 public API/usecase 구조는 변경하지 않았다.
- CI 기본 실행에서는 `@EnabledIf("customerExposureSampleEnabled")`에 의해 비활성이다. 실제 dev DB, restricted index, DeepSeek 호출이 필요한 manual 검증 전용 테스트이며, 자동 CI에서 provider 비용이나 dev DB side effect를 만들지 않도록 보호한다.

## 저장 경계 확인

- prompt 전문과 provider 응답 원문은 report에 기록하지 않았다.
- DeepSeek 키 값은 report와 summary에 기록하지 않았다.
- layer 2는 `restricted://validation/index/reference-index.json`를 사용했고, 검수 log에는 URI/hash/range/count 중심 metadata만 남긴다.
- 실제 PDF, restricted index, build summary는 Git stage 대상에서 제외한다.

## 검증 명령

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:QTAI_AI_CUSTOMER_EXPOSURE_SAMPLE='true'
$env:DEEPSEEK_MODEL='deepseek-chat'
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiBibleVerseAssetCustomerExposureFlowManualTest" --rerun-tasks
```

결과: `BUILD SUCCESSFUL`

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiBibleVerseAssetCustomerExposureFlowManualTest"
```

결과: `BUILD SUCCESSFUL`

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*DomainBoundaryArchTest" --tests "*ArchitectureBoundaryTest"
```

결과: `BUILD SUCCESSFUL`

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiDailyQtVerseExplanationSeedServiceTest" --tests "*AiGenerationJobRunner*Test" --tests "*AiAssetReview*Test" --tests "*QtStudyContent*Test" --tests "*AiReviewValidationServiceTest"
```

결과: `BUILD SUCCESSFUL`

## 판정

오늘 QT `1CO 3:1-15` 기준 BIBLE_VERSE 산출물 생성부터 고객 노출 상태 전환까지 실제 운영형 흐름이 검증됐다. layer 1 `AUTO/PASSED`와 layer 2 `ADVISOR/PASSED`가 모두 저장된 뒤에만 승인했고, 승인 결과 `ai_generated_assets.status=APPROVED` 및 고객 노출 row 저장을 확인했다.
