# Report - 2026-05-27 note-domain-completion BLOCK test fix

작성 시간: 2026-05-27 PM 12:07

## 작업 배경

Claude 자동 코드 리뷰(v3.1)에서 Note 도메인 CRUD PR에 대해 다음 테스트 커버리지 항목이 BLOCK으로 지적되었다.

- `NoteService.update()` 정상 경로 테스트 누락
- `NoteService.getDraft()` 존재 케이스 테스트 누락

이번 작업은 위 BLOCK 항목만 좁게 해결하는 후속 수정이다.

## 변경 파일

- `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java`

## 반영 내용

### getDraft 존재 케이스

- `getDraft_existing_returnsDetail` 테스트를 추가했다.
- `noteRepository.findDraft()`가 임시 묵상 노트를 반환할 때 `exists=true`가 내려가는지 검증했다.
- 상세 응답의 `id`, `category`, `qtPassageId`, `status`, `verses` 매핑을 함께 확인했다.

### update 정상 경로

- `update_prayerNote_replacesFieldsStatusAndVerses` 테스트를 추가했다.
  - PRAYER 노트가 정상 수정되는지 검증했다.
  - 제목, 본문, remember section, 상태 전이(`DRAFT -> SAVED`), `savedAt` 기록을 확인했다.
  - `verseIds` 중복 제거와 요청 순서 기반 `note_verses` 교체를 `ArgumentCaptor`로 검증했다.
- `update_savedNoteToDraft_clearsSavedAt` 테스트를 추가했다.
  - `SAVED -> DRAFT` 상태 전이 시 `savedAt`이 `null`로 돌아가는지 검증했다.
- `update_meditationNote_keepsActiveUniqueKey` 테스트를 추가했다.
  - MEDITATION 노트 수정 성공 시 `activeUniqueKey=ACTIVE`가 유지되는지 검증했다.
  - `noteQtClient.validateReadable()` 호출과 중복 확인 repository 호출을 검증했다.

## 검증 결과

성공:

```bash
.\gradlew.bat test --tests "*NoteServiceTest"
```

- BUILD SUCCESSFUL

성공:

```bash
.\gradlew.bat test --tests "*Note*"
```

- BUILD SUCCESSFUL

성공:

```bash
.\gradlew.bat build
```

- BUILD SUCCESSFUL

성공:

```bash
rg -n "com\\.qtai\\.domain\\.(bible|qt|sharing)\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/note --glob "*.java"
```

- 매치 없음

성공:

```bash
rg -n "개역개정|ESV|NIV|성서유니온|두란노" qtai-server/src/main qtai-server/src/test qtai-server/apis/api-v1/openapi.yaml
```

- 매치 없음

참고:

- `.\gradlew.bat build`는 기본 샌드박스에서 사용자 홈 Gradle wrapper lock 접근 권한 문제로 1회 실패했고, 승인 후 샌드박스 밖에서 재실행해 성공했다.

## 남은 리뷰 항목

이번 수정은 사용자 요청에 따라 BLOCK 테스트 커버리지 항목만 처리했다. 리뷰에 남아 있던 WARN 항목은 별도 판단/작업 대상으로 남긴다.
