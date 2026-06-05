# Workflow - 2026-06-05 ai-review-layer2-reference-sample-flow

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `test/ai-reference-layer2-sample-flow` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | layer 2 실제 검수 흐름에서 `restricted://validation/index/reference-index.json`가 읽히고, 산출물 verse range와 겹치는 참조 항목만 prompt에 주입되며, checklist/log에는 원문이 저장되지 않는지 검증해야 한다. |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-excerpt-injection.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-index-storage-uri-flow.md` |
| 대상 경로 | `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/workflows/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

실제 Spring context에서 DB metadata, restricted URI reader, reference index reader, excerpt selector, layer 2 validation service가 연결되는 샘플 흐름을 검증한다. 외부 LLM 호출은 테스트 더블로 대체하고, 캡처한 prompt를 통해 matching 참조 항목만 주입되는지 확인한다.

실제 IVP PDF와 real `reference-index.json`은 테스트/문서/커밋에 포함하지 않는다. 테스트 index는 synthetic JSON만 사용한다.

## 범위

- `restricted://validation/index/reference-index.json` URI를 사용하되, 테스트에서는 임시 restricted root 아래 synthetic index 파일을 만든다.
- ACTIVE `ValidationReferenceJob`, ACTIVE explanation checklist, EXPLANATION asset을 DB에 저장한다.
- `AiReviewValidationService.validateExplanationAsset(...)`를 호출해 layer 2 검수 흐름을 실제 service로 실행한다.
- `LlmClient`는 mock bean으로 교체해 prompt를 캡처하고 `PASSED` 응답을 반환한다.
- prompt에는 matching synthetic 참조 항목만 들어가고 non-matching 항목은 들어가지 않는지 검증한다.
- 저장된 `AiValidationLog.checklistJson`에는 URI/hash/range/count만 남고 원문 필드와 synthetic 원문 문자열은 저장되지 않는지 검증한다.
- workflow/report를 작성한다.

## 제외 범위

- 생산 코드 변경. 단, 기존 계약 위반이 드러난 경우 `AiReviewValidationService` 저장 JSON 구성의 최소 수정만 허용한다.
- DB schema, OpenAPI, API 응답 필드 변경
- 실제 DeepSeek 호출 또는 운영 server/curl 수동 검증
- 실제 IVP PDF, real `reference-index.json`, `qtai-server/restricted/**`, `qtai-server/build/**` 커밋
- 원문 excerpt, 긴 reference text, PDF 본문을 report에 수록

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceLayer2SampleFlowIntegrationTest.java` | layer 2 샘플 흐름, prompt 주입, log 저장 경계 검증 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-layer2-reference-sample-flow.md` | 작업 명세 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-review-layer2-reference-sample-flow_report.md` | 실행 결과와 검증 기록 |

## 구현 순서

1. `dev` 최신 상태에서 `test/ai-reference-layer2-sample-flow` 브랜치를 생성한다.
2. workflow spec을 저장한다.
3. `AiReviewReferenceLayer2SampleFlowIntegrationTest`를 추가한다.
4. 테스트는 `@SpringBootTest`, `@ActiveProfiles("test")`, test 전용 restricted root property를 사용한다.
5. 테스트 시작 전 validation log, generated asset, checklist, validation reference job 데이터를 정리한다.
6. synthetic index 파일을 `validation/index/reference-index.json` 경로에 작성한다.
7. DB에 ACTIVE reference job, ACTIVE explanation checklist, EXPLANATION asset을 저장한다.
8. `LlmClient.complete(...)`는 prompt를 캡처하고 `{"result":"PASSED"}`를 반환하도록 설정한다.
9. `AiReviewValidationService.validateExplanationAsset(...)`를 호출한다.
10. prompt의 reference metadata, matching excerpt, non-matching excerpt 제외를 검증한다.
11. 저장된 layer 2 `AiValidationLog`와 반환 log의 checklist JSON에 원문이 없는지 검증한다.
12. focused test를 실행한다.
13. 민감 산출물 ignored/staged 상태를 확인한다.
14. report를 작성하고 원문성 키워드가 없는지 확인한다.
15. commit message convention에 맞춰 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiReviewReferenceLayer2SampleFlowIntegrationTest` | layer 2 service가 DB metadata의 최종 restricted URI로 index를 읽음 |
| `AiReviewReferenceLayer2SampleFlowIntegrationTest` | asset verse range와 겹치는 참조 항목만 prompt에 포함 |
| `AiReviewReferenceLayer2SampleFlowIntegrationTest` | non-matching 참조 항목은 prompt에 포함되지 않음 |
| `AiReviewReferenceLayer2SampleFlowIntegrationTest` | Fake LLM 응답으로 layer 2 `ADVISOR/PASSED` log 생성 |
| `AiReviewReferenceLayer2SampleFlowIntegrationTest` | `checklistJson`과 저장 log에는 원문 필드/원문 문자열이 저장되지 않음 |

## 수용 기준

- [ ] layer 2 service가 `restricted://validation/index/reference-index.json` URI로 synthetic index를 읽는다.
- [ ] 산출물 verse range와 겹치는 참조 항목만 prompt `reference.excerpts[]`에 포함된다.
- [ ] matching synthetic 원문은 prompt에만 포함되고, non-matching 원문은 prompt에 포함되지 않는다.
- [ ] 저장된 `AiValidationLog.checklistJson`에는 원문 필드와 synthetic 원문 문자열이 없다.
- [ ] 저장된 log에는 reference job id, source hash, index URI, selected hash/range/count만 남는다.
- [ ] 생산 코드, DB schema, OpenAPI, API 응답 필드는 변경되지 않는다.
- [ ] focused test가 통과한다.
- [ ] 민감 산출물은 ignored 상태이며 staged 대상이 아니다.
- [ ] report에는 원문성 키워드와 synthetic 원문 문자열이 포함되지 않는다.
- [ ] 커밋이 생성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 검증 대상이 service, reader, selector, prompt, persisted log 계약을 한 흐름에서 확인하는 단일 통합 테스트다.
- 테스트와 report가 같은 acceptance criteria를 공유하므로 메인 에이전트가 순서대로 검증하는 편이 안전하다.
- 민감 산출물 stage 금지 확인을 마지막에 직접 수행해야 한다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, 테스트 보강, 검증, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceLayer2SampleFlowIntegrationTest" --tests "*AiReviewValidationServiceTest" --tests "*AiReviewReferenceIndexStorageUriFlowTest"
```

```powershell
git status --short --ignored -- qtai-server\restricted qtai-server\build\ai-review-reference doc\TalkFile_IVP성경배경주석.pdf.pdf
git diff --cached --name-only -- qtai-server\restricted qtai-server\build doc\TalkFile_IVP성경배경주석.pdf.pdf
rg -n "referenceText|본문|synthetic-layer2-reference" doc\workspaces\DevC_강상민\reports\2026-06-05_ai-review-layer2-reference-sample-flow_report.md
git diff --cached --check
```

## 후속 작업으로 남길 항목

- 실제 운영 server/curl 기반 layer 2 수동 검증
- 실제 restricted storage 운영 배포 상태 모니터링
- 외부 LLM 호출까지 포함하는 별도 수동 검증 절차
