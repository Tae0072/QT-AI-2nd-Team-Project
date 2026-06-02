# note·sharing·report 파일 레퍼런스 (담당 도메인 한눈에 보기)

> **담당**: 김지민
> **목적**: 내 담당 도메인(note·sharing·report) 파일이 많아서, 각 파일이 *무슨 일을 하는지* 한국어로 빠르게 찾아보는 지도.
> **표기**: ✅ 구현완료 / ⚠️ 스켈레톤(`// TODO`, 아직 빈 껍데기) / 🗑️ 죽은코드(명세에 없음·삭제검토) / 🟦 팀 완료(다른 팀원이 dev에 머지)
> **기준일**: 2026-05-28 최초 작성 → **2026-05-29 최신 dev 기준 전면 갱신** (이전 버전은 stale 브랜치를 봐서 sharing 분석이 부정확했음)

> 📖 **계층 읽는 법** (모든 도메인 공통)
> - `api` : 다른 도메인·컨트롤러가 호출하는 **공개 창구(UseCase 인터페이스)** + enum. "이 도메인이 뭘 해주는지"의 계약서.
> - `api/dto` : 요청/응답으로 주고받는 **데이터 상자**(record). Entity를 밖에 노출 안 하려고 둠.
> - `internal` : **실제 알맹이** — Entity(테이블), Service(로직), Repository(DB 접근). 다른 도메인이 직접 못 봄.
> - `web` : `/api/v1/**` **HTTP 입구**(Controller).
> - `client` : **다른 도메인을 부를 때** 쓰는 어댑터(상대 도메인의 api 포트를 호출).

---

# 📁 note 도메인 — 묵상·자유 노트

> 내 W2 핵심 작업. 노트 CRUD + 목록/임시본 조회. (JournalEvent·묵상달력은 이지윤 영역인데 dev에 머지됨 — 아래 🟦 참고)

## api — UseCase 포트 (공개 창구)

| 파일 | 무슨 일을 하나 | 입력 → 반환 |
|---|---|---|
| **CreateNoteUseCase** | 노트 한 건 새로 만든다 | `(memberId, CreateNoteCommand)` → `NoteCreateResponse` |
| **UpdateNoteUseCase** | 노트를 고친다(임시저장↔저장 전이 포함) | `(memberId, noteId, UpdateNoteCommand)` → `NoteUpdateResponse` |
| **GetNoteUseCase** | 노트 상세 보기 + 임시본 불러오기 | `get(memberId, noteId)`→`NoteDetailResponse` / `getDraft(memberId, category, qtPassageId)`→`NoteDraftResponse` |
| **ListNotesUseCase** | 내 노트 목록(카테고리 필터·검색·페이징) | `(memberId, category, status, q, pageable)` → `NoteListResponse` |
| **DeleteNoteUseCase** | 노트 삭제(소프트 삭제) | `(memberId, noteId)` → `void` |
| **ListNoteCategoriesUseCase** | 5개 카테고리 메타 목록 | `()` → `NoteCategoryResponse` |

## api — enum (정해진 값 묶음)

| 파일 | 값(뜻) |
|---|---|
| **NoteCategory** | `MEDITATION`(묵상-QT필수·하루1건) / `SERMON`(설교-절필수) / `PRAYER`(기도) / `REPENTANCE`(회개) / `GRATITUDE`(감사) |
| **NoteStatus** | `DRAFT`(임시저장) / `SAVED`(저장) / `DELETED`(삭제) |
| **NoteVisibility** | `PRIVATE`(비공개·기본) / `SHARED`(공개) |

## api/dto — 데이터 상자

| 파일 | 무슨 일을 하나 | 주요 필드 |
|---|---|---|
| **CreateNoteCommand** | 노트 생성에 필요한 값 묶음 | category, qtPassageId, title, body, rememberSection(기억), interpretSection(느낀점), applySection(적용), praySection(기도), verseIds(연결 절), status, visibility |
| **UpdateNoteCommand** | 노트 수정 값 묶음 | CreateNoteCommand와 같은 구조 |
| **NoteCreateResponse** | 생성 결과(§4.3.4) | id, category, status, visibility, sharedPostId(공유글ID·현재 null), createdAt |
| **NoteUpdateResponse** | 수정 결과(§4.3.6) | id, category, status, visibility, activeUniqueKey, savedAt, updatedAt, sharingSnapshotUpdated |
| **NoteDetailResponse** | 노트 상세 한 건 | id, memberId, category, qtPassageId, title, body, 4개섹션, status, visibility, qtDate⚠️, rangeLabel⚠️, shared, savedAt, createdAt, updatedAt, verses |
| **NoteDraftResponse** | 임시본 존재 여부 + 내용 | exists, note(없으면 null) |
| **NoteListResponse** | 목록 응답(페이징 봉투) | content, page, size, totalElements, totalPages, first, last, sort |
| **NoteListItem** | 목록 한 줄 | id, category, title, status, visibility, qtDate⚠️, rangeLabel⚠️, shared, savedAt, createdAt, updatedAt |
| **NoteCategoryResponse / NoteCategoryItem** | 카테고리 메타 | code, label, requiresQtPassage(QT필수?), supportsVerseSelection(절선택?), writableFromList(목록서 작성?) |
| **NoteVerseItem** | 연결된 성경 절 정보 | bibleVerseId, bookCode, chapterNo, verseNo, displayOrder |

> ⚠️ `qtDate`·`rangeLabel`은 아직 안 채워지는 필드(QT 연동 후속).

## internal — 알맹이

| 파일 | 무슨 일을 하나 | 핵심 |
|---|---|---|
| **Note** (Entity) | `notes` 테이블 한 행 | memberId·qtPassageId·category·status·visibility·title·body·4개섹션·savedAt·activeUniqueKey + (상속)createdAt/updatedAt/deletedAt. **유니크 (memberId,qtPassageId,activeUniqueKey) = 묵상 하루 1건.** 메서드: create/update/delete/transitionTo/refreshActiveUniqueKey |
| **NoteVerse** (Entity) | 노트↔성경절 연결(`note_verses`) | noteId, bibleVerseId, displayOrder, highlight. 유니크 (noteId,bibleVerseId) |
| **NoteRepository** | 노트 DB 접근 | search(필터·페이징), findActiveByIdAndMemberId, findDraft, 중복체크 2종 |
| **NoteVerseRepository** | 절 연결 DB | 노트별 순서 조회, 일괄 조회, deleteByNoteId |
| **NoteService** | 노트 로직(6개 UseCase 구현) | list/get/getDraft/create/update/delete/listCategories + 내부 validateForSave(카테고리 검증)·replaceNoteVerses·normalize·escapeLikeWildcards |

## web / client

| 파일 | 무슨 일을 하나 |
|---|---|
| **NoteController** (`/api/v1/notes`) | GET 목록 / GET draft / GET {id} / POST(201) / PATCH / DELETE(204). memberId 없으면 401 |
| **NoteCategoryController** (`/api/v1/note-categories`) | GET → 5개 카테고리 메타 |
| **CreateNoteRequest / UpdateNoteRequest** | HTTP 요청 바디(@Valid 형식 검증) + `toCommand()` |
| **client/qt/NoteQtClient** + **GetQtUseCaseMock** | QT 읽기권한 검증 어댑터(`validateReadable`). 실패 시 QT_PASSAGE_NOT_FOUND |
| **client/member/GetMemberUseCaseMock** | 회원 조회 **미충전 스캐폴드**(아래 ※ 참고) |

> 🟦 **dev에 머지된 이지윤 영역** (note 패키지에 함께 있지만 내 작업 아님): `JournalEvent`/`JournalEventHandler`(묵상 SAVED 이벤트 이력), `MeditationCalendarService`/`MeditationCalendarController`(묵상 달력 `GET /me/meditation-calendar`). → 노트 저장 시 이벤트가 발행돼 달력에 반영되는 흐름이 dev에 들어와 있음.
>
> ※ **`client/member/GetMemberUseCaseMock`은 "잔재"가 아니다.** 8개 도메인에 똑같이 있는 **전역 스캐폴드**다. `member.api.GetMemberUseCase`(실재 포트, nickname 반환)를 회원 조회가 필요해질 때 채우는 자리. W3 나눔 공개(publish)가 닉네임 박제할 때 바로 이 자리를 쓴다 → **삭제 금지**.

---

# 📁 sharing 도메인 — 나눔(커뮤니티)

> 파일이 세 갈래로 섞여 있다: **① 조회(이미 완료)**, **② 쓰기(W3 내가 채울 것)**, **③ 토큰공유(죽은 코드)**. 이 구분이 핵심.

## ① 🟢 나눔 조회 — 이미 완료 (B3 목록 + B4 상세, #135 머지)

| 파일 | 무슨 일을 하나 | 핵심 |
|---|---|---|
| **SharingPost** (Entity) | 공개된 노트의 **스냅샷**(박제본). 원본 노트가 바뀌어도 여기는 안 바뀜 | memberId, **noteId(유니크 — 노트당 1번만 공개)**, status / **스냅샷 7필드**: snapshotTitle·snapshotBody·snapshotCategory·snapshotQtDate·nicknameSnapshot(공개 시점 닉네임)·snapshotVerseLabel(절 범위 라벨) / commentsEnabled(댓글 ON/OFF) / likeCount·commentCount / hiddenAt·sourceNoteUnsharedAt |
| **SharingPostStatus** (enum) | 글 상태 | `PUBLISHED`(공개) / `HIDDEN`(공개중단) / `DELETED`(삭제) |
| **Comment** (Entity) | 나눔 글의 댓글 | sharingPostId, memberId, parentId(대댓글용), body, **isDeleted(소프트삭제)** |
| **PostLike** (Entity) | 좋아요 1건 | id, sharingPostId, memberId, createdAt. **유니크 (post,member) — 중복 좋아요 금지.** BaseEntity 미상속(updatedAt 불필요) |
| **SharingPostRepository** | 나눔 글 DB | `findByIdAndStatus`(PUBLISHED만 → 없으면 404로 존재 숨김), `search`(피드: category+q+페이징) |
| **PostLikeRepository** | 좋아요 DB | `findLikedPostIds(memberId, postIds)` — **N+1 방지** IN 배치 |
| **SharingPostService** | 나눔 조회 로직 | `list`(목록)·`getDetail`(상세, 404+likedByMe+ownedByMe) + 내부: 미리보기 100자 자르기·LIKE 이스케이프·정렬 화이트리스트 |
| **SharingPostController** (`/api/v1/sharing-posts`) | HTTP 입구 | **GET**(목록) / **GET /{postId}**(상세). memberId 없으면 401 |
| **ListSharingPostsUseCase** | 목록 포트 | `(memberId, category, q, pageable)` → `SharingPostListResponse` |
| **GetSharingPostUseCase** | 상세 포트 | `(memberId, postId)` → `SharingPostResponse` |
| **SharingPostListItem** | 피드 한 줄 | id, nicknameSnapshot, titleSnapshot, category, status, verseSnapshot, bodyPreview, commentsEnabled, sourceNoteDeletedAt, likeCount, commentCount, likedByMe, publishedAt |
| **SharingPostListResponse** | 피드 페이징 봉투 | content + page/size/totalElements/totalPages/first/last/sort |
| **SharingPostResponse** | 상세 응답 | 위 + noteId, memberId, bodySnapshot(전체), ownedByMe, hiddenAt, deletedAt, verseSnapshot(Detail) |
| **VerseSnapshot** | 목록용 절 라벨 | rangeLabel("창세기 1:1-5") |
| **VerseSnapshotDetail** | 상세용 절 | rangeLabel + verses[](현재 빈 배열 = v2) |
| **VerseLine** | 절 한 줄 | label, koreanText |

## ② 🟢 나눔 쓰기 — W3 구현 완료 (공개·좋아요·댓글, 2026-06-01, `feature/sharing-write`)

> ⚠️ **publish 중복 구현 / merge 정리 (2026-06-01)**: 나눔 공개(`POST /notes/{id}/share`)는 **이승욱이 PR #179로 dev에 먼저 머지**했다. dev merge 시 충돌 → **A안(dev #179 기반 + 내 버그수정 이식)**으로 해결:
> - 배선은 dev(SharingPostController)로, 내 `NoteController.share`는 제거(중복 매핑 방지).
> - 단, #179 publish는 **묵상노트(body=null)·제목없는노트에서 NOT NULL 위반 크래시** → 내 `composeBody`·null방어·SAVED 검증을 이식해 고쳤다.
> - **내 고유 기여 = 좋아요·댓글** (충돌 없음).

| 파일 | 무슨 일을 하나 | 상태 |
|---|---|---|
| **PublishNoteUseCase** + `SharingPostService.publish` | 노트→나눔 공개. #179 기반 + 내 버그수정 이식(composeBody·null방어·SAVED). 중복공유 `DUPLICATE_SHARING_POST` | ✅ #179+이식 |
| **PublishNoteRequest** | `@NotNull @AssertTrue confirmNicknamePublic` + `isCommentsEnabled()` (dev #179 채택) | ✅ |
| **ToggleLikeUseCase** + `SharingPostService.like/unlike` | 좋아요 토글. **COUNT 재계산**·중복 409·취소 멱등 204 | ✅ |
| **LikeResponse** | likeCount + likedByMe | ✅ |
| **CommentUseCase** + **CommentService** (신규) | 댓글 작성·목록·삭제. 댓글OFF 409·본인만 403·soft delete 멱등·commentCount 재계산 | ✅ |
| **CommentCreateRequest** | `body`(@NotBlank @Size max 1000) — `content` 아님(04 명세 일치) | ✅ |
| **CommentResponse / CommentListResponse** | id·sharingPostId·memberId·nickname·body·ownedByMe·createdAt / 페이징 봉투 | ✅ |
| **CommentController** (`/api/v1`) | POST·GET `/sharing-posts/{id}/comments` · DELETE `/comments/{id}` | ✅ |
| **CommentRepository** (신규) | `findBy…IsDeletedFalseOrderByCreatedAtAsc` · `countBy…IsDeletedFalse` | ✅ |
| **PostLikeRepository** (보강) | `existsBy…` · `countBy…` · `deleteBy…` 추가 | ✅ |
| **Comment / PostLike / SharingPost** (보강) | `Comment.of/markDeleted`, `PostLike.of`, `SharingPost.syncLikeCount/syncCommentCount` | ✅ |
| **ErrorCode** | `DUPLICATE_LIKE`(S0004, 409) 추가 | ✅ |
| **SharingService** | 토큰공유용 스켈레톤(③ 갈래) — 미사용 | ⚠️ |

> - 공유글 삭제(`DELETE /sharing-posts/{postId}`)는 별도(범위 밖). 신고는 dev 완료(#140).
> - 닉네임: 공유글은 **박제**(`nicknameSnapshot`), 댓글은 **실시간 조회**(`getMemberPublic`) — 댓글은 공유본 스냅샷 정책 대상이 아님.
> - 대댓글: `parent_id` 컬럼은 있으나 v1은 평면 댓글(parentId 항상 null), v2용.
> - 테스트: 좋아요 11개 + 댓글 16개 컴파일 통과. 로컬 `gradlew test`는 한글경로 인코딩 이슈로 CI 검증(메인 컴파일·bootJar 정상).

## ③ 🗑️ 토큰공유(Share) 갈래 — 죽은 코드 (04 명세에 없음, 강태오 결정 대기)

> URL 토큰으로 비로그인 공유하는 옛 설계. **07 비회원 접근 금지와 충돌**하고 04 명세(88개)에 없음. 삭제 후보지만 임의 삭제 금지 — 강태오 결정 필요.

| 파일 | 비고 |
|---|---|
| **Share / ShareSnapshot** (Entity) | shareToken(URL노출)·snapshotJson 등 — 전부 `// TODO` |
| **ShareRepository** | findByShareToken 등 — `// TODO` |
| **SharingService** | createShare/getSnapshot/revokeShare — `// TODO` |
| **SharingController** (`/api/v1/shares`) | POST / **GET /token/{token}(비로그인)** ←정책 위배 / DELETE — `// TODO` |
| **CreateShareUseCase / GetSharedSnapshotUseCase / RevokeShareUseCase** | `// TODO` |
| **ShareCreateRequest / SharedSnapshotResponse** (DTO) | `// TODO` |
| **client/{member,note,qt,study}/...UseCaseMock** 4종 | 토큰공유가 원본 조회하려던 어댑터 — `// TODO`. (단 member mock은 ② publish에서 재활용 가능) |

---

# 📁 report 도메인 — 신고 (🟦 팀 완료, #140)

> 신고는 원래 내 분담(`POST /api/v1/reports`)인데, **최신 dev에선 팀이 이미 구현 완료**(#140)했다. 그래서 W3에서 내가 다시 짤 필요 없음 — 참고만.

| 파일 | 무슨 일을 하나 | 상태 |
|---|---|---|
| **ReportController** (`/api/v1/reports`) | `@PostMapping` 신고 접수(201) | ✅ |
| **CreateReportUseCase / GetReportUseCase** | 신고 접수·조회 포트 | ✅ |
| **ReportCreateRequest / ReportResponse** (DTO) | targetType·targetId·reason·detail (04 §4.4.7 기준) | ✅ |
| **Report** (Entity) | `reports` 테이블(V17) | ✅ |
| **ReportStatus** (enum) | RECEIVED→REVIEWING→RESOLVED/REJECTED | ✅ |
| **ReportTargetType** (enum) | POST / COMMENT / AI_QA_REQUEST / AI_ASSET | ✅ |
| **ReportRepository / ReportService** | 중복신고 차단·대상 존재검증(POST는 sharing 포트로 검증) | ✅ |

> ※ 처음 작성한 레퍼런스에 report가 빠졌었고, 그땐 "스켈레톤"이었지만 **dev에서 완료됨**. (이전 가정 "reports 테이블 V14 신규 필요"는 폐기 — 실제 V17로 이미 존재.)

---

## 핵심 개념 6개

| 개념 | 쉽게 |
|---|---|
| **스냅샷(박제)** | 공개하는 순간의 제목·본문·닉네임을 복사해 둔다. 원본을 고쳐도 공개본은 그대로 (nicknameSnapshot·snapshotBody) |
| **noteId 유니크** | 한 노트는 나눔에 딱 한 번만 공개(중복 공개 막기) |
| **activeUniqueKey** | 묵상 노트 하루 1건 강제 키. 삭제하면 null로 풀려 재작성 허용 |
| **likedByMe / N+1 방지** | "내가 좋아요 눌렀나"를 글마다 조회하면 느림(N+1) → IN 절로 한 번에 |
| **ownedByMe** | "내가 쓴 글인가"(상세 전용) — 수정/삭제 버튼 노출 판단 |
| **정렬 화이트리스트** | sort는 URL 문자열이라 허용 필드만 통과(publishedAt→createdAt 변환) |

## 구현 상태 요약 (2026-05-29 최신 dev 기준)
```
note     : 내 노트 CRUD/목록 ✅  (+ 이지윤 JournalEvent·묵상달력 🟦 dev 머지)
sharing  : ① 조회(목록·상세) ✅ #135
           ② 쓰기(공개·좋아요·댓글) ✅ 2026-06-01 (feature/sharing-write, PR 예정) — 공유글삭제는 별도
           ③ 토큰공유(Share) 갈래 🗑️ 죽은코드 (강태오 결정 대기)
report   : 신고 ✅ 🟦 팀 완료(#140) — W3 대상 아님
의존성    : member.GetMemberUseCase.getMember().nickname() → publish 닉네임 박제에 사용
```
