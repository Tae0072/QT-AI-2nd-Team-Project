# AI 검수 참조자료 Local Index Reader 구현 리포트

- 작업일: 2026-06-04
- 작업 브랜치: `feature/ai-review-reference-index-reader`
- workflow spec: `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-index-reader.md`
- PR 대상: `dev`

## 구현 내용

- `qtai.validation.restricted-storage-root` 설정을 추가했다.
  - 기본값: `${QTAI_RESTRICTED_STORAGE_ROOT:./restricted}`
  - `restricted://validation/index/reference-index.json`은 설정 root 하위의 `validation/index/reference-index.json`로 해석한다.
- `RestrictedStorageUriResolver`를 추가했다.
  - 허용 scheme은 `restricted`만 사용한다.
  - 빈 URI, 다른 scheme, path traversal(`.`/`..`), 역슬래시 경로를 거부한다.
  - 최종 resolved path가 restricted storage root 밖으로 벗어나면 거부한다.
- `AiReviewReferenceIndexReader`를 추가했다.
  - `indexStorageUri`가 가리키는 JSON 파일을 UTF-8로 읽는다.
  - `schemaVersion = ai-review-reference-index.v1`만 허용한다.
  - index의 `sourceFileHash`가 호출자가 전달한 기준 hash와 같아야 한다.
  - `entries`는 비어 있으면 안 된다.
  - entry 필수 필드(`bookCode`, 시작/종료 장절, `referenceRangeLabel`, `referenceText`, `referenceHash`)를 검증한다.
  - 읽기 실패, URI 오류, JSON 계약 위반은 `BusinessException(ErrorCode.INTERNAL_ERROR, AI_REVIEW_REFERENCE_INDEX_*)`로 실패시킨다.
- 테스트를 추가했다.
  - `RestrictedStorageUriResolverTest`
  - `AiReviewReferenceIndexReaderTest`

## 제외 범위

- PDF 원문 파싱은 구현하지 않았다.
- 성경 범위 매칭은 구현하지 않았다.
- layer 2 prompt에 `referenceText` excerpt를 주입하는 흐름은 구현하지 않았다.
- `referenceText`를 DB 로그나 감사 로그에 저장하는 처리는 추가하지 않았다.
- 관리자 API/OpenAPI 변경은 없다.

## 검증 결과

- `.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*RestrictedStorageUriResolverTest" --tests "*AiReviewReferenceIndexReaderTest"`
  - 결과: 성공
- `.\qtai-server\gradlew.bat --no-daemon -p qtai-server test`
  - 결과: 성공

## 참고

- Gradle 테스트 종료 후 Windows native file watcher에서 `error = 1784` 로그가 출력되었지만, Gradle task 결과는 `BUILD SUCCESSFUL`이다.
- 후속 작업은 Reader가 반환한 `ReferenceIndexEntry`를 성경 범위 matcher와 연결하고, layer 2 검수 AI prompt에 필요한 excerpt만 주입하는 것이다.
