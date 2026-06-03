# Report - 2026-06-04 ai-review-reference-pdf-index-diagnostics

## 실행 기준

- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-pdf-index-diagnostics.md`
- 브랜치: `feature/ai-review-reference-pdf-index-diagnostics`
- PR 대상: `dev`
- 관련 F-ID: 해당 없음

## 구현 내용

- `qtai-server`에 PDFBox 기반 진단 CLI를 추가했다.
- Gradle task `aiReviewReferencePdfIndexDiagnostics`를 추가했다.
- heading parser는 `19:8`, `19:11-15`, `20:1-26`, `19:42-20:6` 형태의 명확한 장절 heading만 파싱한다.
- PDF 추출 본문 품질 진단을 추가했다.
  - `replacementCharCount`
  - `questionMarkRatio`
  - `suspiciousMojibakeRatio`
  - `hangulRatio`
  - `textLength`
  - `status = USABLE | NEEDS_REVIEW | UNUSABLE`
- candidate JSON writer를 추가했다.
  - candidate schema: `ai-review-reference-index-candidate.v1`
  - candidate에는 진단용 `referenceText`가 포함된다.
  - summary에는 `referenceText` 원문을 포함하지 않는다.
- `.gitignore`에 `doc/TalkFile_IVP*.pdf*`를 추가해 로컬 PDF가 커밋되지 않게 했다.

## 수동 진단 결과

실행 명령:

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server aiReviewReferencePdfIndexDiagnostics --args="--source ..\doc\TalkFile_IVP성경배경주석.pdf.pdf --output build\ai-review-reference\ivp-reference-index-candidate.json --summary build\ai-review-reference\ivp-reference-index-diagnostics.json"
```

결과:

| 항목 | 값 |
| --- | --- |
| sourceFileName | `TalkFile_IVP성경배경주석.pdf.pdf` |
| schemaVersion | `ai-review-reference-index-candidate.v1` |
| totalEntryCount | 1492 |
| usableEntryCount | 0 |
| needsReviewEntryCount | 1487 |
| unusableEntryCount | 5 |
| averageHangulRatio | 0.5993944266148904 |
| averageSuspiciousMojibakeRatio | 0.000152177713682691 |
| totalReplacementCharCount | 491 |

판단:

- PDFBox 기반 heading 후보 추출은 전체 후보를 생성할 수 있다.
- `bookCode`는 이번 v1에서 확정하지 않아 모든 후보가 최소 `NEEDS_REVIEW` 대상이다.
- summary 기준으로 원문 `referenceText`는 포함되지 않았다.
- candidate JSON은 추출 원문을 포함하므로 `qtai-server/build/ai-review-reference/` 아래에만 생성하고 Git에는 포함하지 않는다.

## 리뷰 반영

- generator의 페이지 텍스트 분리 로직을 `entriesFromPages()`로 추출해 단위 테스트를 추가했다.
- heading parser는 `19:8 설명문` 같은 본문 줄을 heading으로 보지 않도록 순수 장절 heading만 허용한다.
- 페이지 끝 heading과 다음 페이지 본문이 이어지는 경우를 보존하도록 page-spanning 상태를 유지한다.
- CLI 출력 경로는 `build/**` 아래만 허용한다.
- candidate JSON 커밋 실수를 줄이기 위해 `.gitignore`에 `**/ai-review-reference/*-candidate*.json`를 추가했다.

리뷰 반영 후 진단 결과:

| 항목 | 이전 | 반영 후 |
| --- | ---: | ---: |
| totalEntryCount | 11041 | 1492 |
| needsReviewEntryCount | 9402 | 1487 |
| unusableEntryCount | 1639 | 5 |

## 검증 결과

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferencePdf*Test"
```

- 결과: 성공

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test
```

- 결과: 성공

참고:

- Gradle 종료 후 Windows file watcher 경고가 출력됐지만, 두 테스트 명령 모두 `BUILD SUCCESSFUL`로 종료됐다.
- 수동 진단 명령도 `BUILD SUCCESSFUL`로 종료됐다.

## 제외 범위 확인

- 운영용 `ai-review-reference-index.v1` 생성은 하지 않았다.
- `restricted://validation/index/reference-index.json` 배포는 하지 않았다.
- layer 2 검수 AI prompt 연결은 하지 않았다.
- PDF 원본과 candidate JSON은 커밋하지 않는다.
- summary/report/checklist/audit에는 추출 원문을 남기지 않았다.

## 후속 작업

- candidate JSON을 검토해 운영용 index로 승격할 변환/검수 절차를 만든다.
- `bookCode` 자동 확정 또는 수동 보정 기준을 정한다.
- 검수 완료된 운영용 index를 restricted 저장소에 배포한다.
- layer 2 prompt가 운영용 index excerpt를 읽도록 연결한다.
