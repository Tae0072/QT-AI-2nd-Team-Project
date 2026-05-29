# note·sharing 파일 레퍼런스 (69파일 역할·필드·기대값)

> **담당**: 김지민
> **목적**: 담당 도메인(note·sharing) 파일이 많아 각 파일의 역할을 한눈에 보기 위한 레퍼런스
> **표기**: ✅구현 / ⚠️미구현 스켈레톤 / 🗑️죽은코드(삭제검토)
> **기준일**: 2026-05-28 (실제 코드 읽고 정리)

---

# 📁 note 도메인 (33파일)

계층: `api`(UseCase 포트+enum) · `api/dto`(요청/응답) · `internal`(Entity/Service/Repository) · `web`(Controller) · `client`(타 도메인 호출)

## api — UseCase 포트

| 파일 | 역할 | 기대값(입력 → 반환) |
|---|---|---|
| **CreateNoteUseCase** | 노트 생성 포트 | `(memberId, CreateNoteCommand)` → `NoteCreateResponse` |
| **UpdateNoteUseCase** | 노트 수정 포트 | `(memberId, noteId, UpdateNoteCommand)` → `NoteUpdateResponse` |
| **GetNoteUseCase** | 상세+임시본 조회 포트 | `get(memberId, noteId)`→`NoteDetailResponse` / `getDraft(memberId, category, qtPassageId)`→`NoteDraftResponse` |
| **ListNotesUseCase** | 목록 조회 포트 | `(memberId, category, status, q, pageable)` → `NoteListResponse` |
| **DeleteNoteUseCase** | 삭제 포트 | `(memberId, noteId)` → `void` |
| **ListNoteCategoriesUseCase** | 카테고리 목록 포트 | `()` → `NoteCategoryResponse` |

## api — enum

| 파일 | 상수값(의미) |
|---|---|
| **NoteCategory** | `MEDITATION`(묵상, QT필수·하루1건) / `SERMON`(설교, 절필수) / `PRAYER`(기도) / `REPENTANCE`(회개) / `GRATITUDE`(감사) |
| **NoteStatus** | `DRAFT`(임시저장) / `SAVED`(저장) / `DELETED`(삭제) |
| **NoteVisibility** | `PRIVATE`(비공개) / `SHARED`(공개) |

## api/dto

| 파일 | 역할 | 주요 필드(한글) |
|---|---|---|
| **CreateNoteCommand** | 생성 명령 | category, qtPassageId, title, body, rememberSection(기억), interpretSection(느낀점/해석), applySection(적용), praySection(기도), verseIds(연결 절), status, visibility |
| **UpdateNoteCommand** | 수정 명령 | CreateNoteCommand와 동일 구조 |
| **NoteCreateResponse** | 생성 응답(§4.3.4) | id, category, status, visibility, sharedPostId(공유글ID·현재 null), createdAt |
| **NoteUpdateResponse** | 수정 응답(§4.3.6) | id, category, status, visibility, activeUniqueKey(묵상 활성키), savedAt, updatedAt, sharingSnapshotUpdated |
| **NoteDetailResponse** | 상세 응답 | id, memberId, category, qtPassageId, title, body, 4개섹션, status, visibility, qtDate⚠️, rangeLabel⚠️, shared, savedAt, createdAt, updatedAt, verses(연결 절) |
| **NoteDraftResponse** | 임시본 응답 | exists(존재여부), note(NoteDetailResponse, 없으면 null) |
| **NoteListResponse** | 목록 응답(페이징 봉투) | content, page, size, totalElements, totalPages, first, last, sort |
| **NoteListItem** | 목록 1건 | id, category, title, status, visibility, qtDate⚠️, rangeLabel⚠️, shared, savedAt, createdAt, updatedAt |
| **NoteCategoryResponse** | 카테고리 목록 응답 | categories(NoteCategoryItem 목록) |
| **NoteCategoryItem** | 카테고리 1건 | category, label(표시명), requiresQtPassage(QT필수?), supportsVerseSelection(절선택 지원?), writableFromList(목록서 작성?) |
| **NoteVerseItem** | 연결된 절 정보 | bibleVerseId, bookCode, chapterNo, verseNo, displayOrder |

> ⚠️ `qtDate`·`rangeLabel`은 현재 미구현(채워지지 않는 필드)

## internal

| 파일 | 역할 | 주요 필드/메서드 |
|---|---|---|
| **Note** (Entity) | 노트 테이블(notes) | 필드: memberId, qtPassageId, category, status, visibility, title, body, 4개섹션, savedAt, activeUniqueKey + (상속)createdAt/updatedAt/deletedAt. **유니크:(memberId,qtPassageId,activeUniqueKey)=묵상 하루1건.** 메서드: create/update/delete/transitionTo/refreshActiveUniqueKey |
| **NoteVerse** (Entity) | 노트-절 연결(note_verses) | noteId, bibleVerseId, displayOrder, highlight(강조텍스트). 유니크:(noteId,bibleVerseId) 중복금지 |
| **NoteRepository** | 노트 DB | search(필터·페이징), findActiveByIdAndMemberId, findDraft, existsBy...ActiveUniqueKey(중복체크 2종) |
| **NoteVerseRepository** | 절 연결 DB | findAllByNoteId...(순서대로), 여러 노트 일괄조회, deleteByNoteId |
| **NoteService** | 노트 로직(6 UseCase 구현) | list/get/getDraft/create/update/delete/listCategories + 내부: validateForSave(카테고리별 검증), replaceNoteVerses, normalize, escapeLikeWildcards |
| **JournalEvent** | ⚠️스켈레톤 | 묵상 SAVED 시 이력 기록 예정 (note, eventType, occurredAt) |

## web

| 파일 | 역할 | 기대값 |
|---|---|---|
| **NoteController** | `/api/v1/notes` | GET(목록)/GET draft/GET {id}/POST(201)/PATCH/DELETE(204). memberId null→401 |
| **NoteCategoryController** | `/api/v1/note-categories` | GET → 5개 카테고리 메타 |
| **CreateNoteRequest** | 생성 요청바디 | @NotNull category, @Size(200) title 등 + `toCommand()` |
| **UpdateNoteRequest** | 수정 요청바디 | (동일) + `toCommand()` |

## client

| 파일 | 역할 | 기대값 |
|---|---|---|
| **NoteQtClient** | QT 호출 포트 | `validateReadable(memberId, qtPassageId)` — QT 읽기권한 검증 |
| **GetQtUseCaseMock** | NoteQtClient 임시구현 | qtPassageId 유효성 검사, 실패시 QT_PASSAGE_NOT_FOUND |
| **GetMemberUseCaseMock** | ⚠️스켈레톤 | 회원 조회 Mock(미사용 잔재) |

---

# 📁 sharing 도메인 (36파일)

## 🟢 나눔(SharingPost) 갈래 — 살아있음

| 파일 | 역할 | 주요 필드/기대값 |
|---|---|---|
| **SharingPost** (Entity) | 나눔 게시글 | memberId, noteId(유니크·노트당1번), status, **스냅샷**: snapshotTitle/Body/Category/QtDate, nicknameSnapshot(발행시점 닉네임), snapshotVerseLabel / commentsEnabled / likeCount, commentCount / hiddenAt, sourceNoteUnsharedAt |
| **SharingPostStatus** (enum) | 상태 | `PUBLISHED`(공개) / `HIDDEN`(비공개전환·공유취소) / `DELETED`(삭제) |
| **Comment** (Entity) | 댓글 | sharingPostId, memberId, parentId(대댓글), body(1000자), isDeleted |
| **PostLike** (Entity) | 좋아요 | id, sharingPostId, memberId, createdAt. 유니크:(post,member) 중복금지. BaseEntity 미상속(updatedAt 불필요) |
| **SharingPostRepository** | 게시글 DB | `findByIdAndStatus`(상세, PUBLISHED만→없으면 404), `search`(피드, category+q+페이징) |
| **PostLikeRepository** | 좋아요 DB | `findLikedPostIds(memberId, postIds)` — N+1 방지 배치(IN 절) |
| **SharingPostService** | 나눔 조회 로직 | `list`(목록 B3) / `getDetail`(상세 B4, 404+likedByMe+ownedByMe) + 내부: findLikedPostIds(배치), toItem, toPreview(100자), toEscapedQuery, translateSort(정렬 화이트리스트) |
| **SharingPostController** | `/api/v1/sharing-posts` | GET(목록), GET /{postId}(상세). memberId null→401 |
| **ListSharingPostsUseCase** | 목록 포트 | `(memberId, category, q, pageable)` → `SharingPostListResponse` |
| **GetSharingPostUseCase** | 상세 포트 | `(memberId, postId)` → `SharingPostResponse` |
| **SharingPostListItem** | 피드 1건 | id, nicknameSnapshot, titleSnapshot, category, status, verseSnapshot, bodyPreview, commentsEnabled, sourceNoteDeletedAt, likeCount, commentCount, likedByMe, publishedAt |
| **SharingPostListResponse** | 피드 응답(페이징) | content + page/size/totalElements/totalPages/first/last/sort |
| **SharingPostResponse** | 상세 응답 | (위 필드 + ) noteId, memberId, bodySnapshot(전체), ownedByMe, hiddenAt, deletedAt, verseSnapshot(Detail) |
| **VerseSnapshot** | 목록용 절 스냅샷 | rangeLabel("창세기 1:1-5") |
| **VerseSnapshotDetail** | 상세용 절 스냅샷 | rangeLabel + verses(현재 빈 배열, v2) |
| **VerseLine** | 절 1줄 | label, koreanText |
| **ToggleLikeUseCase** | ⚠️스켈레톤 | 예정: like/unlike(post) |
| **CommentUseCase** | ⚠️스켈레톤 | 예정: 댓글 create/delete |
| **PublishNoteUseCase** | ⚠️스켈레톤 | 예정: publish(노트→나눔, confirmNicknamePublic 필수, 스냅샷 박제) |
| **CommentCreateRequest** | ⚠️스켈레톤 | 예정: content |
| **CommentResponse** | ⚠️스켈레톤 | 예정: id, memberId, content, createdAt |
| **PublishNoteRequest** | ⚠️스켈레톤 | 예정: confirmNicknamePublic(필수), commentsEnabled |

## 🗑️ 토큰공유(Share) 갈래 — 죽은 코드 (명세 없음·비회원접근 위배·앱전용 불가 → 삭제 검토)

| 파일 | 역할 | 예정 필드/비고 |
|---|---|---|
| **Share** (Entity) | ⚠️스켈레톤 | ownerId, resourceType(QT/NOTE/STUDY), resourceId, **shareToken**(URL노출), snapshotId, sharedAt, expiresAt, revoked, revokedAt |
| **ShareSnapshot** (Entity) | ⚠️스켈레톤 | resourceType, originalResourceId, **snapshotJson**(원본 JSON 박제), createdAt |
| **ShareRepository** | ⚠️스켈레톤 | findByShareToken..., findByOwnerId... 예정 |
| **SharingService** | ⚠️스켈레톤 | createShare/getSnapshot/revokeShare 예정 (client 4종 사용) |
| **SharingController** | ⚠️스켈레톤 | `/api/v1/shares`: POST(생성), **GET /token/{token}(비로그인)** ←정책 위배, DELETE |
| **CreateShareUseCase** | ⚠️스켈레톤 | `(memberId, ShareCreateRequest)` → `SharedSnapshotResponse` |
| **GetSharedSnapshotUseCase** | ⚠️스켈레톤 | `(shareToken)` → `SharedSnapshotResponse` (비로그인) |
| **RevokeShareUseCase** | ⚠️스켈레톤 | `(memberId, shareId)` → `void` (owner만) |
| **ShareCreateRequest** | ⚠️스켈레톤 | resourceType, resourceId, expiresAt |
| **SharedSnapshotResponse** | ⚠️스켈레톤 | snapshotId, shareToken, resourceType, snapshotJson, ownerNickname, sharedAt, expiresAt, revoked |
| **client/member/GetMemberUseCaseMock** | ⚠️스켈레톤 | 토큰공유가 회원 조회용 |
| **client/note/GetNoteUseCaseMock** | ⚠️스켈레톤 | 토큰공유가 노트 원본 조회용 |
| **client/qt/GetQtUseCaseMock** | ⚠️스켈레톤 | 토큰공유가 QT 원본 조회용 |
| **client/study/GetStudyUseCaseMock** | ⚠️스켈레톤 | 토큰공유가 스터디 원본 조회용 |

> 🗑️ Share 갈래 13개는 Share 삭제 시 함께 제거 대상. 삭제하면 sharing 36→23개.

---

## 핵심 개념 5개

| 개념 | 설명 |
|---|---|
| **스냅샷** | 공개 시점 박제 — 원본 노트/닉네임이 바뀌어도 공유본은 그대로 (nicknameSnapshot, snapshotBody 등) |
| **activeUniqueKey** | 묵상 노트 하루 1건 강제 키. 삭제 시 null로 풀어 재작성 허용 |
| **likedByMe** | 사용자별 "내가 좋아요 눌렀나". 글마다 조회하면 N+1 → IN 절 배치로 1회 조회 |
| **ownedByMe** | "내가 쓴 글인가"(상세 전용). 수정/삭제 버튼 노출 판단용 |
| **정렬 화이트리스트** | sort는 URL 문자열이라 허용 필드만 통과(보안·안정성). publishedAt→createdAt 변환 |

## 구현 상태 요약
```
note(33)    : 거의 다 구현 ✅ (JournalEvent·member mock만 스켈레톤)
sharing(36) : 나눔 조회(SharingPost) 구현 ✅ / 나눔 쓰기(좋아요·댓글·공개) 스켈레톤 ⚠️
            : 토큰공유(Share) 13개 = 죽은 코드 🗑️
```

---

## 📌 정정 (Addendum) — 2026-05-29 (W2 마감 교차검증)

> W2 마감 시 구현_리스트.md(김지민 파트2)와 코드 전수 grep으로 교차검증하며 위 본문의 두 가지를 정정한다. **본문은 보존하고 아래로 덮어쓴다.**

### 정정 1 — `note/client/member/GetMemberUseCaseMock`은 "미사용 잔재"가 아니다

- 본문(line 77, 145)에서 "미사용 잔재 / member mock만 스켈레톤"으로 표기했으나 **부정확**하다.
- 실제로는 **8개 도메인(study·sharing·report·qt·praise·notification·note·mission)에 동일하게 존재하는 전역 스캐폴드**다. `member.api.GetMemberUseCase` 포트가 **실재**하고, 각 도메인은 CLAUDE.md §4 규칙(`client/{타도메인}/...UseCaseMock.java`)대로 회원 조회가 필요해지면 채울 자리를 미리 깔아둔 것이다.
- **W3 `PublishNoteUseCase`(노트→나눔 공개)가 `nickname_snapshot`을 채우려면 `members.nickname`을 조회**해야 한다 → 바로 이 member 클라이언트가 쓰일 자리. **죽은 코드가 아니라 곧 쓸 미충전 스캐폴드.**
- → **삭제 금지.** note 것만 지우면 나머지 7개 도메인과 규칙이 깨지고 W3에서 다시 만들어야 한다.

### 정정 2 — report(신고) 도메인이 본문에서 누락됐다

- 본문은 note(33)+sharing(36)만 다루나, **신고는 김지민 분담**(구현_리스트 파트2: `POST /api/v1/reports`)이다. report 도메인은 실재하며 아래 파일로 스캐폴딩돼 있다.

| 파일 | 역할 | 상태 |
|---|---|---|
| `report/api/CreateReportUseCase` | 신고 접수 포트 | 스켈레톤 |
| `report/api/GetReportUseCase` | 신고 조회 포트 | 스켈레톤 |
| `report/api/dto/ReportCreateRequest` / `ReportResponse` | 요청/응답 DTO | 스켈레톤 |
| `report/internal/Report` (Entity) | 신고(reports 테이블) | 스켈레톤 |
| `report/internal/ReportRepository` / `ReportService` | DB·로직 | 스켈레톤 |
| `report/web/ReportController` | `/api/v1/reports` | **`// TODO` 미배선** |
| `report/client/member/GetMemberUseCaseMock` | 회원 조회 스캐폴드 | (정정1과 동일 패턴) |
| `report/client/sharing/GetSharedSnapshotUseCaseMock` | 나눔 스냅샷 조회 스캐폴드 | 스켈레톤 |

- → `POST /api/v1/reports`는 **W3 작업**(컨트롤러 배선 + Service 구현). 빠뜨린 게 아니라 일정상 이연.

### 정정 3 — JournalEvent 소유 주의

- 본문(line 60)은 note 스켈레톤으로 적었으나, **엔티티 소유·이벤트 발행은 이지윤**(구현_리스트 line 25 "엔티티 소유는 이지윤", line 129 이지윤 "묵상 완료 비동기 이벤트 발행"). 김지민이 빠뜨린 항목이 아니다.
