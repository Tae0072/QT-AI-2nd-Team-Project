# 2026-06-14 나눔 게시글 저장(북마크) — Phase 1

나눔 기능 확장 2단계 중 **1단계(저장/북마크)**. 2단계(닉네임 #태그)는 별도 PR.

## 요청
- 나눔 피드에서 공유된 게시글을 책갈피처럼 저장할 수 있어야 한다.
- 나눔 피드 카테고리 줄 **제일 오른쪽에 '저장' 버튼**을 두고, 누르면 저장한 글 목록을 볼 수 있어야 한다.

## 결정 (Lead)
- 저장 목록 보관 위치: **서버 DB** (기기를 바꿔도 유지). → `sharing_bookmarks` 테이블 + API 신설.
- 진행 순서: 저장(1단계) → 태그(2단계) 단계별 PR.

## 구현 — 백엔드 (service-note `domain.sharing`)
- 스키마(admin-server 단독 소유): `V41__create_sharing_bookmarks.sql`
  - `sharing_bookmarks(id, sharing_post_id, member_id, created_at)`, `(sharing_post_id, member_id)` UNIQUE,
    FK→sharing_posts/members, `idx_sharing_bookmarks_member(member_id, created_at DESC)`.
  - 좋아요(`post_likes`)와 동일 구조 — updatedAt 불필요, createdAt만 보관(저장 시각=목록 정렬 기준).
- 도메인 신규 파일(좋아요 패턴 그대로):
  - `internal/SharingBookmark.java` (엔티티), `internal/SharingBookmarkRepository.java`
  - `api/ToggleBookmarkUseCase.java`, `api/ListBookmarksUseCase.java`, `api/dto/BookmarkResponse.java`
  - `internal/SharingBookmarkService.java` (저장/해제/내 저장 목록)
  - `web/BookmarkController.java`
    - `POST   /api/v1/sharing-posts/{postId}/bookmark` (201) — 멱등(이미 저장 시 중복 INSERT 없이 true)
    - `DELETE /api/v1/sharing-posts/{postId}/bookmark` (204) — 멱등
    - `GET    /api/v1/me/bookmarks` — 내 저장 글(피드와 동일 `SharingPostListResponse`, 최근 저장순)
- 피드/상세에 저장 표시:
  - `SharingPostListItem`·`SharingPostResponse`에 `bookmarkedByMe` 추가(좋아요 `likedByMe`와 동일).
  - `SharingPostService`에 `SharingBookmarkRepository` 주입 → 목록은 배치 조회(N+1 방지), 상세는 단건 조회.
- 정책: 저장은 PUBLISHED 글만(없으면 404). 저장 후 글이 숨김·삭제되면 저장 목록에서 자동 제외.
- 쓰기 로직을 별도 `SharingBookmarkService`로 분리해 기존 `SharingPostService`(7 UseCase)·테스트 영향 최소화.

## 구현 — 프론트(Flutter)
- `SharingRepository`: `bookmark()`, `unbookmark()`, `getBookmarks()` 추가.
- 모델 `SharingPostItem`·`SharingPostDetail`에 `bookmarkedByMe`(+copyWith).
- `sharing_providers.dart`:
  - 피드 `SharingFeedNotifier.toggleBookmark()` (낙관적+롤백)
  - 상세 `SharingDetailNotifier.toggleBookmark()` (낙관적+롤백)
  - `bookmarksProvider`(`BookmarksNotifier`) — 저장 목록 + `removeBookmark()`(목록에서 즉시 제거, 실패 복원)
- UI:
  - 피드 카드(`PostCard`) 액션 줄 오른쪽 끝에 저장 토글 아이콘(`onBookmark`).
  - 나눔 피드 카테고리 줄을 `Row(Expanded(ListView) + 구분선 + '저장' 버튼)`으로 바꿔, **저장 버튼을 항상 오른쪽 고정**.
  - 저장 목록 화면 `sharing_bookmarks_screen.dart` 신설(라우트 `/sharing/bookmarks`). 빈 상태 안내 포함.
  - 상세 화면 AppBar에 저장 토글 액션 추가.

## 검증
- 백엔드: `:service-note:test` 전체 GREEN (신규 `SharingBookmarkServiceTest` 5건, 기존 `SharingPostServiceTest` 11건, `DomainBoundaryTest` 포함). admin-server `compileJava` OK.
- 프론트: `flutter analyze` 무이슈, `flutter test` 전체 **309개 통과**(신규 북마크 노티파이어 5건 + 카드 2건 포함).
- 2~3회 검토: 좋아요 패턴과 1:1 대응(멱등·낙관적·N+1 배치·시각 Clock 주입), 도메인 경계(api/internal/web) 준수 확인.

## 참고/후속
- OpenAPI(`apis/api-v1/openapi.yaml`)에는 나눔 엔드포인트(좋아요·댓글 포함)가 아직 미기재 상태 → 본 PR도 동일하게 코드만. 추후 나눔 전체를 묶어 스펙 반영 권장.
- admin-server는 sharing web 컨트롤러가 없어(api+internal만) 저장 기능은 service-note에만 추가. 스키마(V41)만 admin-server 소유.
- 다음(Phase 2): 닉네임 `#태그` — 멘션 + 글쓴이 이름까지 닉네임 변경 시 일괄 반영(스냅샷 정책 F-10 변경, Lead 승인). 별도 PR.

## Git/PR
- 브랜치 `feature/sharing-bookmarks` → PR 대상 `dev`. 커밋 `feat(sharing): 나눔 게시글 저장(북마크) 기능`.
