# 진행 중 Todo — W3 나눔 쓰기 (공개·좋아요·댓글 완료 → PR)

> **목표**: 나눔 쓰기(공개·좋아요·댓글) 백엔드 API 완성
> **브랜치**: `feature/sharing-write` (최신 dev 기반, 한 브랜치 = 나눔 쓰기 묶음, 기능별 커밋)
> **완료 기준**: 기능별 커밋 + 테스트 3종 + dev-console로 동작 눈 확인 → PR 1개
> **최종 업데이트**: 2026-06-02 (공개·좋아요·댓글 #191 dev 머지 / 공유글 삭제·숨김·되돌리기 신규 완료 → PR)
> **상세**: [workflows/2026-W3_상세-워크플로우.md](../workflows/2026-W3_상세-워크플로우.md) · [진행현황](../김지민_작업_진행현황.md)

---

## ✅ 오늘(2026-06-02) 한 일 — 공유글 삭제·숨김·되돌리기 (`feature/sharing-post-delete`)

- **삭제** `DELETE /sharing-posts/{id}`: 작성자 본인만 soft delete(status=DELETED, deletedAt), 멱등.
- **숨김** `PATCH /sharing-posts/{id}/hide`: PUBLISHED→HIDDEN(hiddenAt), 멱등.
- **되돌리기** `PATCH /sharing-posts/{id}/show`: HIDDEN→PUBLISHED(hiddenAt=null), 멱등. **04에 없던 신규 API**(팀 상의로 v1 포함, §4.4.6에 추가).
- **설계**: 엔티티 delete/hide/show(전이+검증, DELETED는 409) + 서비스 멱등 + `findOwnedPost` 헬퍼(조회+본인검증). 관리자 권한은 admin 도메인 이후(TODO).
- **테스트** 16개(서비스10·컨트롤러6) + dev-console "나눔 글 관리" 섹션 + **Docker(MySQL)로 스모크 통과**(숨김/되돌리기/삭제 204, 타인 403, 없음 404, 삭제후숨김 409).
- **착수 점검**: 어제 작업이 #191로 dev 머지됨 확인 → `feature/sharing-write`(21커밋 stale) 대신 origin/dev 최신 기반 새 브랜치.
- **후속**: 04 SSoT는 gitignore 로컬 캐시만 갱신 → 문서저장소 별도 반영 필요(Lead). ERD `deleted_at` 불일치 02/04 정합 후보.

---

## ✅ 이전(2026-06-01) — 나눔 쓰기 3기능 (#191 dev 머지 완료)

- **공개** `POST /notes/{id}/share`: ⑤ NoteController 배선 + 테스트 3종 + dev-console. (①~④는 5/29)
- **좋아요** `POST·DELETE /sharing-posts/{id}/like`: COUNT 재계산·중복 409·취소 멱등 204. 컨트롤러 **지민 직접**. 테스트 11개. (화→월 당겨옴)
- **댓글** `POST·GET /sharing-posts/{id}/comments` + `DELETE /comments/{id}`: 평면 댓글·body·댓글OFF 409·본인만 403·soft delete 멱등. CommentService/CommentController(**지민 직접**)/CommentRepository 신규. 테스트 16개. (목→월 당겨옴)
- **공통**: dev-console에 공개/좋아요/댓글 3섹션. `ErrorCode.DUPLICATE_LIKE`(S0004). bootJar·compileTestJava 통과.
- **⚠️ 테스트 로컬 CLI 실행 불가**(한글경로 인코딩) → CI 검증. 부팅은 `java -jar`로만.

---

## ✅ 완료 — 나눔 쓰기 3기능 (`feature/sharing-write`)

- [x] **공개** — PublishNoteRequest·existsByNoteId·SharingPost.publish·PublishNoteUseCase+publish·NoteController 배선 + 테스트 3종
- [x] **좋아요** — ToggleLikeUseCase·LikeResponse·like/unlike·PostLike.of·syncLikeCount·레포 보강·SharingPostController + DUPLICATE_LIKE + 테스트 11
- [x] **댓글** — Comment.of/markDeleted·syncCommentCount·CommentRepository·DTO 3·CommentUseCase·CommentService·CommentController + 테스트 16
- [x] dev-console 3섹션(공개·좋아요·댓글)
- [ ] (지민) dev-console로 공개·좋아요·댓글 눈 확인 (새 jar로 서버 재기동 후) — PR은 CI 검증이라 눈 확인이 막지 않음

---

## ⬜ 남은 작업 (별도)

| 작업 | 비고 |
| --- | --- |
| ~~공유글 삭제~~ | ✅ 2026-06-02 완료 (삭제·숨김·되돌리기, `feature/sharing-post-delete`) |
| 04 SSoT 문서저장소 반영 (show 추가) | Lead 상의 — gitignore 로컬 캐시만 갱신됨 |
| 관리자(ADMIN+OPERATOR) 강제 삭제·숨김 | admin 도메인 담당 이후 (코드 TODO) |
| 토큰공유(Share) 죽은코드 정리 | 강태오 결정 대기 |
| 외부공유(Flutter, share_plus) | W4·디자인 확정 후 |

> ❌ 신고(`POST /reports`)는 dev에 이미 완료(#140).

---

## 매일 검증 루틴

1. 앱 dev 실행 (`spring.profiles.active=dev`)
2. `http://localhost:8080/dev-console.html` → X-Dev-User-Id 입력 → 그날 버튼 → 응답 status·JSON
3. `http://localhost:8080/h2-console` → 테이블 행 확인 (JDBC `jdbc:h2:mem:qtai;...;MODE=MYSQL`, sa, pw 빈값)

> ✅ dev-console.html 생성됨(공개·좋아요·댓글 3섹션). **`.gitignore` 처리**(개인 dev 도구라 PR 제외). dev 부팅은 `java -jar build/libs/*.jar --spring.profiles.active=dev` + JWT env(테스트 키).

---

## ⏸️ 막힌 것 / 의존성

| 항목 | 상태 |
| --- | --- |
| git 커밋/push/PR | 마무리 단계 — **push 전 dev pull→merge**(origin/dev 4커밋 뒤) 후 PR 1개. 사용자 동의 후 |
| Flutter 디자인 확정 | 대기 — 확정돼야 화면 착수(외부공유 포함) |
| 토큰공유(Share) 죽은코드 | 강태오 결정 대기 |
| verses[] 다중 절 | v2 이관 |

---

## 다음 세션 Quick Start

```
오늘 W3 [요일] 시작
```
→ 나눔 쓰기 3기능 완료. 다음: **PR 처리** 또는 **공유글 삭제**(별도) 또는 **W4 Flutter**(디자인 확정 시).

---

## 참고 문서
- W3 워크플로우: [workflows/2026-W3_상세-워크플로우.md](../workflows/2026-W3_상세-워크플로우.md)
- 진행현황: [김지민_작업_진행현황.md](../김지민_작업_진행현황.md)
- 오늘 리포트: [reports/2026-05-29_W3착수-나눔공개-publish-문서정리_리포트.md](../reports/2026-05-29_W3착수-나눔공개-publish-문서정리_리포트.md)
- 파일 레퍼런스: [reports/2026-05-28_note-sharing-파일레퍼런스_report.md](../reports/2026-05-28_note-sharing-파일레퍼런스_report.md)
- API 계약: `doc/standards/04_API_명세서.md` §4.3.8·§4.4
