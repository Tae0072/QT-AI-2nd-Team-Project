# 진행 중 Todo — W3 나눔 쓰기 (공개·좋아요·댓글 완료 → PR)

> **목표**: 나눔 쓰기(공개·좋아요·댓글) 백엔드 API 완성
> **브랜치**: `feature/sharing-write` (최신 dev 기반, 한 브랜치 = 나눔 쓰기 묶음, 기능별 커밋)
> **완료 기준**: 기능별 커밋 + 테스트 3종 + dev-console로 동작 눈 확인 → PR 1개
> **최종 업데이트**: 2026-06-01 (공개·좋아요·댓글 3기능 코드+테스트+dev-console 완료 → 마무리·PR)
> **상세**: [workflows/2026-W3_상세-워크플로우.md](../workflows/2026-W3_상세-워크플로우.md) · [진행현황](../김지민_작업_진행현황.md)

---

## ✅ 오늘(2026-06-01) 한 일 — 나눔 쓰기 3기능 완성

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
| 공유글 삭제 `DELETE /sharing-posts/{id}` | W3 범위 밖 → 별도 작업으로 이월 |
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
