# Report - 2026-05-27 note delete/update negative paths

작성 시간: 2026-05-27 오후 12:28

## 작업 배경

Claude 자동 코드 리뷰(v3.1)에서 Note 도메인 테스트 커버리지 기준으로 다음 BLOCK 항목이 지적되었다.

- `NoteService.delete()` 부정 경로 테스트 누락
  - `noteId` 미존재 -> `NOTE_NOT_FOUND`
  - 타 사용자 노트 -> `FORBIDDEN`
  - 이미 삭제된 노트 -> 예외 없이 반환하는 멱등 처리
- `NoteService.update()` 미존재 노트 부정 경로 테스트 누락
  - `findById()` 결과 없음 -> `NOTE_NOT_FOUND`

이번 작업은 사용자 요청에 따라 위 BLOCK 항목만 좁게 해결했다.

## 변경 파일

- `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java`

## 반영 내용

- `update_missingNote_rejected` 테스트를 추가했다.
  - `noteRepository.findById()`가 빈 값을 반환하면 `NOTE_NOT_FOUND`를 검증한다.
- `delete_missingNote_rejected` 테스트를 추가했다.
  - 삭제 대상 노트가 없으면 `NOTE_NOT_FOUND`를 검증한다.
- `delete_otherMemberNote_rejected` 테스트를 추가했다.
  - 타 사용자 노트 삭제 시 `FORBIDDEN`을 검증한다.
- `delete_alreadyDeletedNote_returnsWithoutException` 테스트를 추가했다.
  - 이미 삭제된 노트는 예외 없이 반환하고 삭제 상태가 유지되는지 검증한다.

## 검증 결과

성공:

```bash
.\gradlew.bat test --tests com.qtai.domain.note.internal.NoteServiceTest
```

- BUILD SUCCESSFUL

성공:

```bash
.\gradlew.bat test
```

- BUILD SUCCESSFUL

성공:

```bash
.\gradlew.bat build
```

- BUILD SUCCESSFUL

성공:

```bash
git diff --check
```

- 공백 오류 없음. CRLF 변환 안내 warning만 출력.

## 실행 불가 검증

- `jacocoTestReport`, `jacocoTestCoverageVerification`: 현재 `qtai-server` Gradle 프로젝트에 해당 task가 없다.
- `gitleaks`: 로컬에 `gitleaks` 명령이 설치되어 있지 않다.
- `spectral`: 이번 변경은 OpenAPI 파일을 수정하지 않았고, 현재 브랜치에서 `apis` 대상 스펙 파일을 찾지 못했다.

## 남은 리뷰 항목

이번 수정은 BLOCK 테스트 커버리지 항목만 처리했다. 기존 리뷰의 WARN/INFO 항목은 사용자 요청 범위 밖이므로 수정하지 않았다.
