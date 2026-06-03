# Workflow - 2026-06-04 ai-review-reference-pdf-index-diagnostics

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-review-reference-pdf-index-diagnostics` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | layer 2 검수 AI 참조자료 index를 사람이 전부 작성하기 어렵기 때문에, PDF 원본에서 자동 후보를 만들고 한글 추출 품질을 먼저 진단해야 함 |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-index-contract.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-index-reader.md` |
| 대상 경로 | `.gitignore`, `qtai-server/build.gradle.kts`, `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

`doc/TalkFile_IVP성경배경주석.pdf.pdf`를 PDFBox로 전체 추출해 장절 heading 기반 index 후보 JSON을 생성한다. 동시에 추출된 본문의 한글 품질을 점수화해서 운영용 index로 승격 가능한지 판단할 수 있는 진단 summary를 만든다.

이번 산출물은 운영용 `ai-review-reference-index.v1`이 아니라 진단용 `ai-review-reference-index-candidate.v1`이다. `restricted://validation/index/reference-index.json` 배포와 layer 2 prompt 연결은 이번 범위에 포함하지 않는다.

## 범위

- `qtai-server`에 PDFBox 의존성과 진단용 JavaExec task를 추가한다.
- PDF heading parser를 추가해 `19:8`, `19:11-15`, `20:1-26`, `19:42-20:6` 형태의 명확한 장절 범위를 파싱한다.
- PDF 전체 텍스트를 페이지 단위로 추출하고 heading 이후 본문을 후보 entry로 만든다.
- candidate JSON에는 `schemaVersion`, `sourceFileName`, `sourceFileHash`, `generatedAt`, `qualitySummary`, `entries[]`를 기록한다.
- entry에는 `pageStart`, `detectedHeading`, `bookCode`, 장절 범위, `referenceRangeLabel`, `referenceText`, `referenceHash`, `quality`를 기록한다.
- `bookCode`를 안정적으로 찾지 못하면 `null`로 두고 `quality.status = NEEDS_REVIEW`와 사유를 기록한다.
- summary JSON과 report에는 원문 `referenceText`를 남기지 않고 집계 수치와 판단만 남긴다.
- `.gitignore`에 `doc/TalkFile_IVP*.pdf*`를 추가해 로컬 PDF가 커밋되지 않게 한다.

## 제외 범위

- 운영용 `ai-review-reference-index.v1` 생성 또는 restricted 저장소 배포
- layer 2 검수 AI prompt에 후보 JSON을 직접 연결
- PDF 원문, 후보 JSON, 추출 원문을 Git에 커밋
- OCR, 수동 교정 UI, 관리자 API/OpenAPI 변경
- 성경 본문 또는 외부 SSoT 원문을 DB, 감사 로그, checklist 로그에 저장
- bookCode 자동 확정 로직의 완성

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `.gitignore` | 로컬 IVP PDF 커밋 방지 |
| Modify | `qtai-server/build.gradle.kts` | PDFBox 의존성과 진단 task 추가 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferencePdfHeadingParser.java` | heading 문자열을 장절 범위로 파싱 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferenceTextQualityAnalyzer.java` | 한글 추출 품질 점수와 상태 계산 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferencePdfIndexCandidateWriter.java` | candidate JSON과 원문 없는 summary JSON 작성 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferencePdfIndexCandidateGenerator.java` | PDFBox 추출, heading 후보 분리, 품질 진단 연결 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferencePdfIndexDiagnosticsTool.java` | CLI entry point |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferencePdfHeadingParserTest.java` | heading 파싱 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceTextQualityAnalyzerTest.java` | 품질 진단 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferencePdfIndexCandidateWriterTest.java` | JSON schema, hash, summary 원문 제외 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-pdf-index-diagnostics_report.md` | 실행 결과와 품질 판단 기록 |

## 구현 순서

1. workflow spec을 저장한다.
2. `workflow-spec-runner` 절차로 spec을 읽고 직접 실행 경로를 선택한다.
3. focused test 3개를 먼저 추가한다.
4. heading parser, quality analyzer, candidate writer를 구현해 focused test를 통과시킨다.
5. PDFBox 의존성, generator, CLI, Gradle JavaExec task를 추가한다.
6. `.gitignore`에 로컬 PDF 패턴을 추가한다.
7. focused test를 실행한다.
8. 전체 test를 실행한다.
9. 로컬 PDF가 있으면 수동 진단 task를 실행하고 summary 수치를 확인한다.
10. report를 작성한다.
11. 커밋 메시지 규칙에 맞춰 커밋하고 작업 브랜치에 push한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiReviewReferencePdfHeadingParserTest` | `19:8`, `19:11-15`, `20:1-26`, `19:42-20:6`을 장절 범위로 파싱 |
| `AiReviewReferencePdfHeadingParserTest` | 깨진 heading과 애매한 문자열은 invalid 처리 |
| `AiReviewReferenceTextQualityAnalyzerTest` | 정상 한글/영문 텍스트는 낮은 오류율과 `USABLE` 또는 `NEEDS_REVIEW` 판정 |
| `AiReviewReferenceTextQualityAnalyzerTest` | `�`, `?`, `占`, `竊` marker가 많은 텍스트는 `NEEDS_REVIEW` 또는 `UNUSABLE` 판정 |
| `AiReviewReferenceTextQualityAnalyzerTest` | 너무 짧은 본문과 너무 긴 본문은 사유 기록 |
| `AiReviewReferencePdfIndexCandidateWriterTest` | candidate JSON 최상위 schema와 entry 필수 필드 생성 |
| `AiReviewReferencePdfIndexCandidateWriterTest` | `referenceHash`가 `referenceText` sha256 기준으로 계산 |
| `AiReviewReferencePdfIndexCandidateWriterTest` | summary JSON에는 `referenceText` 원문이 포함되지 않음 |

## 수용 기준

- [ ] PDF 진단 task가 `--source`, `--output`, `--summary` 인자를 받아 실행된다.
- [ ] candidate JSON은 `ai-review-reference-index-candidate.v1` schemaVersion을 가진다.
- [ ] summary JSON과 report에는 추출 원문 `referenceText`가 포함되지 않는다.
- [ ] `sourceFileHash`와 `referenceHash`는 sha256으로 계산된다.
- [ ] heading parser focused test가 통과한다.
- [ ] quality analyzer focused test가 통과한다.
- [ ] candidate writer focused test가 통과한다.
- [ ] 전체 test가 통과한다.
- [ ] 로컬 PDF와 build 산출물이 Git stage에 포함되지 않는다.
- [ ] report가 작성된다.
- [ ] 커밋 후 작업 브랜치에 push된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- parser, analyzer, writer, generator가 같은 candidate schema record를 공유하므로 한 흐름에서 맞추는 편이 충돌 가능성이 낮다.
- 테스트와 구현이 `ai/internal` 안에서 서로 맞물려 있어 병렬 편집보다 직접 실행이 빠르다.
- workflow, report, commit, push까지 순차 검증이 필요한 작업이다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow spec 기준으로 TDD, 구현, 검증, 수동 진단 실행, report, 커밋, push를 직접 수행한다.

## 검증 계획

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferencePdf*Test"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test
.\qtai-server\gradlew.bat --no-daemon -p qtai-server aiReviewReferencePdfIndexDiagnostics --args="--source ..\doc\TalkFile_IVP성경배경주석.pdf.pdf --output build\ai-review-reference\ivp-reference-index-candidate.json --summary build\ai-review-reference\ivp-reference-index-diagnostics.json"
```

전체 test가 환경 문제로 실패하면 focused test 결과와 실패 원인을 report에 기록한다. 로컬 PDF가 없으면 수동 진단 명령은 실행하지 않고 사유를 report에 기록한다.

## 후속 작업으로 남길 항목

- candidate JSON을 검수해 운영용 `ai-review-reference-index.v1`로 승격하는 변환/검수 절차
- bookCode 자동 확정 또는 수동 보정 기준
- restricted 저장소 배포
- layer 2 prompt에서 운영용 index excerpt를 사용하는 연결
