# Report - 2026-05-27 note-domain-completion

## 작업 요약

`feature/note-core` 브랜치에서 Note 도메인 완성 workflow의 1차 구현 상태를 정리했다.

이번 작업은 기존 `GET /api/v1/notes` 목록 조회 중심 구현을 확장해, 노트 카테고리 조회, 임시 노트 조회, 노트 생성, 상세 조회, 수정, 삭제 흐름까지 `NoteService`와 `NoteController`에 연결하는 것을 목표로 한다.

## 관련 F-ID

- F-03 QT 묵상 노트
- F-13 자유 노트
- F-16 노트 관리

## 주요 변경 파일

- `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteService.java`
- `qtai-server/src/main/java/com/qtai/domain/note/web/NoteController.java`
- `qtai-server/src/main/java/com/qtai/domain/note/web/CreateNoteRequest.java`
- `qtai-server/src/main/java/com/qtai/domain/note/web/UpdateNoteRequest.java`
- `qtai-server/src/main/java/com/qtai/domain/note/web/NoteCategoryController.java`
- `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java`
- `qtai-server/src/test/java/com/qtai/domain/note/web/NoteControllerTest.java`
- `qtai-server/src/test/java/com/qtai/common/ArchitectureBoundaryTest.java`

## 구현 반영 내용

- `NoteService`가 `ListNotesUseCase` 외에 `GetNoteUseCase`, `CreateNoteUseCase`, `UpdateNoteUseCase`, `DeleteNoteUseCase`, `ListNoteCategoriesUseCase`를 구현하도록 확장했다.
- `GET /api/v1/notes/draft`, `GET /api/v1/notes/{noteId}`, `POST /api/v1/notes`, `PATCH /api/v1/notes/{noteId}`, `DELETE /api/v1/notes/{noteId}` 컨트롤러 엔드포인트를 연결했다.
- `GET /api/v1/note-categories` 전용 컨트롤러를 추가했다.
- 생성/수정 HTTP 요청 DTO를 `CreateNoteRequest`, `UpdateNoteRequest`로 분리하고 UseCase command로 변환하도록 구성했다.
- `MEDITATION` 노트는 QT passage 검증과 활성 노트 중복 검사를 수행하도록 했다.
- `SERMON` 노트는 `verseIds`가 비어 있으면 저장하지 않도록 검증했다.
- 자유 노트 계열은 `qtPassageId` 전달을 차단하고, `verseIds`가 있으면 `note_verses`에 교체 저장하도록 했다.
- `verseIds`는 null/빈 값 처리, 양수 검증, 요청 순서 유지 중복 제거를 거친다.
- 삭제는 물리 삭제가 아니라 `status=DELETED`, `deletedAt`, `activeUniqueKey=null`로 처리하도록 했다.
- 목록/상세 응답에서 고정 placeholder였던 `visibility`, `shared`, `savedAt` 등을 실제 모델 값 기반으로 매핑하도록 보강했다.
- `domain.note`가 `bible`, `qt`, `sharing`의 `internal`/`web` 패키지를 직접 import하지 않는지 확인하는 ArchitectureBoundaryTest 항목을 추가했다.

## 테스트 보강

- `NoteServiceTest`
  - 목록 응답의 실제 visibility 매핑
  - QT 묵상 노트 중복 생성 차단
  - 설교 노트의 구절 필수 검증
  - `verseIds` 중복 제거 및 순서 유지 저장
  - 임시 노트 미존재 응답
  - 삭제 노트 수정 차단
  - 소프트 삭제 상태 전환
- `NoteControllerTest`
  - 미인증 memberId 차단
  - 목록 조회 위임
  - 생성/수정/삭제 UseCase 위임
- `ArchitectureBoundaryTest`
  - note 도메인의 타 도메인 internal/web 직접 import 금지

## 검증 결과

성공:

```bash
git diff --check
```

결과:

- exit code 0
- 공백 오류 없음
- Git 경고로 일부 파일의 LF가 다음 Git 처리 시 CRLF로 바뀔 수 있다는 메시지만 출력됨

실패:

```bash
.\gradlew.bat test --tests "*NoteServiceTest"
```

결과:

- 저장소 루트에서 실행 시 `.\gradlew.bat` 파일을 찾지 못해 실패
- wrapper가 `qtai-server/gradlew.bat` 아래에 있어 실행 위치를 `qtai-server`로 변경해야 함

실패:

```bash
.\gradlew.bat test --tests "*NoteServiceTest"
```

실행 위치:

- `qtai-server`

결과:

- 전역 Gradle 캐시의 lock 파일 접근 거부로 실패
- `C:\Users\G\.gradle\wrapper\dists\gradle-9.5.1-bin\...\gradle-9.5.1-bin.zip.lck`

실패:

```bash
$env:GRADLE_USER_HOME='C:\Workspace\QT-AI-2nd-Team-Project\qtai-server\.gradle-local'; .\gradlew.bat test --tests "*NoteServiceTest"
```

결과:

- workspace 로컬 Gradle 캐시 사용으로 재시도했으나 Gradle zip unzip 단계에서 실패
- 생성된 `qtai-server/.gradle-local` 캐시는 작업 후 삭제함

## 실행하지 못한 검증 / 사유

- `./gradlew -p qtai-server build`
- `./gradlew -p qtai-server test jacocoTestReport`
- `./gradlew -p qtai-server jacocoTestCoverageVerification`
- `npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`

사유:

- 이번 보고서 작성 중에는 NoteService 단위 테스트를 우선 실행했으나 Gradle wrapper 캐시/압축 해제 환경 문제로 테스트 실행 단계에 도달하지 못했다.
- OpenAPI와 전체 품질 게이트 검증은 아직 수행하지 않았다.

## 남은 리스크 / 후속 작업

- 현재 변경은 OpenAPI 명세 반영 여부가 아직 확인되지 않았다.
- Controller path와 DTO 필드가 `qtai-server/apis/api-v1/openapi.yaml`의 Note 계약과 일치하는지 추가 확인이 필요하다.
- `NoteServiceTest`가 실제로 컴파일/통과하는지는 Gradle 환경 정상화 후 재검증해야 한다.
- `NoteCategoryController`에 대한 별도 컨트롤러 테스트가 아직 없다.
- 생성/수정 요청 DTO의 필수값 검증은 Service에서 처리하고 있으나, HTTP 레벨 validation 메시지/스키마와의 정합성 확인이 필요하다.
- `NoteService.toListItem`에서 목록 조회용 `noteVerses`를 미리 조회하지만 현재 응답 매핑에는 사용하지 않는다. 이후 `rangeLabel` 보강용인지, 불필요 조회인지 확인이 필요하다.
- 전체 build, coverage, Spectral, gitleaks 검증은 PR 전 재실행해야 한다.
