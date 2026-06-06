# Report - 2026-06-04 ai-review-reference-book-section-map-candidate

## 작업 개요

- workflow spec: `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-book-section-map-candidate.md`
- 브랜치: `feature/ai-review-reference-book-section-map-candidate`
- PR 대상: `dev`
- 커밋 메시지: `feat(ai): 검수 참조자료 book section map 후보 생성 도구를 추가`

PDF에서 성경 66권 책 제목 위치를 자동 탐지해 검토용 `ai-review-reference-book-section-map-candidate.v1` 후보 JSON과 summary JSON을 생성하는 CLI를 추가했다. 이 결과는 운영용 최종 `ai-review-reference-book-section-map.v1`이 아니며, 사람이 summary를 보고 최종 map으로 정리하기 전 단계의 보조 산출물이다.

## 구현 내용

- `aiReviewReferenceBookSectionMapCandidate` Gradle task를 추가했다.
- PDFBox로 페이지별 텍스트를 읽고, 책 제목 라인을 기준으로 section 시작 페이지 후보를 탐지한다.
- 한 페이지에서 책 제목이 과도하게 많이 탐지되면 목차성 페이지로 보고 section 후보에서 제외한다.
- 탐지된 책 시작 페이지를 기준으로 `pageStart/pageEnd`를 계산한다.
- 후보 section에는 `bookCode`, `bookName`, `pageStart`, `pageEnd`, `detectedTitle`, `confidence`, `reasons`만 저장한다.
- summary에는 탐지 수, HIGH/LOW 수, 누락 책, 중복 탐지 수만 저장하고 PDF 본문/추출 원문/긴 excerpt는 저장하지 않는다.

## 수동 CLI 실행 결과

실행 명령:

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server aiReviewReferenceBookSectionMapCandidate --args="--source ..\doc\TalkFile_IVP성경배경주석.pdf.pdf --output build\ai-review-reference\ivp-book-section-map-candidate.json --summary build\ai-review-reference\ivp-book-section-map-candidate-summary.json"
```

요약:

- totalBookCount: 66
- detectedSectionCount: 64
- highConfidenceSectionCount: 39
- lowConfidenceSectionCount: 25
- missingBookCount: 2
- duplicateDetectionCount: 1284
- missingBooks: `JOB(욥기)`, `OBA(오바댜)`

생성된 candidate/summary JSON은 `qtai-server/build/**` 아래 산출물이며 Git에 커밋하지 않는다.

## 검증 결과

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceBookSection*Test"
```

- 결과: 성공

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test
```

- 결과: 성공

## 제외 범위

- 운영용 `ai-review-reference-book-section-map.v1` 최종 파일 작성
- promotion CLI로 실제 운영 `reference-index.json` 생성
- `restricted/validation/index/reference-index.json` 배포
- layer 2 prompt, DB, OpenAPI, 관리자 API 변경
- PDF 원본과 생성 JSON 산출물 커밋

## 후속 작업

- candidate summary를 보고 `LOW` section과 누락 책 `JOB`, `OBA`를 사람이 확인한다.
- 검토가 끝난 page range를 최종 `ai-review-reference-book-section-map.v1`로 작성한다.
- promotion CLI로 운영용 `reference-index.json`을 생성한다.
- 생성된 운영 index를 restricted storage에 배포하고 `validation_reference_jobs.indexStorageUri`와 연결한다.
