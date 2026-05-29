# Workflow - 2026-05-27 note-meditation-lifecycle

| 항목 | 내용 |
| --- | --- |
| 담당자 | 이지윤 |
| 협업/리뷰 | 김지민, 이승욱, 강태오(Lead) |
| 브랜치 | `feature/note-meditation-lifecycle` |
| PR 대상 | `dev` |
| 관련 F-ID | F-03, F-13 |
| 트리거 | QT 노트 `MEDITATION`의 `DRAFT`/`SAVED`/`DELETED` 상태 전이, 1일 1건 활성 제약, 묵상 달력 반영 기준을 구현 전에 명확히 고정한다. |
| 기준 문서 | `AGENTS.md`, `CODE_CONVENTION.md`, `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/05_시퀀스_다이어그램.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/23_도메인_용어사전.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `.github/workflows/qt-ai-ci.yml`, `.github/workflows/claude-pr-review.yml`, `.github/pull_request_template.md`, `.github/CODEOWNERS` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/note/**`, `qtai-server/src/test/java/com/qtai/domain/note/**`, `qtai-server/src/test/java/com/qtai/common/**`, `doc/workspaces/DevA_이지윤/reports/**` |

## 작업 목표

QT 노트 `MEDITATION` 카테고리의 생명주기를 `07_요구사항_정의서.md` F-03과 `03_아키텍처_정의서.md` §4.4 기준으로 확정한다. 사용자는 오늘의 QT 본문에 연결된 묵상 노트를 명시적인 버튼 동작으로만 임시저장하거나 저장 확정할 수 있고, 삭제 시 같은 QT 본문에 다시 작성할 수 있어야 한다.

이번 workflow는 자동 저장 금지, 기본 비공개, 작성자 본인 접근, `activeUniqueKey` 중복 제약, 삭제 후 재작성, 묵상 달력 집계 기준을 하나의 작업 단위로 검증한다. PR 자동 리뷰에서 BLOCK이 나올 수 있는 인접 부정 경로까지 함께 점검해 같은 public 메서드 안에서 반복 차단이 발생하지 않도록 한다.

## 범위

- `MEDITATION` 노트의 상태를 `DRAFT`, `SAVED`, `DELETED`로 한정한다.
- `DRAFT`와 `SAVED`는 모두 삭제되지 않은 활성 QT 노트로 보고 `activeUniqueKey='ACTIVE'`를 유지한다.
- `DELETED` 전이 시 `deletedAt`을 기록하고 `activeUniqueKey=NULL`, `savedAt=NULL`로 정리한다.
- 같은 `memberId + qtPassageId + category=MEDITATION + activeUniqueKey='ACTIVE'` 조합은 1건만 허용한다.
- `GET /api/v1/notes/draft?category=MEDITATION&qtPassageId=`는 작성자 본인의 `DRAFT`만 반환하고, 없으면 `exists=false`를 반환한다.
- `POST /api/v1/notes`는 `MEDITATION` 생성 시 `qtPassageId`를 필수로 요구하고 기본 `visibility=PRIVATE`를 적용한다.
- `PATCH /api/v1/notes/{noteId}`는 `DRAFT -> SAVED`, `SAVED -> DRAFT`, 본문/4개 섹션/구절 메타데이터 교체를 처리한다.
- `DELETE /api/v1/notes/{noteId}`는 작성자 본인만 수행하며, 이미 삭제된 본인 노트에 대한 반복 삭제는 멱등 성공으로 유지한다.
- `SAVED` 상태만 묵상 완료로 집계하고, `DRAFT` 또는 `DELETED`는 완료 집계에서 제외한다.
- `MEDITATION` 저장 가능 입력은 `title`, `body`, `rememberSection`, `interpretSection`, `applySection`, `praySection` 중 하나 이상 의미 있는 값이 있는 경우로 정의한다.
- `verseIds`는 `@`멘션 본문 자동 삽입 결과를 클라이언트가 전달하는 메타데이터로만 다루며, 서버가 본문 문자열을 파싱하지 않는다.
- 요청의 `verseIds`는 중복을 제거하고 첫 등장 순서대로 `note_verses.display_order`에 저장한다.
- `MEDITATION`의 `qtPassageId` 존재와 읽기 가능 여부는 `domain.qt.api` 또는 note 도메인의 `client/qt` 어댑터로 검증한다.
- `verseIds` 존재 여부는 `domain.bible.api.GetBibleVerseUseCase`로 검증하고, `domain.bible.internal` 직접 import는 금지한다.
- PR 본문에는 workflow 경로와 report 경로를 반드시 남긴다.

## 제외 범위

- `/api/v1/notes/{noteId}/share` 공유 스냅샷 생성 구현은 제외한다.
- `domain.sharing`의 공유글 상태 변경, 댓글, 좋아요, 신고 처리는 제외한다.
- 묵상 달력 API 전체 구현은 제외한다. 단, `SAVED`/`DRAFT`/`DELETED` 집계가 가능하도록 노트 상태 필드는 검증한다.
- 노트 로컬 캐시, 오프라인 큐잉, 기기 간 충돌 해결은 v1.1 범위로 분리한다.
- 서버의 `@`멘션 문자열 파싱, 성경 본문 자동 삽입, 성경 본문 seed/fixture 추가는 제외한다.
- AI가 노트 본문을 생성, 수정, 평가, 추천하는 기능은 금지한다.
- 관리자 노트 열람, 사용자 노트 검열 API는 제외한다.
- 자유 노트 `SERMON`, `PRAYER`, `REPENTANCE`, `GRATITUDE`의 전체 생명주기 재설계는 제외한다.

## 라이프사이클 계약

| 현재 상태 | 사용자 동작 | 다음 상태 | DB 필드 기준 | 응답 기준 |
| --- | --- | --- | --- | --- |
| 없음 | 임시저장 | `DRAFT` | `visibility=PRIVATE`, `activeUniqueKey='ACTIVE'`, `savedAt=NULL`, `deletedAt=NULL` | `201 Created`, `status=DRAFT` |
| 없음 | 저장 | `SAVED` | `visibility=PRIVATE`, `activeUniqueKey='ACTIVE'`, `savedAt=now`, `deletedAt=NULL` | `201 Created`, `status=SAVED` |
| `DRAFT` | 임시저장 | `DRAFT` | 본문/섹션/구절 교체, `savedAt=NULL` 유지 | `200 OK`, `status=DRAFT` |
| `DRAFT` | 저장 | `SAVED` | 본문/섹션/구절 교체, `savedAt=now` | `200 OK`, `status=SAVED` |
| `SAVED` | 임시저장 | `DRAFT` | 본문/섹션/구절 교체, `savedAt=NULL` | `200 OK`, `status=DRAFT` |
| `SAVED` | 저장 | `SAVED` | 본문/섹션/구절 교체, `savedAt=now` 갱신 | `200 OK`, `status=SAVED` |
| `DRAFT` 또는 `SAVED` | 삭제 | `DELETED` | `activeUniqueKey=NULL`, `savedAt=NULL`, `deletedAt=now` | `204 No Content` |
| `DELETED` | 삭제 재요청 | `DELETED` | 기존 `deletedAt` 유지 | `204 No Content` |

## 문서 충돌과 적용 판단

| 항목 | 판단 |
| --- | --- |
| `activeUniqueKey` 적용 상태 | `07_요구사항_정의서.md`의 "사용자별·날짜별 1건"과 `03_아키텍처_정의서.md`의 "`status != DELETED`인 활성 묵상 노트" 기준을 우선해 `DRAFT`와 `SAVED` 모두 `ACTIVE`로 본다. |
| `05_시퀀스_다이어그램.md`의 "저장 확정된 활성 QT 묵상 노트만 ACTIVE" 표현 | 상위 문서와 충돌 가능성이 있어 문서 정합화가 필요하다. 구현은 `07` -> `03` 우선순위에 따라 진행하고, report에 Lead 검토 필요 항목으로 남긴다. |
| `MEDITATION verseIds` 범위 | `04_API_명세서.md` §4.3.6은 QT 범위 안으로 제한한다고 쓰지만, `@`멘션은 다른 구절 인용을 허용한다. 이번 workflow에서는 `qtPassageId`는 QT 본문 검증용, `verseIds`는 인용 메타데이터로 분리하고, 범위 제한 재확정은 report에 Lead 검토 필요 항목으로 남긴다. |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/Note.java` | `MEDITATION` 상태 전이, `savedAt`, `deletedAt`, `activeUniqueKey` 변경 규칙을 도메인 메서드로 고정 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteService.java` | 생성/수정/삭제 입력 검증, 작성자 검증, 중복 검증, 구절 메타데이터 교체, 트랜잭션 경계 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteRepository.java` | 활성 중복 조회, draft 조회, 작성자 활성 조회, 삭제 제외 목록 조회 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteVerseRepository.java` | note별 구절 삭제 후 재삽입과 정렬 조회 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/web/CreateNoteRequest.java` | `MEDITATION` 생성 입력을 command로 변환하고 Bean Validation 누락 방지 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/web/UpdateNoteRequest.java` | 상태 전이와 4개 섹션 입력을 command로 변환 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/web/NoteController.java` | 인증 주체 null 가드, UseCase 위임, HTTP status 유지 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/api/**` | UseCase와 DTO가 `web`/`internal` 타입을 노출하지 않도록 유지 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | 정상 경로와 인접 부정 경로 단위 테스트 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/note/web/NoteControllerTest.java` | 인증, 위임, status code, envelope 검증 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteRepositoryIntegrationTest.java` | 삭제 제외, draft 조회, active unique 제약 검증 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteVerseRepositoryTest.java` | 구절 교체와 display order 검증 |
| Modify | `qtai-server/src/test/java/com/qtai/common/JpaEntityDdlTest.java` | `notes` unique 제약과 `active_unique_key` DDL 정합성 |
| Modify/Create | `doc/workspaces/DevA_이지윤/reports/2026-05-27_note-meditation-lifecycle_report.md` | 구현 결과, BLOCK 예방 점검, 검증 결과, Lead 검토 항목 기록 |

## 구현 순서

1. 현재 브랜치가 `feature/note-meditation-lifecycle`인지 확인하고, 다르면 작업 시작 전 전환 여부를 사용자에게 확인한다.
2. `git status --short`로 미커밋 변경을 확인하고, 사용자가 만든 변경은 되돌리지 않는다.
3. `07_요구사항_정의서.md` §6.4, §18.2, §19와 `03_아키텍처_정의서.md` §4.4, §11.2, §13.1을 다시 확인한다.
4. `Note`, `NoteService`, `NoteRepository`, `NoteController`, 기존 테스트에서 `MEDITATION` 상태 전이와 중복 제약이 이미 충족되는지 목록화한다.
5. `Note.create`와 `Note.update`에서 `MEDITATION`의 `activeUniqueKey`가 `DRAFT`/`SAVED` 모두 `ACTIVE`, `DELETED`는 `NULL`이 되도록 검증한다.
6. `Note.delete`가 `status=DELETED`, `savedAt=NULL`, `activeUniqueKey=NULL`, `deletedAt=now`를 한 번에 처리하는지 확인한다.
7. `NoteService.create`에서 `MEDITATION` 생성 시 `qtPassageId` 필수, `noteQtClient.validateReadable(memberId, qtPassageId)` 호출, 활성 중복 검사 순서를 고정한다.
8. `NoteService.update`에서 미존재 노트, 타 사용자 노트, 삭제 노트, 중복 `MEDITATION`, 자유 노트에 `qtPassageId` 전달 케이스를 모두 차단한다.
9. `MEDITATION` 입력이 `title/body`가 비어 있어도 4개 섹션 중 하나가 있으면 저장 가능한지 확인하고, 현재 코드가 막고 있으면 normalization을 보정한다.
10. `SAVED -> DRAFT` 전이 시 `savedAt=NULL`이 되어 묵상 달력 완료 집계에서 제외되는지 확인한다.
11. `delete`는 본인 삭제 노트 재요청만 멱등 성공으로 유지하고, 미존재 또는 타 사용자 노트는 각각 `NOTE_NOT_FOUND`, `FORBIDDEN`으로 유지한다.
12. `replaceNoteVerses`는 `verseIds`가 비어 있어도 기존 연결을 삭제하고 빈 목록으로 저장되는지 확인한다.
13. `verseIds` 중복 제거는 첫 등장 순서를 유지하고, 존재하지 않는 절이 있으면 `BIBLE_VERSE_NOT_FOUND`로 저장 전체를 중단한다.
14. `NoteController`는 모든 엔드포인트에서 `@AuthenticationPrincipal Long memberId`가 null이면 `UNAUTHORIZED`를 던지고 Repository를 직접 호출하지 않는다.
15. `domain.note` 코드에서 `domain.bible.internal`, `domain.qt.internal`, `domain.sharing.internal`, 각 도메인 `web` import가 없는지 확인한다.
16. 운영 코드 변경이 필요한 경우 최소 범위로 수정하고, 관련 없는 WARN/INFO 개선이나 포맷팅은 하지 않는다.
17. 변경 후 `doc/workspaces/DevA_이지윤/reports/2026-05-27_note-meditation-lifecycle_report.md`에 원인, 수정 내용, 검증 결과, Lead 검토 항목을 기록한다.
18. PR 본문에는 workflow와 report 경로를 포함하고, CODEOWNERS 기준 note 도메인 owner를 확인했다고 적는다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | `MEDITATION` `DRAFT` 생성 성공, `SAVED` 생성 성공, `DRAFT -> SAVED`, `SAVED -> DRAFT`, `SAVED -> SAVED`, 삭제 시 `activeUniqueKey`/`savedAt` 정리 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | `title/body` 없이 4개 섹션 중 하나만 있는 `MEDITATION` 저장 성공 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | `MEDITATION` `qtPassageId` 누락, 미존재 QT 또는 읽기 불가 QT, 활성 중복, `status=DELETED` 요청 차단 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | 타 사용자 수정/삭제 `FORBIDDEN`, 미존재 노트 `NOTE_NOT_FOUND`, 삭제 노트 수정 `INVALID_STATUS_TRANSITION`, 삭제 노트 재삭제 멱등 성공 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | `verseIds` 중복 제거, 빈 배열 교체, 존재하지 않는 절 `BIBLE_VERSE_NOT_FOUND`, 구절 저장 순서 보존 |
| `qtai-server/src/test/java/com/qtai/domain/note/web/NoteControllerTest.java` | 생성 `201`, 수정 `200`, 삭제 `204`, draft 조회 `200`, 인증 주체 null `UNAUTHORIZED`, UseCase 위임 인자 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteRepositoryIntegrationTest.java` | `findDraft`가 `DRAFT`만 반환하고 `SAVED`/`DELETED`를 반환하지 않음 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteRepositoryIntegrationTest.java` | `memberId + qtPassageId + activeUniqueKey` unique 제약으로 활성 `MEDITATION` 중복 삽입 실패 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteRepositoryIntegrationTest.java` | `DELETED` 처리 후 같은 `memberId + qtPassageId`로 새 `MEDITATION` 생성 가능 |
| `qtai-server/src/test/java/com/qtai/common/JpaEntityDdlTest.java` | `notes.active_unique_key` nullable, unique 제약, `deleted_at`, `saved_at` 컬럼 존재 |
| `qtai-server/src/test/java/com/qtai/common/ArchitectureBoundaryTest.java` | note 도메인의 타 도메인 `internal`/`web` 직접 import 금지 |

## BLOCK 예방 점검

| 점검 축 | 확인할 인접 경로 |
| --- | --- |
| 정상 경로 | 신규 `DRAFT`, 신규 `SAVED`, 기존 `DRAFT` 저장 확정, 기존 `SAVED` 재저장 |
| 미존재 리소스 | 없는 noteId, 없는 qtPassageId, 없는 bibleVerseId |
| 타 사용자/권한 없음 | 다른 회원의 noteId 조회, 수정, 삭제 |
| 삭제/비활성 상태 | `DELETED` 조회 제외, 수정 차단, 삭제 재요청 멱등 성공 |
| 중복/멱등성 | 활성 `MEDITATION` 중복 생성 차단, 삭제 후 재작성 허용, 같은 verseId 중복 제거 |
| 잘못된 입력값 | `MEDITATION`의 `qtPassageId` 누락, 자유 노트의 `qtPassageId` 전달, `status=DELETED` 요청, 빈 본문/빈 섹션 |
| 상태 전이 실패 | 삭제 노트 수정, 중복 활성 노트로의 수정, 존재하지 않는 구절 포함 저장 |
| 프로젝트 guardrail | 자동 저장 없음, AI 노트 생성 없음, 금지 번역본/본문 데이터 없음, `javax.*` 없음, Controller Repository 직접 호출 없음 |

## 수용 기준

- [ ] `MEDITATION` `DRAFT` 생성 시 `activeUniqueKey='ACTIVE'`, `savedAt=NULL`, `visibility=PRIVATE`가 된다.
- [ ] `MEDITATION` `SAVED` 생성 시 `activeUniqueKey='ACTIVE'`, `savedAt`이 기록된다.
- [ ] `DRAFT -> SAVED` 전이에서 본문/4개 섹션/구절 메타데이터가 교체되고 `savedAt`이 기록된다.
- [ ] `SAVED -> DRAFT` 전이에서 `savedAt=NULL`이 되어 묵상 완료 집계 대상에서 제외된다.
- [ ] 삭제 시 `status=DELETED`, `deletedAt` 기록, `savedAt=NULL`, `activeUniqueKey=NULL`이 함께 반영된다.
- [ ] 삭제된 `MEDITATION` 이후 같은 사용자와 같은 `qtPassageId`로 새 노트를 생성할 수 있다.
- [ ] 같은 사용자와 같은 `qtPassageId`에 `DRAFT` 또는 `SAVED` 활성 `MEDITATION`이 있으면 추가 생성이 `DUPLICATE_NOTE`로 차단된다.
- [ ] 타 사용자 노트 접근은 `FORBIDDEN` 또는 작성자 조회 API 기준 `NOTE_NOT_FOUND`로 차단된다.
- [ ] 삭제된 노트 수정은 `INVALID_STATUS_TRANSITION`으로 차단된다.
- [ ] 본인 삭제 노트 삭제 재요청은 예외 없이 `204 No Content`로 끝난다.
- [ ] `MEDITATION`은 `title`, `body`, 4개 섹션 중 하나 이상 입력되면 저장 가능하다.
- [ ] `verseIds`는 중복 없이 첫 등장 순서로 저장되고, 존재하지 않는 절이 포함되면 저장되지 않는다.
- [ ] 서버는 `@`멘션 문자열을 파싱하지 않고 요청의 `verseIds`만 메타데이터로 동기화한다.
- [ ] Controller는 Repository를 직접 호출하지 않는다.
- [ ] `domain.note`는 다른 도메인의 `internal`, `web` 타입을 import하지 않는다.
- [ ] 테스트와 예시에 실제 성경 본문, 금지 번역본, plain secret/token/password/private key 예시가 없다.
- [ ] PR 본문에 `doc/workspaces/DevA_이지윤/workflows/2026-05-27_note-meditation-lifecycle.md`와 report 경로가 포함된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 대상이 `NoteService`, `Note`, Repository, Controller 테스트의 같은 생명주기 규칙에 강하게 연결되어 있다.
- `activeUniqueKey`, `savedAt`, `deletedAt`, `DRAFT/SAVED` 집계 기준은 한 사람이 일관되게 확인해야 재작업이 줄어든다.
- BLOCK 예방 테스트도 같은 public 메서드의 인접 부정 경로를 따라가야 하므로 병렬 분리보다 순차 검증이 안전하다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 코드 변경, 테스트 보강, report 작성, 최종 검증을 직접 수행한다.

## 검증 계획

- `git diff --check`
- `.\gradlew.bat test --tests "*NoteServiceTest"`
- `.\gradlew.bat test --tests "*NoteControllerTest"`
- `.\gradlew.bat test --tests "*NoteRepositoryIntegrationTest"`
- `.\gradlew.bat test --tests "*NoteVerseRepositoryTest"`
- `.\gradlew.bat test --tests "*JpaEntityDdlTest"`
- `.\gradlew.bat test --tests "*ArchitectureBoundaryTest"`
- `.\gradlew.bat test --tests "*Note*" --tests "*JpaEntityDdlTest" --tests "*ArchitectureBoundaryTest"`
- `.\gradlew.bat build`
- `.\gradlew.bat test jacocoTestReport`
- `.\gradlew.bat jacocoTestCoverageVerification`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`
- `rg -n "com\\.qtai\\.domain\\.(bible|qt|sharing)\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/note --glob "*.java"`
- `rg -n "개역개정|ESV|NIV|성서유니온|두란노|plain secret|password|private key|/ai/sessions|SseEmitter|text/event-stream|KafkaTemplate|spring-kafka|VectorStore|EmbeddingStore" qtai-server/src/main qtai-server/src/test doc/workspaces/DevA_이지윤/workflows/2026-05-27_note-meditation-lifecycle.md`

`jacocoTestReport`, `jacocoTestCoverageVerification`, `spectral`, `gitleaks`가 로컬 환경에서 실행 불가하면 실행 불가 사유를 report와 최종 응답에 남기고, CI의 `qt-ai-ci.yml` 게이트에서 다시 확인한다.

## PR/커밋 기준

- 브랜치는 `dev`에서 `feature/note-meditation-lifecycle`로 생성한다.
- 커밋 메시지는 Conventional Commits 형식을 사용한다. 예: `fix(note): harden meditation note lifecycle`.
- PR 대상은 `dev`로 둔다.
- PR 본문에는 관련 F-ID `F-03`, `F-13`, 기준 문서 섹션, workflow/report 경로를 남긴다.
- note 도메인 CODEOWNERS는 `@ij447504-source`, `@rmfdnjf98`, `@LeeSeung-Wook`, `@Tae0072`임을 확인한다.
- Claude 자동 리뷰가 `APPROVE`이고 CI 전체가 success이면 squash merge 대상이 된다.
- BLOCK이 발생하면 지적 항목만 고치지 않고 같은 public 메서드의 인접 부정 경로를 함께 점검한다.
- push는 사용자가 요청한 경우에만 수행한다.

## 후속 작업으로 남길 항목

- `doc/프로젝트 문서/05_시퀀스_다이어그램.md`의 `active_unique_key` 설명 문구 정합화 Lead 검토
- `04_API_명세서.md` §4.3.6의 `MEDITATION verseIds` 범위 제한 문구 재확정
- `GET /api/v1/me/meditation-calendar` 본문 구현
- `domain.sharing` 소유의 공유 스냅샷 생성과 원본 삭제 시각 반영
- v1.1 노트 로컬 캐시, 오프라인 큐잉, 충돌 해결 정책
