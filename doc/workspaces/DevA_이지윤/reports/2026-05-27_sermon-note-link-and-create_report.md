# Report - 2026-05-27 sermon-note-link-and-create

## 작업 요약

- `POST /api/v1/notes`의 `category=SERMON` 생성 흐름을 구현했다.
- 설교 노트는 `visibility=PRIVATE`, `qtPassageId=null`, `activeUniqueKey=null` 정책으로 저장되도록 했다.
- 요청 `verseIds`는 첫 등장 순서 기준으로 중복 제거 후 `note_verses.display_order`에 저장되도록 했다.
- Bible 공개 UseCase로 선택 구절 존재 여부를 검증하고, 존재하지 않는 구절이 있으면 노트와 연결 구절을 저장하지 않도록 했다.

## 변경 파일

- `qtai-server/src/main/java/com/qtai/domain/note/api/CreateNoteUseCase.java`
- `qtai-server/src/main/java/com/qtai/domain/note/api/dto/NoteCreateRequest.java`
- `qtai-server/src/main/java/com/qtai/domain/note/api/dto/NoteResponse.java`
- `qtai-server/src/main/java/com/qtai/domain/note/internal/Note.java`
- `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteService.java`
- `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteVerse.java`
- `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteVerseRepository.java`
- `qtai-server/src/main/java/com/qtai/domain/note/web/NoteController.java`
- `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java`
- `qtai-server/src/test/java/com/qtai/domain/note/web/NoteControllerTest.java`

## 검증 결과

- `cd qtai-server && ./gradlew test --tests "*NoteServiceTest" --tests "*NoteControllerTest"` 성공
- `cd qtai-server && ./gradlew test --tests "*NoteRepositoryIntegrationTest" --tests "*JpaEntityDdlTest"` 성공
- `cd qtai-server && ./gradlew test` 성공
- `rg -n "^import .*domain\.[^.]+\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/note` 결과 없음
- `rg -n "Repository" qtai-server/src/main/java/com/qtai/domain/note/web` 결과 없음
- `git diff --check` 통과

## 남은 작업

- 설교 노트 수정 시 구절 교체 정책은 후속 PR에서 처리한다.
- 설교 노트 상세 조회에서 `note_verses`를 성경 좌표 응답으로 확장하는 작업은 후속 PR에서 처리한다.
- 설교 노트 공유 시 구절 스냅샷 저장은 공유 도메인 연동 PR에서 처리한다.
