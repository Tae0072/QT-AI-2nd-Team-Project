# Report - 2026-05-27 note-meditation-lifecycle

작성 시간: 2026-05-27 오후 04:31

## 작업 요약

`note-meditation-lifecycle` workflow 기준으로 QT 묵상 노트(`MEDITATION`)의 `DRAFT`/`SAVED`/`DELETED` 생명주기, 활성 중복 제약, 삭제 후 재작성 가능성, 도메인 경계 import 상태를 점검했다.

관련 F-ID는 F-03, F-13이며, 기준 workflow는 `doc/workspaces/DevA_이지윤/workflows/2026-05-27_note-meditation-lifecycle.md`다.

## 확인한 주요 파일

- `qtai-server/src/main/java/com/qtai/domain/note/internal/Note.java`
- `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteService.java`
- `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteRepository.java`
- `qtai-server/src/main/java/com/qtai/domain/note/web/NoteController.java`
- `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java`
- `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteRepositoryIntegrationTest.java`
- `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteVerseRepositoryTest.java`
- `qtai-server/src/test/java/com/qtai/domain/note/web/NoteControllerTest.java`
- `qtai-server/src/test/java/com/qtai/common/JpaEntityDdlTest.java`

## 반영 상태

- `Note`는 `MEDITATION` 생성/수정 시 `activeUniqueKey='ACTIVE'`를 부여하고, `delete()`에서 `status=DELETED`, `savedAt=null`, `activeUniqueKey=null`, `deletedAt=now`를 함께 반영한다.
- `transitionTo()`는 `SAVED` 전환 시 `savedAt`을 기록하고, `DRAFT` 전환 시 `savedAt`을 비운다.
- `NoteRepository`는 `memberId + qtPassageId + activeUniqueKey` unique 제약을 엔티티에 선언하고, 활성 `MEDITATION` 중복 조회 메서드를 제공한다.
- `NoteService.create()`/`update()`는 `MEDITATION` 저장 전 `qtPassageId` 필수, QT 읽기 가능 여부, 활성 중복 여부를 검증한다.
- `getDraft()`는 `category=MEDITATION`과 `qtPassageId`가 있는 경우에만 draft 조회를 허용한다.
- `delete()`는 본인 노트만 처리하고, 이미 삭제된 노트는 예외 없이 멱등 반환한다.
- `replaceNoteVerses()`는 기존 연결을 삭제한 뒤 요청 `verseIds`를 중복 제거된 순서대로 다시 저장한다.
- Controller는 Repository를 직접 호출하지 않고 UseCase만 호출하며, `memberId`가 없으면 `UNAUTHORIZED`를 반환하도록 공통 검증을 둔다.

## 테스트 보강 상태

- `NoteServiceTest`에 묵상 노트 중복 차단, 생성/수정 상태 전환, `SAVED -> DRAFT` 시 `savedAt` 정리, 삭제 시 `DELETED` 전환, 타 사용자 접근 차단, 삭제 노트 멱등 삭제, `verseIds` 중복 제거/존재 검증 경로가 포함되어 있다.
- `NoteControllerTest`에 draft/detail/create/update/delete UseCase 위임과 인증 주체 null 차단 경로가 포함되어 있다.
- `NoteRepositoryIntegrationTest`에 삭제 노트 조회 제외, status 필터, 검색/정렬, draft 조회 관련 검증이 포함되어 있다.
- `JpaEntityDdlTest`에 `Note MEDITATION` unique 제약 검증이 포함되어 있다.

## 잔여 리스크

- workflow 수용 기준은 `MEDITATION` 입력을 `title`, `body`, `rememberSection`, `interpretSection`, `applySection`, `praySection` 중 하나 이상으로 정의했지만, 현재 `NoteService.normalize()`는 `title`과 `body`가 모두 비어 있으면 section 값이 있어도 `INVALID_INPUT`으로 차단한다. 이 기준을 유지하려면 section 값까지 포함해 빈 입력 여부를 판단하도록 보정이 필요하다.
- workflow의 `SERMON`, `PRAYER`, `REPENTANCE`, `GRATITUDE` 전체 생명주기 재설계는 제외 범위로 남아 있다.
- `GET /api/v1/me/meditation-calendar`, sharing 연동, 오프라인/충돌 처리, note 로컬 캐시는 후속 작업 범위다.
- `05_시퀀스_다이어그램.md`의 `active_unique_key` 설명과 `04_API_명세서.md`의 `MEDITATION verseIds` 범위 문구는 Lead 검토 후 문서 정합화가 필요하다.

## 검증 결과

성공:

```bash
rg -n "com\\.qtai\\.domain\\.(bible|qt|sharing)\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/note --glob "*.java"
```

- 매치 없음. note 도메인의 타 도메인 `internal`/`web` 직접 import는 발견되지 않았다.

성공:

```bash
rg -n "javax\\.|개역개정|ESV|NIV|성서유니온|두란노|plain secret|password|private key|/ai/sessions|SseEmitter|text/event-stream|KafkaTemplate|spring-kafka|VectorStore|EmbeddingStore" qtai-server/src/main/java/com/qtai/domain/note qtai-server/src/test/java/com/qtai/domain/note qtai-server/src/test/java/com/qtai/common
```

- 매치 없음. 점검 범위에서 금지 import, 금지 번역본/본문 데이터, plain secret 예시는 발견되지 않았다.

이전 확인:

```bash
.\gradlew.bat compileJava
```

- BUILD SUCCESSFUL.

## 미실행 검증

- `.\gradlew.bat test --tests "*NoteServiceTest"`
- `.\gradlew.bat test --tests "*NoteControllerTest"`
- `.\gradlew.bat test --tests "*NoteRepositoryIntegrationTest"`
- `.\gradlew.bat test --tests "*NoteVerseRepositoryTest"`
- `.\gradlew.bat test --tests "*JpaEntityDdlTest"`
- `.\gradlew.bat test --tests "*ArchitectureBoundaryTest"`
- `.\gradlew.bat build`
- `.\gradlew.bat test jacocoTestReport`
- `.\gradlew.bat jacocoTestCoverageVerification`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`

사유:

- 이번 요청은 보고서 작성 범위였고, Gradle test 계열 명령은 별도 승인 없이 실행하지 않았다.
- `jacocoTestReport`, `jacocoTestCoverageVerification`, Spectral, gitleaks는 기존 보고서 기준으로 로컬 task/도구 부재 가능성이 있어 이번 보고서 작성 단계에서는 재시도하지 않았다.

## 결론

묵상 노트의 핵심 생명주기 구현은 workflow의 큰 방향과 일치한다. 다만 section-only 묵상 저장 허용 기준은 현재 코드와 불일치하므로, PR 제출 전 수정하거나 workflow 수용 기준을 조정해야 한다.
