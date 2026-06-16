# 관리자 회원 상세 확장 — 노트/공유글/댓글/좋아요/미션 (2026-06-15)

## 1. 요청
회원 상세에서 그 회원의 **작성 노트 전체, 공유한 글 전체, 작성 댓글 전체, 좋아요한 글 전체, 미션 진행률**을 볼 수 있게.
(닉네임 변경 이력은 별도 PR②에서 — 이력 테이블이 없어 인프라 신설 필요.)

## 2. 구현 (도메인 경계 준수)
member 도메인이 note/sharing 내부를 직접 호출하지 않도록, 각 도메인의 `api/UseCase`로만 본다.

### 백엔드(admin-server)
- note: `ListMemberNotesForAdminUseCase`(+`AdminNoteItem`) / `AdminNoteQueryService`(기존 `NoteRepository.search` 재사용, 전체 상태).
- sharing: `AdminMemberSharingQueryUseCase`(공유글/댓글/좋아요 3종, +DTO 3개) / `AdminMemberSharingQueryService`.
  - 좋아요는 나눔 운영(AD-13 전체)과 분리된 가벼운 읽기. `PostLike` 페이지 → 글 배치 로드로 N+1 회피.
  - 리포지토리 가산: `SharingPostRepository.findByMemberIdOrderByCreatedAtDesc`,
    `CommentRepository.findByMemberIdOrderByCreatedAtDesc`, `PostLikeRepository.findByMemberIdOrderByCreatedAtDesc`.
- mission: 기존 `GetMemberMissionProgressUseCase` 재사용.
- `AdminMemberController`에 엔드포인트 5개 추가(모두 OPERATOR):
  - `GET /{id}/notes`, `/{id}/posts`, `/{id}/comments`, `/{id}/likes`(페이징), `/{id}/missions`(전체).
- 컨트롤러 테스트 생성자/목 갱신.

### 프런트(admin-web)
- `api/members.ts`: 5개 조회 함수·타입 추가.
- `MembersPage.tsx`: 상세 모달을 **탭(요약·노트·공유글·댓글·좋아요·미션)** 으로 확장. 각 탭은 처음 열릴 때 1회 로드,
  페이징 서브테이블 + 미션은 진행률 바 목록.

## 3. 노출 정책
- 노트 본문 전체는 노출하지 않고 메타데이터(제목·분류·상태·공개·작성일)만. 댓글은 운영 식별을 위해 본문·삭제여부 표시.
- 개인정보(이메일·카카오ID)는 기존대로 미노출.

## 4. 검증
- admin-server: compile + `:admin-server:test` 전체 BUILD SUCCESSFUL(경계/ArchUnit 포함).
- admin-web: `tsc --noEmit` 무오류 + `npm run build` + 계약 테스트 PASS.

## 5. 후속
- PR②: 닉네임 변경 이력(테이블 신설 + service-user 기록 + 조회).
