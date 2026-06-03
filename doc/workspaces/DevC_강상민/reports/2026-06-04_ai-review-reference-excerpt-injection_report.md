# AI 검수 참조자료 범위 매칭 및 Prompt Excerpt 주입 리포트

- 작업일: 2026-06-04
- 작업 브랜치: `feature/ai-review-reference-excerpt-injection`
- workflow spec: `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-excerpt-injection.md`
- PR 대상: `dev`

## 구현 내용

- `AiReviewReferenceExcerptSelector`를 추가했다.
  - 산출물 `payloadJson.sourceMetadata.verses[]`의 `bookCode`, `chapterNo`, `verseNo`를 기준으로 index entry와 범위 매칭한다.
  - 같은 `bookCode`이고 산출물 verse가 entry 시작/종료 장절 범위 안에 있으면 선택한다.
  - index 순서를 유지하며 최대 3개 excerpt만 선택한다.
  - prompt용 `referenceText`는 각 1200자까지 제한한다.
- `AiReviewValidationService`에 index reader와 selector를 연결했다.
  - 최신 ACTIVE reference metadata가 있으면 index를 읽고 매칭 excerpt를 만든다.
  - 매칭 excerpt가 있으면 layer 2 LLM prompt의 `reference.excerpts[]`에 주입한다.
  - 매칭 실패, verse metadata 누락, index reader 오류는 LLM을 호출하지 않고 `ADVISOR/NEEDS_REVIEW` 로그로 남긴다.
- 저장 정책을 유지했다.
  - `referenceText` 원문은 LLM prompt에만 포함한다.
  - `checklistJson`에는 `selectedReferenceExcerptCount`, `selectedReferenceHashes`, `selectedReferenceRangeLabels`만 저장한다.

## 제외 범위

- 새 관리자 API/OpenAPI 변경 없음
- DB schema migration 없음
- index JSON 생성, PDF 파싱, OCR 구현 없음
- excerpt 요약/랭킹 모델 추가 없음
- `payloadJson`, `checklistJson`, 감사 로그에 `referenceText` 원문 저장 없음

## 검증 결과

- `.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceExcerptSelectorTest" --tests "*AiReviewValidationServiceTest"`
  - 결과: 성공
- `.\qtai-server\gradlew.bat --no-daemon -p qtai-server test`
  - 결과: 성공

## 참고

- Gradle 종료 후 Windows native file watcher에서 `error = 1784` 로그가 출력되었지만, Gradle task 결과는 `BUILD SUCCESSFUL`이다.
- 후속 작업은 운영 index 생성/배포 자동화와 layer 2 prompt 품질 평가 케이스 축적이다.
