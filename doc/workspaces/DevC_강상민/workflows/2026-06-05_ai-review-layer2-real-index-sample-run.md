# Workflow - 2026-06-05 ai-review-layer2-real-index-sample-run

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `test/ai-reference-layer2-real-index-sample-run` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | synthetic index가 아니라 로컬 restricted storage의 실제 `reference-index.json`로 layer 2 검수 샘플을 실행하고, 실제 DeepSeek 호출 후 저장 log에 원문이 남지 않는지 확인해야 한다. |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-layer2-reference-sample-flow.md`, `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-index-quality-evaluation_report.md` |
| 대상 경로 | `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/workflows/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

dev MySQL과 로컬 restricted storage의 실제 운영 후보 index를 사용해 layer 2 검수 샘플을 한 번 실행한다. 샘플은 임시 DB row로 만들고 test transaction rollback으로 정리한다.

외부 LLM은 실제 DeepSeek를 1회 호출한다. prompt 전문, provider raw response, 참조 원문은 파일/report/log에 저장하지 않고, URI/hash/range/count와 판정 결과만 기록한다.

## 범위

- `AiReviewReferenceLayer2RealIndexSampleManualTest`를 추가한다.
- 기본 실행은 비활성화하고 `QTAI_AI_REVIEW_REAL_SAMPLE=true` 또는 test JVM system property가 있을 때만 실행한다.
- `@ActiveProfiles("dev")`와 dev MySQL을 사용한다.
- `qtai.validation.restricted-storage-root`는 로컬 `qtai-server/restricted` 절대 경로로 지정한다.
- sample reference job, checklist, prompt version, generation job, asset row를 생성한다.
- sample asset verse range는 실제 index에서 matching 항목이 확인된 `JHN 3:16`으로 둔다.
- 실제 파일의 `generatedAt` numeric timestamp와 reader 계약 불일치가 드러나면 reader가 문자열/숫자형을 모두 허용하도록 최소 보강한다.
- future promotion output은 `generatedAt`을 ISO 문자열로 쓰도록 writer를 보강한다.
- 실제 `DeepSeekLlmClient` 호출을 수행하되, test wrapper가 prompt metadata만 캡처한다.
- sanitized summary JSON은 `qtai-server/build/ai-review-reference/layer2-real-index-sample-summary.json`에 쓴다.
- workflow/report를 작성한다.

## 제외 범위

- 생산 코드 변경
- DB schema, OpenAPI, API 응답 필드 변경
- 실제 운영 서버/curl 검증
- 실제 IVP PDF, real index JSON, `qtai-server/restricted/**`, `qtai-server/build/**` 커밋
- prompt 전문, provider raw response, 참조 원문을 report나 tracked file에 수록

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceLayer2RealIndexSampleManualTest.java` | real index, dev DB, actual DeepSeek 호출 기반 manual 검증 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferenceIndexReader.java` | 기존 real index numeric `generatedAt` 호환 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferenceCandidatePromotionService.java` | future promoted index `generatedAt` ISO 문자열 직렬화 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceIndexReaderTest.java` | numeric `generatedAt` reader 호환 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceCandidatePromotionServiceTest.java` | promoted index writer의 ISO 문자열 직렬화 검증 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-layer2-real-index-sample-run.md` | 작업 명세 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-review-layer2-real-index-sample-run_report.md` | 실행 결과와 안전 검증 기록 |
| Generate ignored | `qtai-server/build/ai-review-reference/layer2-real-index-sample-summary.json` | sanitized manual run summary. Git stage 금지 |

## 구현 순서

1. `dev` 최신 상태에서 `test/ai-reference-layer2-real-index-sample-run` 브랜치를 생성한다.
2. workflow spec을 저장한다.
3. manual integration test를 추가한다.
4. real index `generatedAt` 호환성을 reader/writer 테스트로 보강한다.
5. test 시작 시 real sample flag, DeepSeek key, real index 파일, MySQL 연결 가능성을 검증한다.
6. `JHN 3:16`과 겹치는 real index entry가 1개 이상 있는지 reader/selector 흐름으로 확인한다.
7. 임시 reference job/checklist/prompt version/generation job/asset row를 생성한다.
8. `AiReviewValidationService.validateExplanationAsset(...)`를 호출한다.
9. 실제 DeepSeek 호출이 1회 수행됐는지 확인한다.
10. prompt metadata에서 index URI와 selected hash/range/count를 검증한다.
11. 저장 log checklist JSON에 원문 필드와 참조 원문 문자열이 없는지 검증한다.
12. sanitized summary JSON을 build output에 쓴다.
13. manual test와 회귀 test를 실행한다.
14. 민감 산출물 ignored/staged 상태를 확인한다.
15. report를 작성하고 원문성 키워드가 없는지 확인한다.
16. commit message convention에 맞춰 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiReviewReferenceLayer2RealIndexSampleManualTest` | real `reference-index.json` schema/hash/count 확인 |
| `AiReviewReferenceLayer2RealIndexSampleManualTest` | `restricted://validation/index/reference-index.json`가 layer 2 흐름에 사용됨 |
| `AiReviewReferenceLayer2RealIndexSampleManualTest` | `JHN 3:16`에 matching selected hash/range가 존재 |
| `AiReviewReferenceLayer2RealIndexSampleManualTest` | 실제 DeepSeek 호출 1회 수행 |
| `AiReviewReferenceLayer2RealIndexSampleManualTest` | `ai_validation_logs.checklist_json`에는 원문 필드/원문 문자열이 저장되지 않음 |
| `AiReviewReferenceIndexReaderTest` | numeric `generatedAt` real index를 읽음 |
| `AiReviewReferenceCandidatePromotionServiceTest` | future promoted index는 ISO 문자열 `generatedAt`을 씀 |

## 수용 기준

- [ ] real index schema/hash/count가 확인된다.
- [ ] reader가 기존 numeric `generatedAt` real index를 읽는다.
- [ ] promotion writer가 future output `generatedAt`을 ISO 문자열로 쓴다.
- [ ] sample reference job의 `indexStorageUri`가 layer 2 prompt metadata에 연결된다.
- [ ] `JHN 3:16` verse range에 맞는 참조 hash/range가 선택된다.
- [ ] 실제 DeepSeek 호출이 1회 수행되고 layer 2 `ADVISOR` log가 생성된다.
- [ ] log result는 `PASSED`, `REJECTED`, `NEEDS_REVIEW` 중 하나다.
- [ ] 저장 log에는 URI/hash/range/count만 남고 참조 원문은 저장되지 않는다.
- [ ] report에는 원문, 긴 excerpt, prompt 전문, provider raw response가 없다.
- [ ] focused manual test와 회귀 test가 통과한다.
- [ ] 민감 산출물은 ignored 상태이며 staged 대상이 아니다.
- [ ] 커밋이 생성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- DB sample, real index, prompt capture, DeepSeek 호출, 저장 경계 확인이 하나의 수동 검증 흐름으로 묶여 있다.
- 병렬 작업보다 단일 실행자가 precondition과 cleanup을 직접 확인하는 편이 안전하다.
- 민감 산출물과 원문 저장 금지 검증을 마지막에 직접 수행해야 한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, manual test, 검증, report, 커밋을 직접 수행한다.

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
$env:QTAI_AI_REVIEW_REAL_SAMPLE='true'
$env:DEEPSEEK_MODEL='deepseek-chat'
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceLayer2RealIndexSampleManualTest" --rerun-tasks
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewValidationServiceTest" --tests "*AiReviewReferenceIndexReaderTest" --tests "*AiReviewReferenceExcerptSelectorTest" --tests "*AiReviewReferenceCandidatePromotionServiceTest"
```

```powershell
git status --short --ignored -- qtai-server\restricted qtai-server\build\ai-review-reference doc\TalkFile_IVP성경배경주석.pdf.pdf
git diff --cached --name-only -- qtai-server\restricted qtai-server\build doc\TalkFile_IVP성경배경주석.pdf.pdf
rg -n "referenceText|본문|synthetic-layer2-reference" doc\workspaces\DevC_강상민\reports\2026-06-05_ai-review-layer2-real-index-sample-run_report.md
git diff --cached --check
```

## 후속 작업으로 남길 항목

- 운영 server/curl 기반 검증
- 실제 운영 DB row 기반 asset 선택 검증
- 실제 판정 품질 샘플링과 사람이 보는 품질 평가
