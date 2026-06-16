# 워크플로우 — 관리자 나눔 공유글 관리(AD-15) 재구성

- 날짜: 2026-06-16
- 작성: Lead 강태오 (with Claude)
- 브랜치: `feature/admin-sharing-posts` → PR to `dev`
- 기능: F-10 나눔(Sharing) 공유글 관리 — 관리자 웹에서 공유글 목록/상세/숨김/복원

## 1. 배경 — 왜 "복원"이 아니라 "재구성"인가

stash WIP 4기능 복원의 마지막 항목. 그런데 stash의 나눔 관리 코드는 **옛 sharing 도메인** 기준이라 dev와 설계가 갈렸다.

- stash `AdminSharingService`는 한 클래스에 (a) 관리자 목록/상세, (b) 회원 통계(countPostsByMember 등), (c) 정리(purge용 id 목록), (d) 숨김 복원/소프트 삭제를 전부 몰아넣은 god-service였다.
- 그런데 dev에는 이미 회원 통계(`MemberSharingStatsUseCase`)·정리(`PurgeMemberSharingDataUseCase`)·신고 연동 강제숨김(`HideSharingPostForModerationUseCase`/`SharingModerationService`)이 **별도 서비스로 분리**돼 있다. stash를 그대로 가져오면 같은 UseCase를 구현하는 Bean이 둘이 되어 충돌한다.
- 또 stash엔 하드 성격의 `DELETE /{id}` 엔드포인트가 있었는데, dev의 모더레이션 정책은 **숨김(soft) 중심**이다.

→ Lead 결정(2026-06-16): **dev 구조에 맞춰 깔끔히 재구성**. 목록/상세/숨김/복원만 하는 focused 서비스로 새로 작성하고, 회원통계·정리·하드삭제는 넣지 않는다.

## 2. 한 일

관리자 웹에 "나눔 공유글 관리(AD-15)" 화면을 추가했다. 신고 경유가 아니어도 운영자가 공유글을 직접 조회·숨김·복원할 수 있다(전체 상태 PUBLISHED/HIDDEN/DELETED 조회 가능).

### 백엔드 (admin-server, `domain.sharing` — admin 고유 기능)

- `api/AdminSharingPostUseCase.java`(신규) — 단일 포커스 인터페이스: listForAdmin / getForAdmin / hide / restore
- `api/dto/AdminSharingPostResponse.java`(신규) — 목록은 미리보기(bodyPreview), 상세는 전체 본문(body)·절 라벨·QT 날짜까지
- `internal/AdminSharingService.java`(신규) — `SharingPostRepository` + `Clock`만 의존. 숨김/복원은 엔티티 `SharingPost.hide(now)`/`show()`를 직접 사용(멱등 처리, 삭제본은 `INVALID_STATUS_TRANSITION`)
- `internal/SharingPostRepository.java`(수정) — 관리자 전체상태 검색 `searchForAdmin(status, q, pageable)` **한 메서드만** 추가(status null=전체, 제목·닉네임 LIKE). 기존 사용자 피드 `search`는 건드리지 않음
- `web/AdminSharingController.java`(신규) — `/api/v1/admin/sharing-posts` (GET 목록, GET 상세, PATCH /hide, PATCH /restore). 권한 ROLE_ADMIN + admin_users OPERATOR(report 컨트롤러 requireOperator 패턴)
- `internal/AdminSharingServiceTest.java`(신규) — 숨김/복원 멱등, 삭제본 차단, 404 단위 테스트 7건

엔드포인트:

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/v1/admin/sharing-posts?status=&q=&page=&size=` | 전체 상태 목록·검색(미리보기) |
| GET | `/api/v1/admin/sharing-posts/{postId}` | 상세(전체 본문) |
| PATCH | `/api/v1/admin/sharing-posts/{postId}/hide` | 숨김(PUBLISHED→HIDDEN) |
| PATCH | `/api/v1/admin/sharing-posts/{postId}/restore` | 복원(HIDDEN→PUBLISHED) |

### 프런트엔드 (admin-web)

- `api/sharingPosts.ts`(신규) — list/detail/hide/restore (delete 제외)
- `pages/SharingPostsPage.tsx`(신규) — 상태 필터 + 검색 + 보기(전체 본문 모달) + 숨김/복원. `usePagedList` 재사용
- `App.tsx`(수정) `/sharing-posts` 라우트, `menu.ts`(수정) AD-15(OPERATOR)

## 3. dev와의 충돌 회피 (재구성 포인트)

- **회원통계/정리(purge) 제외**: dev의 `MemberSharingStatsUseCase`/`PurgeMemberSharingDataUseCase`와 Bean 중복을 피하려고 본 서비스는 그 UseCase들을 구현하지 않는다.
- **하드 삭제 제외**: dev 숨김 기반 모더레이션과 일치시켜 숨김/복원만 제공. 화면의 삭제 버튼·API도 제거.
- **repo는 외과적 추가만**: `CommentRepository`/`SharingPostRepository`를 통째로 가져오지 않고, dev 현재 repo에 `searchForAdmin` 한 메서드만 추가.

## 4. 검증

- `:admin-server:compileJava` / `:admin-server:test` (AdminSharingServiceTest 7건 + ArchUnit/Modulith 경계) BUILD SUCCESSFUL
- admin-web `tsc --noEmit` 0 errors
- 도메인 경계: `domain.sharing`은 admin `api/VerifyAdminRoleUseCase`만 의존(인가). 엔티티/repo는 자기 도메인 내부. 타 도메인 internal import 없음

## 5. stash 복원 4기능 마무리

1. ✅ 미션 관리 AD-16 (#675)
2. ✅ 앱버전 백엔드 AD-19 (#678, V44→V50)
3. ✅ 셀프테스트 AD-18 (#680)
4. ✅ 나눔 공유글 관리 AD-15 (본 PR) — 재구성

후속(별도): AD-19 관리자 화면·헤더 적용 버튼·Flutter 버전체크, 앱 마이페이지 미션/찬양 FE WIP.
