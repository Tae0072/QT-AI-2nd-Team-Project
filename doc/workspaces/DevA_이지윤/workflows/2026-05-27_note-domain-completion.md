# Workflow - 2026-05-27 note-domain-completion

| 항목 | 내용 |
| --- | --- |
| 담당자 | 이지윤 |
| 협업/리뷰 | 이승욱(Note 도메인 소유), 강태오(Lead) |
| 브랜치 | `feature/note-domain-completion` |
| PR 대상 | `dev` |
| 관련 F-ID | F-03, F-13, F-16 |
| 트리거 | Note 도메인이 `GET /api/v1/notes` 목록 조회까지만 본문 구현되어 있어, v1 시연에 필요한 노트 카테고리, 임시 노트, 생성, 상세, 수정, 삭제, `note_verses` 동기화, OpenAPI 계약을 한 작업 단위로 고정한다. |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/05_시퀀스_다이어그램.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/23_도메인_용어사전.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `qtai-server/02_ERD_문서.md`, `CODE_CONVENTION.md` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/note/**`, `qtai-server/src/test/java/com/qtai/domain/note/**`, `qtai-server/src/main/resources/db/migration/**`, `qtai-server/apis/api-v1/openapi.yaml` |

## 작업 목표

Note 도메인을 `04_API_명세서.md` §4.3.1~§4.3.7과 `07_요구사항_정의서.md` F-03/F-16 기준으로 완성한다. 완료 후 인증된 사용자는 본인의 QT 묵상 노트와 자유 노트를 카테고리별로 조회, 임시 조회, 생성, 상세 조회, 수정, 삭제할 수 있어야 하며, 서버는 자동 저장 없이 명시적 `DRAFT`/`SAVED` 상태 전이만 처리한다.

`@`멘션 본문 자동 삽입은 클라이언트가 성경 절 조회 API로 본문을 삽입하는 흐름이므로 서버가 본문을 파싱하거나 성경 텍스트를 생성하지 않는다. 서버는 요청에 포함된 `verseIds`를 `note_verses` 메타데이터로 중복 없이 저장하고, 상세 조회에서 인덱스 정보를 반환하는 데 집중한다.

## 범위

- `GET /api/v1/notes` 목록 조회의 임시 고정값 필드(`visibility`, `qtDate`, `rangeLabel`, `shared`)를 실제 모델/조회 기준으로 보강한다.
- `GET /api/v1/notes/draft?category=MEDITATION&qtPassageId=` 임시 노트 조회를 구현한다.
- `GET /api/v1/note-categories` 카테고리 조회를 구현한다.
- `POST /api/v1/notes` 노트 생성 API를 구현한다.
- `GET /api/v1/notes/{noteId}` 노트 상세 조회 API를 구현한다.
- `PATCH /api/v1/notes/{noteId}` 노트 수정 API를 구현한다.
- `DELETE /api/v1/notes/{noteId}` 노트 소프트 삭제 API를 구현한다.
- `MEDITATION` 노트는 `qtPassageId` 필수, `activeUniqueKey='ACTIVE'`, 같은 사용자+QT 활성 노트 1건 제약을 적용한다.
- `SERMON` 노트는 성경 구절 선택을 필수로 하며, `PRAYER`, `REPENTANCE`, `GRATITUDE`는 구절 없이 저장할 수 있지만 `@`멘션으로 인용한 `verseIds`가 있으면 함께 저장한다.
- 요청에 `verseIds`가 있으면 `note_verses`를 요청 배열 기준으로 교체하고, 같은 절이 중복 전달되면 1건만 저장한다.
- HTTP 요청/응답 DTO와 UseCase Command/Result DTO를 API 명세 기준으로 구체화한다.
- `Note`, `NoteVerse`, migration을 ERD/API와 정합화한다. 현재 `BaseEntity.deletedAt`이 존재하므로 `notes.deleted_at` 컬럼도 migration에 반영한다.
- `NoteService` write path에는 `@Transactional`, read path에는 `@Transactional(readOnly = true)`를 적용한다.
- Controller는 UseCase만 호출하고 Repository를 직접 호출하지 않는다.
- 다른 도메인 연동은 상대 도메인의 `api/UseCase` 또는 임시 `client/{domain}/...UseCaseMock`만 사용한다.
- `qtai-server/apis/api-v1/openapi.yaml`에 노트 API 계약, 공통 envelope, 실패 코드를 반영한다.

## 제외 범위

- `/api/v1/notes/{noteId}/share`의 공유 스냅샷 생성 본문 구현은 제외한다. 공유글 스냅샷은 `domain.sharing` 소유이므로 별도 sharing workflow에서 처리한다.
- 묵상 달력 API `GET /api/v1/me/meditation-calendar` 구현은 제외한다. 단, Note 삭제/저장 시 달력 집계가 가능하도록 `savedAt`, `deletedAt`, `status` 정합성은 맞춘다.
- 노트 로컬 DB 우선 저장, 오프라인 큐잉, 동기화 충돌 응답은 v1.1 이후 정책이므로 제외한다.
- 서버가 `@`멘션 문자열을 직접 파싱하거나 성경 본문을 자동 삽입하는 기능은 제외한다.
- AI가 노트 본문을 작성, 수정, 평가, 추천하는 기능은 금지한다.
- 실제 성경 본문, 개역개정/ESV/NIV seed, fixture, OpenAPI 예시는 추가하지 않는다.
- 관리자 노트 열람/검열 API는 제외한다.

## 모델/API 계약 명세

### 상태와 카테고리

| 구분 | 기준 |
| --- | --- |
| 카테고리 | `MEDITATION`, `SERMON`, `PRAYER`, `REPENTANCE`, `GRATITUDE` |
| 상태 | `DRAFT`, `SAVED`, `DELETED`를 `NoteStatus`에 둔다. 삭제 시 `status=DELETED`, `deleted_at=now()`, `active_unique_key=NULL`을 함께 기록한다. |
| 공개 범위 | `NoteVisibility` enum을 두고 기본값은 `PRIVATE`로 저장한다. 공유 완료 후 `SHARED` 전환은 sharing workflow에서 처리한다. |
| 저장 시각 | `SAVED`가 되는 시점에 `savedAt=now()`를 기록한다. `SAVED -> DRAFT` 전환은 허용하되 `savedAt=NULL`로 되돌려 달력 집계에서 제외한다. |

### 구절 연결 정책

| 카테고리 | `qtPassageId` | `verseIds` 기준 |
| --- | --- | --- |
| `MEDITATION` | 필수 | 선택. 오늘 QT 본문 밖의 다른 성경 구절도 `@`멘션으로 인용할 수 있으므로 QT 본문 범위로 제한하지 않는다. |
| `SERMON` | 금지 | 필수. 한 절 이상 선택되어야 하며, 존재하지 않는 절이면 저장을 막는다. |
| `PRAYER` | 금지 | 선택. 본문 좌표 없이 저장 가능하지만 `@`멘션으로 인용한 절은 `note_verses`에 저장한다. |
| `REPENTANCE` | 금지 | 선택. 본문 좌표 없이 저장 가능하지만 `@`멘션으로 인용한 절은 `note_verses`에 저장한다. |
| `GRATITUDE` | 금지 | 선택. 본문 좌표 없이 저장 가능하지만 `@`멘션으로 인용한 절은 `note_verses`에 저장한다. |

### HTTP API

| Method | Path | 인증 | 책임 |
| --- | --- | --- | --- |
| GET | `/api/v1/notes` | USER | 본인 노트 목록, 필터, 검색, 페이지네이션 |
| GET | `/api/v1/notes/draft` | USER | 같은 사용자+QT+MEDITATION 활성 임시 노트 조회 |
| GET | `/api/v1/note-categories` | USER | 노트 카테고리 메타데이터 조회 |
| POST | `/api/v1/notes` | USER | 노트 생성 |
| GET | `/api/v1/notes/{noteId}` | USER, 작성자 | 노트 상세 조회 |
| PATCH | `/api/v1/notes/{noteId}` | USER, 작성자 | 노트 수정과 상태 전이 |
| DELETE | `/api/v1/notes/{noteId}` | USER, 작성자 | 소프트 삭제 |

### 요청 검증

| 조건 | 처리 |
| --- | --- |
| 인증 주체 없음 | `401 UNAUTHORIZED` |
| 다른 사용자의 노트 접근 | `403 FORBIDDEN` |
| 존재하지 않는 노트 | `404 NOTE_NOT_FOUND` |
| `MEDITATION`인데 `qtPassageId` 없음 | `400 INVALID_INPUT` |
| 자유 노트인데 `qtPassageId` 전달 | `400 INVALID_INPUT` |
| `SERMON`인데 `verseIds`가 비어 있음 | `400 INVALID_INPUT` |
| 같은 사용자+QT 활성 묵상 노트 중복 | `409 DUPLICATE_NOTE` |
| 삭제된 노트 수정 | `409 INVALID_STATUS_TRANSITION` |
| 모든 카테고리의 잘못된 `verseIds` 전달 | 존재하지 않는 절은 `404 BIBLE_VERSE_NOT_FOUND`, 중복 값은 저장 전 제거 |

현재 `ErrorCode`에는 `NOTE_NOT_FOUND`만 있으므로 `DUPLICATE_NOTE`를 추가한다. 입력 검증은 공통 `INVALID_INPUT`, 권한 위반은 `FORBIDDEN`, 상태 전이 위반은 `INVALID_STATUS_TRANSITION`으로 통일한다. OpenAPI와 테스트는 이 매핑을 따른다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/resources/db/migration/V4__create_notes.sql` 또는 신규 보정 migration | `notes.deleted_at`, `saved_at`, `visibility`, `remember_section`, `interpret_section`, `apply_section`, `pray_section` 컬럼과 인덱스/제약 정합화 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/Note.java` | 상태 전이, 저장/수정/삭제 메서드, visibility/savedAt/section 필드, `activeUniqueKey` 규칙 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteVerse.java` | `note_id`, `bible_verse_id`, `display_order`, 중복 방지 기준과 생성 팩토리 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteRepository.java` | 목록, 상세, 임시 노트, 활성 중복 검사, 작성자 검증 조회 |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteVerseRepository.java` | `note_verses` 교체 저장과 상세 조회용 조회 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteService.java` | `ListNotesUseCase`, `GetNoteUseCase`, `CreateNoteUseCase`, `UpdateNoteUseCase`, `DeleteNoteUseCase` 구현 |
| Create/Modify | `qtai-server/src/main/java/com/qtai/domain/note/api/**` | UseCase 인터페이스, enum, Command/Result/Response DTO 확정 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/web/NoteController.java` | 노트 목록/임시/생성/상세/수정/삭제 엔드포인트 |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/web/NoteCategoryController.java` | `/api/v1/note-categories` 엔드포인트 |
| Create/Modify | `qtai-server/src/main/java/com/qtai/domain/note/web/**Request.java`, `qtai-server/src/main/java/com/qtai/domain/note/web/**Response.java` | HTTP 요청/응답 DTO. Controller 외부 계약을 담당하고 Entity를 직접 노출하지 않는다. |
| Modify/Create | `qtai-server/src/main/java/com/qtai/domain/note/client/qt/**` | `MEDITATION` 노트의 QT 존재/권한 검증 어댑터. `verseIds`를 QT 본문 범위로 제한하지 않는다. |
| Modify/Create | `qtai-server/src/main/java/com/qtai/domain/note/client/bible/**` | `verseIds` 존재 검증 어댑터. `domain.bible.internal` 직접 import 금지 |
| Modify | `qtai-server/src/main/java/com/qtai/common/exception/ErrorCode.java` | 필요한 노트 오류 코드 추가 |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | §4.3 노트 API 계약 반영 |
| Create/Modify | `qtai-server/src/test/java/com/qtai/domain/note/**` | Controller, Service, Repository, JPA 테스트 보강 |
| Modify | `qtai-server/src/test/java/com/qtai/common/ArchitectureBoundaryTest.java` | `domain.note`의 타 도메인 internal/web import 금지 검증 |

## 구현 순서

1. `04_API_명세서.md` §4.3.1~§4.3.7, `07_요구사항_정의서.md` §6.4/§6.16, `25_기능_명세서.md` F-03/F-16을 다시 확인한다.
2. 현재 `Note`, `NoteVerse`, `V4__create_notes.sql`, 기존 note DTO의 누락 필드를 목록화한다.
3. `DELETED`, `visibility`, `savedAt`, QT 4개 섹션 컬럼을 스키마와 Entity에 반영하고, API 명세와 맞는 모델을 먼저 고정한다.
4. DB가 초기 개발 DB이면 V4를 정합화하고, 이미 적용된 환경을 고려해야 하면 신규 보정 migration을 추가한다.
5. `NoteStatus`에 `DELETED`를 추가하고 삭제 시 `status=DELETED`, `deletedAt=now()`, `activeUniqueKey=null`이 함께 바뀌도록 한다.
6. `NoteVisibility` enum을 도입해 문자열 임시 고정값을 제거한다.
7. HTTP용 `CreateNoteRequest`와 `UpdateNoteRequest`를 `web` 패키지에 분리한다. 생성 요청은 `category`, `qtPassageId`, `title`, `body`, QT 4개 섹션, `verseIds`, `status`, `visibility`를 담는다. `SERMON`은 제목 또는 본문 중 하나와 `verseIds` 1개 이상을 요구하고, `PRAYER`/`REPENTANCE`/`GRATITUDE`는 본문을 요구한다.
8. 응답 DTO는 목록용 `NoteListResponse`/`NoteListItem`, 임시 조회용 `NoteDraftResponse`, 상세용 `NoteDetailResponse`, 생성/수정 결과용 `NoteSaveResponse`, 카테고리용 `NoteCategoryResponse`로 분리한다.
9. `GetNoteUseCase`, `CreateNoteUseCase`, `UpdateNoteUseCase`, `DeleteNoteUseCase`의 미구현 주석을 실제 메서드 시그니처로 교체한다.
10. `Note` 엔티티에 생성 팩토리와 `update`, `markSaved`, `markDraft`, `delete` 같은 도메인 메서드를 추가한다.
11. `MEDITATION` 생성 시 `qtPassageId` 필수, `activeUniqueKey='ACTIVE'`, 중복 검사와 DB unique 제약을 함께 적용한다.
12. 자유 노트는 `qtPassageId=null`, `activeUniqueKey=null`을 유지한다.
13. `NoteVerseRepository`를 추가하고 `replaceNoteVerses(noteId, verseIds)` 흐름을 Service 트랜잭션 안에서 처리한다.
14. `verseIds` 검증은 모든 카테고리에서 `domain.bible.api.GetBibleVerseUseCase` 또는 note 도메인의 `client/bible` mock 어댑터를 통해 수행한다. 같은 절이 여러 번 전달되면 첫 등장 순서를 기준으로 1건만 저장한다.
15. `MEDITATION`의 `qtPassageId` 존재/권한 검증은 note 도메인의 `client/qt/GetQtUseCaseMock` 또는 실제 `domain.qt.api` 계약을 사용한다. `@`멘션으로 인용한 `verseIds`는 QT 본문 범위로 제한하지 않는다.
16. `GET /api/v1/notes/draft`는 `category=MEDITATION`과 `qtPassageId` 조합만 허용하고, 없으면 `exists=false`, 있으면 `exists=true`와 note를 반환한다.
17. `GET /api/v1/note-categories`는 enum 기반 정적 응답으로 구현하고, `requiresQtPassage`, `supportsVerseSelection`, `writableFromList`를 명세와 일치시킨다.
18. `GET /api/v1/notes/{noteId}`는 작성자 본인만 접근하게 하고, 삭제된 노트는 조회하지 않는다.
19. `PATCH /api/v1/notes/{noteId}`는 `DRAFT <-> SAVED` 전이와 본문/구절 교체를 한 트랜잭션으로 처리한다.
20. `DELETE /api/v1/notes/{noteId}`는 `deletedAt`, `status=DELETED`, `activeUniqueKey=null`을 함께 처리해 같은 QT 묵상 노트 재작성을 허용한다.
21. 공유된 노트 수정/삭제 시 `sharing_posts` 스냅샷을 자동 변경하지 않는다. 삭제 알림용 `source_note_deleted_at` 반영은 sharing workflow 범위로 남긴다.
22. `NoteController`는 인증 주체 null 가드를 모든 엔드포인트에 동일하게 적용한다.
23. OpenAPI에 `/api/v1/notes`, `/api/v1/notes/draft`, `/api/v1/note-categories`, `/api/v1/notes/{noteId}`의 성공/실패 응답을 추가한다.
24. 테스트와 OpenAPI 예시에 실제 성경 본문 문장을 넣지 않고 `더미 구절 본문입니다.` 같은 중립 문구만 사용한다.
25. `ArchitectureBoundaryTest` 또는 `rg`로 `domain.note`가 다른 도메인의 `internal`/`web`을 import하지 않는지 확인한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/note/web/NoteControllerTest.java` | 목록, 임시 조회, 카테고리, 생성, 상세, 수정, 삭제의 UseCase 위임과 인증 주체 null 가드 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | 생성/수정/삭제 상태 전이, `MEDITATION` 필수값, `SERMON` 구절 필수, 자유 노트 `@`멘션 `verseIds` 허용, 중복 묵상 노트, `note_verses` 교체 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteRepositoryIntegrationTest.java` | `deleted_at` 필터, `saved_at` 정렬/필터, active unique 제약, draft 조회, 작성자 격리 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteVerseRepositoryTest.java` | 중복 verseId 제거, displayOrder 보존, note별 삭제 후 재삽입 |
| `qtai-server/src/test/java/com/qtai/common/JpaEntityDdlTest.java` | `Note`, `NoteVerse`, migration 컬럼 정합성 |
| `qtai-server/src/test/java/com/qtai/common/ArchitectureBoundaryTest.java` | `domain.note`가 `domain.bible.internal`, `domain.qt.internal`, `domain.sharing.internal` 직접 import하지 않음 |

## 수용 기준

- [ ] `GET /api/v1/notes`가 본인 노트만 반환하고 삭제된 노트를 노출하지 않는다.
- [ ] 목록 응답의 `visibility`, `qtDate`, `rangeLabel`, `shared`가 실제 정책 또는 명시된 null 기준을 따른다.
- [ ] `GET /api/v1/note-categories`가 5개 카테고리 메타데이터를 반환한다.
- [ ] `GET /api/v1/notes/draft`가 같은 사용자+QT+MEDITATION 임시 노트를 정확히 찾고, 없으면 `exists=false`를 반환한다.
- [ ] `POST /api/v1/notes`가 QT 묵상 노트와 자유 노트를 생성한다.
- [ ] 모든 자유 노트 카테고리에서 `@`멘션으로 전달된 `verseIds`를 저장할 수 있다.
- [ ] `SERMON` 노트는 `verseIds` 1개 이상 없이는 저장되지 않는다.
- [ ] `PATCH /api/v1/notes/{noteId}`가 본인 노트만 수정하고 `DRAFT`/`SAVED` 전이를 검증한다.
- [ ] `DELETE /api/v1/notes/{noteId}`가 `deletedAt`, `DELETED`, `activeUniqueKey=null`을 반영해 소프트 삭제한다.
- [ ] 같은 사용자+QT+MEDITATION 활성 노트 중복 생성이 `409 DUPLICATE_NOTE` 또는 확정된 동등 오류로 차단된다.
- [ ] `verseIds`는 `note_verses`에 중복 없이 저장되고 수정 시 요청 배열 기준으로 교체된다.
- [ ] 서버는 `@`멘션 본문 파싱이나 성경 본문 자동 삽입을 하지 않는다.
- [ ] 노트 본문 생성/수정에 AI를 사용하지 않는다.
- [ ] Controller는 Repository를 직접 호출하지 않는다.
- [ ] `domain.note`는 다른 도메인의 `api` 또는 `client` 패키지만 사용한다.
- [ ] OpenAPI와 실제 Controller 경로, 요청/응답 DTO가 일치한다.
- [ ] 테스트/fixture/OpenAPI 예시에 실제 성경 본문과 금지 번역본 데이터가 들어가지 않는다.

## Subagent Decision

### 권장 여부

Subagent use is authorized for this workflow when the agent determines that parallel work is beneficial.

### 판단 근거

- Note CRUD 구현, JPA/migration 정합화, OpenAPI 계약, 테스트 보강은 경로를 분리하면 병렬 진행이 가능하다.
- 단, DTO 필드명, 상태 전이, `activeUniqueKey`, 삭제 정책은 한 번 정하면 여러 파일에 파급되므로 메인 에이전트가 먼저 계약을 고정해야 한다.
- `sharing`, `qt`, `bible`과 맞닿는 지점은 도메인 경계 위반 위험이 있어 최종 통합 검증을 메인 에이전트가 직접 수행해야 한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| Worker 1 | Note 엔티티/서비스/리포지토리 구현 | `qtai-server/src/main/java/com/qtai/domain/note/**` |
| Worker 2 | Note 테스트 보강 | `qtai-server/src/test/java/com/qtai/domain/note/**`, `qtai-server/src/test/java/com/qtai/common/**` |
| Worker 3 | OpenAPI 계약 반영 | `qtai-server/apis/api-v1/openapi.yaml` |
| Worker 4 | migration/JPA 정합성 확인 | `qtai-server/src/main/resources/db/migration/**`, `qtai-server/src/test/java/com/qtai/common/JpaEntityDdlTest.java` |

### 직접 실행 판단

메인 에이전트는 상태 전이, 오류 코드, 도메인 경계, OpenAPI-Controller 정합성, 금지 데이터 검사를 직접 통합 확인한다.

## 검증 계획

- `git diff --check`
- `./gradlew -p qtai-server test --tests "*Note*"`
- `./gradlew -p qtai-server test --tests "*JpaEntityDdlTest"`
- `./gradlew -p qtai-server test --tests "*ArchitectureBoundaryTest"`
- `./gradlew -p qtai-server build`
- `./gradlew -p qtai-server test jacocoTestReport`
- `./gradlew -p qtai-server jacocoTestCoverageVerification`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`
- `rg -n "com\\.qtai\\.domain\\.(bible|qt|sharing)\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/note --glob "*.java"`
- `rg -n "개역개정|ESV|NIV|성서유니온|두란노" qtai-server/src/main qtai-server/src/test qtai-server/apis/api-v1/openapi.yaml`

`spectral` 또는 `gitleaks` 실행 파일이 없는 환경에서는 설치 여부와 실패 사유를 PR 본문에 남기고, CI에서 같은 명령을 다시 수행한다.

## 후속 작업으로 넘길 항목

- `domain.sharing` 소유의 `/api/v1/notes/{noteId}/share` 공유 스냅샷 생성 구현
- `GET /api/v1/me/meditation-calendar` 묵상 달력 API 구현
- v1.1 이후 노트 로컬 캐시, 오프라인 큐잉, 충돌 감지 정책
- 공유된 원본 노트 삭제 시 `sharing_posts.source_note_deleted_at` 반영 흐름
- 노트 검색 고도화 또는 구절 기반 내 노트 보기
