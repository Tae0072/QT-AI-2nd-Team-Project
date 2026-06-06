# Report - 2026-06-04 ai-review-reference-candidate-promotion

## 실행 기준

- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-candidate-promotion.md`
- 브랜치: `feature/ai-review-reference-candidate-promotion`
- PR 대상: `dev`
- 관련 F-ID: 해당 없음

## 구현 내용

- `ai-review-reference-book-section-map.v1` reader를 추가했다.
  - `sourceFileHash` 일치 여부를 검증한다.
  - page range 역전/겹침/필수 필드 누락을 거부한다.
- candidate promotion service를 추가했다.
  - 입력 candidate schema는 `ai-review-reference-index-candidate.v1`만 허용한다.
  - output schema는 `ai-review-reference-index.v1`로 생성한다.
  - `UNUSABLE` entry는 승격하지 않는다.
  - `pageStart`가 book section range에 포함되는 entry만 승격한다.
  - `bookCode`는 map에서 가져오고, `referenceRangeLabel`은 `<bookName> <detectedHeading>`로 재작성한다.
  - summary에는 `referenceText` 원문을 포함하지 않는다.
- CLI `AiReviewReferenceCandidatePromotionTool`과 Gradle task `aiReviewReferencePromoteCandidateIndex`를 추가했다.
- `.gitignore`를 보강해 candidate/promotion/reference-index 산출물이 Git에 올라가지 않게 했다.

## 검증 결과

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReference*Promotion*Test" --tests "*AiReviewReferenceBookSectionMapReaderTest"
```

- 결과: 성공

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test
```

- 결과: 성공

참고:

- Gradle 종료 후 Windows file watcher 경고가 출력됐지만, 테스트 명령은 모두 `BUILD SUCCESSFUL`로 종료됐다.

## 제외 범위 확인

- 실제 IVP 전체 book section map은 작성하지 않았다.
- `restricted/validation/index/reference-index.json` 배포는 하지 않았다.
- layer 2 prompt 연결은 변경하지 않았다.
- PDF 원본, candidate JSON, 운영용 reference-index JSON 산출물은 커밋하지 않는다.
- DB migration, OpenAPI, 관리자 API 변경은 없다.

## 후속 작업

- 사람이 PDF를 확인해 실제 IVP 전체 book section map을 작성한다.
- 승격 도구로 운영용 `reference-index.json`을 생성한다.
- 검수 완료된 운영용 index를 restricted 저장소에 배포한다.
- `validation_reference_jobs.indexStorageUri`가 배포된 운영용 index를 가리키도록 연결한다.
