# Workflow - 2026-06-05 ai-review-reference-index-storage-uri-flow

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `test/ai-reference-index-storage-uri-flow` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | `validation_reference_jobs.indexStorageUri`가 DB metadata로 저장되고 layer 2 검수 흐름에서 동일 URI로 index reader에 전달되는지 검증해야 한다. |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-restricted-index-link.md`, `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-review-reference-restricted-index-link_report.md` |
| 대상 경로 | `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/workflows/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

`validation_reference_jobs.indexStorageUri = restricted://validation/index/reference-index.json`가 DB row에 저장되고, 최신 ACTIVE reference metadata로 조회되며, layer 2 검수 서비스가 같은 URI를 `AiReviewReferenceIndexReader`에 넘기는 흐름을 테스트로 고정한다.

테스트에는 실제 IVP 원문 JSON을 사용하지 않는다. synthetic 소형 index JSON과 짧은 테스트 문자열만 사용한다.

## 범위

- repository 테스트에서 `.json`까지 포함한 `indexStorageUri` 저장/조회 검증을 추가한다.
- reference service 테스트에서 최신 ACTIVE metadata가 `.json`까지 포함한 URI를 반환하는지 검증한다.
- 내부 흐름 테스트를 추가해 DB에서 가져온 metadata URI로 restricted index를 읽고 verse range selection이 되는지 검증한다.
- layer 2 validation service 테스트의 URI 기대값을 최종 URI로 보강한다.
- workflow/report를 작성한다.

## 제외 범위

- DB schema, API, OpenAPI 변경
- system API 실제 서버 호출 검증
- 운영 restricted storage 배포
- 실제 IVP 원문 JSON 또는 PDF를 테스트/문서에 포함
- `qtai-server/restricted/**`, `qtai-server/build/**`, PDF 커밋

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/internal/ValidationReferenceJobRepositoryTest.java` | DB row의 index URI 저장/조회 검증 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceServiceTest.java` | 최신 ACTIVE metadata의 index URI 반환 검증 |
| Create | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceIndexStorageUriFlowTest.java` | DB metadata -> reader -> selector 연결 검증 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewValidationServiceTest.java` | layer 2가 최종 URI를 reader/prompt/checklist에 사용하는지 검증 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-index-storage-uri-flow.md` | 작업 명세 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-review-reference-index-storage-uri-flow_report.md` | 실행 결과와 검증 기록 |

## 구현 순서

1. `dev` 최신 상태에서 `test/ai-reference-index-storage-uri-flow` 브랜치를 생성한다.
2. workflow spec을 저장한다.
3. `ValidationReferenceJobRepositoryTest`에 `.json`까지 포함한 URI 저장/조회 assertion을 추가한다.
4. `AiReviewReferenceServiceTest`의 test fixture URI를 `restricted://validation/index/reference-index.json`로 바꾸고 metadata 반환을 검증한다.
5. `AiReviewReferenceIndexStorageUriFlowTest`를 추가한다.
6. `AiReviewValidationServiceTest`의 metadata/read expectation/prompt assertion을 최종 URI로 보강한다.
7. focused test를 실행한다.
8. 민감 산출물 ignored/staged 상태를 확인한다.
9. report를 작성하고 원문성 키워드가 없는지 확인한다.
10. commit message convention에 맞춰 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `ValidationReferenceJobRepositoryTest` | DB row가 `restricted://validation/index/reference-index.json`를 저장/조회 |
| `AiReviewReferenceServiceTest` | latest ACTIVE metadata가 최종 URI를 반환 |
| `AiReviewReferenceIndexStorageUriFlowTest` | DB metadata URI로 synthetic restricted index 읽기 및 verse range 선택 |
| `AiReviewValidationServiceTest` | layer 2가 최종 URI를 reader와 prompt/checklist metadata에 사용 |

## 수용 기준

- [ ] DB row에 최종 `indexStorageUri`가 저장/조회된다.
- [ ] latest ACTIVE reference metadata가 최종 URI를 반환한다.
- [ ] reader가 metadata URI로 synthetic restricted index를 읽는다.
- [ ] selector가 asset verse range와 겹치는 synthetic entry를 선택한다.
- [ ] layer 2 검수 흐름이 최종 URI를 reader에 전달한다.
- [ ] prompt에는 선택된 짧은 테스트 문자열만 들어가고 checklist/log에는 원문 텍스트가 저장되지 않는다.
- [ ] focused test가 통과한다.
- [ ] 민감 산출물은 ignored 상태이며 staged 대상이 아니다.
- [ ] report에는 원문성 키워드가 포함되지 않는다.
- [ ] 커밋이 생성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 테스트들이 같은 내부 계약인 reference metadata URI 흐름에 묶여 있다.
- fixture URI 값을 일관되게 맞춰야 하므로 한 흐름으로 수정하는 편이 안전하다.
- 민감 산출물 stage 금지 확인을 직접 수행해야 한다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, 테스트 보강, 검증, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*ValidationReferenceJobRepositoryTest" --tests "*AiReviewReferenceServiceTest" --tests "*AiReviewReferenceIndexStorageUriFlowTest" --tests "*AiReviewValidationServiceTest"
```

```powershell
git status --short --ignored -- qtai-server\restricted qtai-server\build\ai-review-reference doc\TalkFile_IVP성경배경주석.pdf.pdf
git diff --cached --name-only -- qtai-server\restricted qtai-server\build doc\TalkFile_IVP성경배경주석.pdf.pdf
rg -n "referenceText|excerpt|본문" doc\workspaces\DevC_강상민\reports\2026-06-05_ai-review-reference-index-storage-uri-flow_report.md
```

## 후속 작업으로 남길 항목

- system API 실제 서버 호출 방식 검증
- 운영 환경 `QTAI_RESTRICTED_STORAGE_ROOT` 연결 검증
- layer 2 실제 산출물 샘플 검수
