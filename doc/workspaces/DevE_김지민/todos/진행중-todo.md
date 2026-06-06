# 진행 중 Todo — M-05 내 나눔 관리 화면

> **목표**: M-05(내 나눔 관리) Flutter 화면 — 내가 쓴 나눔 글 목록 + 숨김/되돌리기/삭제
> **브랜치**: `feature/sharing-my-posts-screen` (origin/dev 최신 기반, note PR #246 머지 후)
> **완료 기준**: 목록 조회 + ⋮메뉴(상태별 숨김/되돌리기/삭제) + 삭제 확인 + `flutter analyze` 무경고
> **구현 방식**: 학습 모드(SKILL 🟢 강 모드)
> **최종 업데이트**: 2026-06-05 (M-05 구현 완료)
> **상세**: [workflows/2026-06-05_M05-내나눔관리-워크플로우.md](../workflows/2026-06-05_M05-내나눔관리-워크플로우.md)

---

## ✅ M-05 내 나눔 관리 (2026-06-05) 완료 — `flutter analyze` 무경고

- [x] 모델 `MySharingPostItem`(+페이징) — 04 §4.4.5 필드(닉네임·미리보기 없음, status로 분기)
- [x] repository `getMySharingPosts`/`hidePost`/`showPost` (deletePost 재사용)
- [x] provider `mySharingPostsProvider` (공개+숨김 한 목록, 변경 후 invalidate)
- [x] `my_sharing_screen.dart` — 목록 + 상태 뱃지 + **⋮ 메뉴(공개중=숨김/삭제, 숨김=되돌리기/삭제)** + 삭제 확인창 + 탭→상세
- [x] 라우팅 `mySharing` + **나눔 피드 AppBar "내 나눔" 진입**(팀 합의 — 06 마이페이지 진입 대신)
- [x] 문서 반영 요청 메모([workflows/2026-06-05_문서반영요청-M05-진입점.md](../workflows/2026-06-05_문서반영요청-M05-진입점.md))

> 📌 진입점: 06은 마이페이지(M-01) 진입으로 정의 → 팀 합의로 나눔 피드 진입 구현. 06 갱신은 문서 담당에 요청(#decisions).
> ⏳ 다음: commit → push → PR(base dev). 마이페이지 진입은 QuickMenuCard 소유자 협의 시 추가(옵션).

---

# (아카이브) W3 Flutter 노트 화면

> 노트(N-01~04)·달력·외부공유·서식툴바·나눔 댓글/신고 — `feature/note-flutter-screens` → dev 머지 완료(#246)

---

## ✅ Day1(목 2026-06-04) 완료 — Flutter 노트 화면 N-01~03

- [x] note 피처 골격(models/services/providers) — sharing 복제
- [x] N-01 노트 목록(카테고리 6칩 + `GET /notes` + 빈/에러 + DRAFT 뱃지)
- [x] N-02 카테고리 선택(기도/회개/감사 → arguments 전달)
- [x] N-03 작성(제목+본문, **저장+임시저장**, `POST /notes`, popUntil 목록)
- [x] 라우팅 case + 홈 5탭 + `flutter analyze` 무경고
- [x] 에뮬레이터 레벨1 UI 스모크(DEV_FORCE_HOME) — 화면·이동·폼·에러처리 확인
- [x] 커밋(5a96d52) + **dev 머지**(f2e96ed): dev #233(웜파스텔 5탭+실제 성경화면) 채택 + 노트 탭에 NoteListScreen 연결, placeholder 제거

> 📌 실데이터·저장 왕복은 레벨2(백엔드 `docker compose up --build` + 카카오 로그인) 필요 — 추후.

## ✅ Day2(금 2026-06-05) 완료 — 전 항목 + `flutter analyze` 전체 무경고

- [x] **N-04 상세/수정/삭제** — 수정=N-03 편집모드 재사용(설계 A) + 전달 i(noteId 재조회) + `NoteEditArgs`. 묵상은 [수정] 숨김(화면 부재→회의)
- [x] **N-01 달력 탭** — `table_calendar` 점 표시 + 월이동 재조회 + 토글
- [x] **외부공유** — `note_share_sheet`(텍스트/카드이미지 RepaintBoundary), share_plus 12.x
- [x] **서식 툴바** — 마크다운 마커(설계 A, body=평문 유지). 색/크기/하이라이트는 회의 안건
- [x] **나눔 댓글/신고 보완** — `sharing_detail` placeholder 제거, 댓글 CRUD + 신고 시트 연결
- [x] 🆕 **명세 대조** — 설교노트 PATCH 절 손실 버그 차단([수정] 게이트 `writableNoteCategories`로)
- [x] 🆕 **학습 문서** — `study-notes/2026-06-05_W3-Day2-Flutter개념정리`, `..._table_calendar-초보자-완전가이드`
- [x] **W3 마감** — 리포트·워크플로우·todo·회고·**커밋 완료** (push·PR은 김지민 직접)

> 📌 패키지 3종(table_calendar/share_plus/path_provider) pubspec 추가 → 팀원 pull 후 `flutter pub get`(+cold restart).
> 🗣️ 회의 안건 6건 정리: `workflows/2026-06-05_회의-안건-목록.md`

---

## (백엔드 완료 기록) W3 나눔 쓰기 — 아카이브

---

## ✅ 오늘(2026-06-04) 한 일 — 내 나눔 목록 조회 API (`feature/sharing-my-posts`)

- **API** `GET /api/v1/me/sharing-posts` (F-10, 화면 M-05, 04 §4.4.5 #56) — V1 점검 중 발견한 **유일 누락 엔드포인트**. hide/show/delete의 짝.
- **설계 결정 2개**: ① 응답은 **신규 lean DTO**(공개 피드 재사용 X — 04가 4필드 적게 정의, 계약 정확성+결합 끊기) ② status 생략 시 **PUBLISHED+HIDDEN**(삭제 제외, `DELETED`·이상값→400).
- **구현**: `ListMySharingPostsUseCase`(포트) + `MySharingPostListItem/Response`(lean DTO) 신규 / `SharingPostService.listMine()`+`resolveStatuses()`(검증) / `findByMemberIdAndStatusIn`(파생쿼리) / 컨트롤러 핸들러. ErrorCode 신규 없음(`INVALID_INPUT` 재사용).
- **회귀**: 컨트롤러 필드 추가로 `@RequiredArgsConstructor` 생성자 인자 수가 바뀌어 깨진 **기존 SharingPostControllerTest 생성자 7인자로 수정**.
- **테스트** 6개(서비스3·컨트롤러2·통합1) — compileTestJava 통과(실행은 CI). dev-console "내 나눔 목록" 섹션 + **Docker(MySQL) 8081 스모크 통과**(401/200/200/400).
- **PR 규모**: 신규 3 + 수정 6 = 9파일, +217/-4. dev-console.html은 gitignore라 제외.
- **착수 점검**: `sharing-posts`(77 behind)·`sharing-post-delete`(PR #208 머지 끝) 둘 다 재사용 부적합 → origin/dev 최신 기반 새 브랜치.

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
