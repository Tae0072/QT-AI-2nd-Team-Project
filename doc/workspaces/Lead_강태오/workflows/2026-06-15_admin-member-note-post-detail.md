# 관리자 회원 상세 — 노트·공유글 상세 조회 (2026-06-15)

## 1. 요청
회원 상세의 노트/공유글 목록에서 **행을 클릭하면 본문 전체를 상세 조회**할 수 있게.

## 2. 구현 (도메인 경계 준수, 기존 UseCase 확장)
### 백엔드(admin-server)
- note: `ListMemberNotesForAdminUseCase`에 `getNoteDetailForMember(memberId, noteId)` 추가(+`AdminNoteDetail`:
  본문 body + 묵상 4섹션 R/I/A/P). `AdminNoteQueryService` 구현. 소유 검증(다른 회원 노트면 NOTE_NOT_FOUND).
- sharing: `AdminMemberSharingQueryUseCase`에 `getPostDetailForMember(memberId, postId)` 추가
  (+`AdminMemberPostDetail`: snapshotBody·구절 라벨·좋아요/댓글 수 등). 소유 검증(SHARING_POST_NOT_FOUND).
- `AdminMemberController`: `GET /{memberId}/notes/{noteId}`, `GET /{memberId}/posts/{postId}` (OPERATOR).
  - 기존 UseCase 인터페이스 확장이라 컨트롤러 생성자/주입 변화 없음 → 컨트롤러 테스트 영향 없음.

### 프런트(admin-web)
- `api/members.ts`: `getMemberNote`, `getMemberPost` + `AdminNoteDetail`/`AdminMemberPostDetail` 타입.
- `MembersPage`: `PagedSubTable`에 `onRowClick` 추가. 노트·공유글 탭의 행 클릭 시 중첩 모달로 본문 전체 표시
  (제목·메타 + 본문/섹션). 노트 본문은 전체, 공유글은 공개 스냅샷 본문.

## 3. 노출 정책
- 운영 식별 목적의 읽기 전용 상세. 노트 본문/섹션은 운영자에게만 보이며 개인정보(이메일·카카오ID)는 미노출.

## 4. 검증
- admin-server: compile + `:admin-server:test`(회원 컨트롤러·경계/ArchUnit) BUILD SUCCESSFUL.
- admin-web: `tsc --noEmit` 무오류, `npm run build` 성공.

## 5. 참고 (이 PR과 무관한 기존 dev 잔재)
- admin-web 계약 테스트(admin-page-contracts) 1건(`praise song payload remains metadata-only`)이 clean dev에서도
  실패한다. 찬양→배경음악 통합 후 PraiseSongs 계약/페이지가 미정리로 남은 별도 부채로, 본 PR 범위가 아니다(별도 정리 권장).
