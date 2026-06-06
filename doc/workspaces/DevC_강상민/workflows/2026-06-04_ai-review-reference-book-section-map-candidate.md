# Workflow - 2026-06-04 ai-review-reference-book-section-map-candidate

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-review-reference-book-section-map-candidate` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | IVP PDF 전체를 사람이 직접 66권 page range로 매핑하기 어렵기 때문에, PDF에서 책 제목 위치를 자동 탐지해 book section map 후보와 검토 요약을 생성하는 도구가 필요하다. |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-pdf-index-diagnostics.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-candidate-promotion.md` |
| 대상 경로 | `qtai-server/build.gradle.kts`, `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

`doc/TalkFile_IVP성경배경주석.pdf.pdf` 같은 로컬 PDF를 PDFBox로 읽고, 페이지별 텍스트에서 성경 66권 책 제목 시작 페이지를 자동 탐지한다. 탐지 결과는 운영용 최종 `ai-review-reference-book-section-map.v1`이 아니라 검토용 `ai-review-reference-book-section-map-candidate.v1` 후보 JSON과 summary JSON으로 저장한다.

후보가 불완전해도 CLI는 실패하지 않는다. 낮은 신뢰도, 중복 탐지, 누락 책은 summary에 기록해 사람이 최종 map으로 승격하기 전에 확인할 수 있게 한다.

## 범위

- `aiReviewReferenceBookSectionMapCandidate` Gradle JavaExec task를 추가한다.
- CLI 입력은 `--source`, `--output`, `--summary`를 받는다.
- `--output`, `--summary`는 `build/**` 하위 경로만 허용한다.
- 책 목록 기준은 기존 Bible seed convention의 3글자 코드와 한글 책 이름을 사용한다.
- PDFBox로 페이지별 텍스트를 추출하고 책 제목 라인 후보를 탐지한다.
- 탐지된 시작 페이지를 정렬해 `pageEnd = 다음 책 시작 페이지 - 1`로 계산한다.
- candidate JSON에는 section별 `bookCode`, `bookName`, `pageStart`, `pageEnd`, `detectedTitle`, `confidence`, `reasons`를 기록한다.
- summary JSON에는 탐지 수, `HIGH/LOW/MISSING` 수, 중복 탐지 수, 누락 책 목록, source hash를 기록한다.
- report를 작성하고 커밋 후 작업 브랜치에 push한다.

## 제외 범위

- 최종 운영용 `ai-review-reference-book-section-map.v1` 파일 작성
- `restricted/validation/index/reference-index.json` 배포
- promotion CLI로 실제 IVP 운영 index 생성
- layer 2 prompt, DB, OpenAPI, 관리자 API 변경
- PDF 원문, 추출 본문, candidate/summary JSON 산출물 커밋
- summary/report/checklist/audit에 PDF 본문 또는 긴 excerpt 저장

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/build.gradle.kts` | `aiReviewReferenceBookSectionMapCandidate` task 추가 |
| Create | `AiReviewReferenceBookCatalog.java` | 66권 bookCode/bookName/displayOrder 기준 catalog 제공 |
| Create | `AiReviewReferenceBookSectionTitleDetector.java` | 페이지 라인에서 책 제목 후보와 confidence 판단 |
| Create | `AiReviewReferenceBookSectionMapCandidateGenerator.java` | 페이지별 텍스트를 section 후보와 summary로 변환 |
| Create | `AiReviewReferenceBookSectionMapCandidateWriter.java` | candidate/summary JSON 작성 |
| Create | `AiReviewReferenceBookSectionMapCandidateTool.java` | CLI entry point와 인자/경로 검증 |
| Test | `AiReviewReferenceBookSectionTitleDetectorTest.java` | 제목 탐지/오탐 방지/낮은 confidence 검증 |
| Test | `AiReviewReferenceBookSectionMapCandidateGeneratorTest.java` | page range 계산, 누락/중복 summary 검증 |
| Test | `AiReviewReferenceBookSectionMapCandidateWriterTest.java` | schema와 원문 미포함 검증 |
| Test | `AiReviewReferenceBookSectionMapCandidateToolTest.java` | CLI 인자와 build 하위 출력 경로 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-book-section-map-candidate_report.md` | 실행 결과와 후속 작업 기록 |

## 구현 순서

1. workflow spec을 저장한다.
2. workflow-spec-runner 절차로 spec을 읽고 직접 실행을 선택한다.
3. book catalog와 title detector 테스트를 먼저 추가하고 구현한다.
4. candidate generator 테스트를 추가하고 구현한다.
5. writer 테스트를 추가하고 candidate/summary JSON 출력을 구현한다.
6. CLI tool 테스트를 추가하고 Gradle task를 연결한다.
7. focused test와 전체 test를 실행한다.
8. 로컬 PDF가 있으면 수동 CLI를 실행하고 summary 수치만 report에 남긴다.
9. report를 작성한다.
10. commit-message-convention에 맞춰 커밋하고 pr-prep-assistant 기준으로 PR 내용을 준비한 뒤 push한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiReviewReferenceBookSectionTitleDetectorTest` | 정확한 책 제목 라인을 탐지한다. |
| `AiReviewReferenceBookSectionTitleDetectorTest` | 본문 중 우연히 등장한 책 이름은 제목으로 오탐하지 않는다. |
| `AiReviewReferenceBookSectionTitleDetectorTest` | 약한 매칭은 `LOW` 사유를 남긴다. |
| `AiReviewReferenceBookSectionMapCandidateGeneratorTest` | 탐지된 시작 페이지로 `pageStart/pageEnd`를 계산한다. |
| `AiReviewReferenceBookSectionMapCandidateGeneratorTest` | 중복 탐지와 누락 책을 summary에 기록한다. |
| `AiReviewReferenceBookSectionMapCandidateGeneratorTest` | 불완전한 결과여도 후보 JSON 생성은 실패하지 않는다. |
| `AiReviewReferenceBookSectionMapCandidateWriterTest` | candidate schema와 필수 필드를 생성한다. |
| `AiReviewReferenceBookSectionMapCandidateWriterTest` | summary에 PDF 원문이나 긴 본문이 포함되지 않는다. |
| `AiReviewReferenceBookSectionMapCandidateToolTest` | 필수 CLI 인자 누락을 거부한다. |
| `AiReviewReferenceBookSectionMapCandidateToolTest` | `--output`, `--summary`가 `build/**` 밖이면 거부한다. |

## 수용 기준

- [ ] candidate schemaVersion은 `ai-review-reference-book-section-map-candidate.v1`이다.
- [ ] 책 코드는 `GEN`, `EXO`, `MAT`, `MRK`, `LUK`, `JHN`, `REV` 같은 3글자 convention을 따른다.
- [ ] `HIGH`, `LOW`, `MISSING` 상태를 구분해 기록한다.
- [ ] 누락 책이 있어도 CLI는 candidate와 summary를 생성한다.
- [ ] output/summary는 `build/**` 하위만 허용한다.
- [ ] summary/report에는 PDF 본문, 추출 원문, 긴 excerpt가 포함되지 않는다.
- [ ] focused test와 가능한 전체 test가 통과한다.
- [ ] report가 작성된다.
- [ ] 작업 브랜치에 push된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- detector, generator, writer, CLI가 같은 내부 record와 JSON 계약을 공유해 순차적으로 맞추는 편이 안전하다.
- 테스트와 구현 경로가 모두 `ai/internal` 아래에서 겹친다.
- workflow 작성, 구현, 검증, report, commit, push까지 한 흐름으로 확인해야 한다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow spec 기준으로 TDD, 구현, 검증, report, 커밋, push를 직접 수행한다.

## 검증 계획

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceBookSection*Test"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test
.\qtai-server\gradlew.bat --no-daemon -p qtai-server aiReviewReferenceBookSectionMapCandidate --args="--source ..\doc\TalkFile_IVP성경배경주석.pdf.pdf --output build\ai-review-reference\ivp-book-section-map-candidate.json --summary build\ai-review-reference\ivp-book-section-map-candidate-summary.json"
```

## 후속 작업으로 남길 항목

- 사람이 candidate summary를 보고 최종 `ai-review-reference-book-section-map.v1`로 정리
- promotion CLI로 운영용 `reference-index.json` 생성
- `restricted/validation/index/reference-index.json` 배포
- `validation_reference_jobs.indexStorageUri`와 운영 index 연결 검증
