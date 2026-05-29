# 진행 중 Todo — W3 시작 (나눔 쓰기 + 신고)

> **목표**: 나눔 쓰기(공개·좋아요·댓글·삭제) + 신고 백엔드 API 완성
> **완료 기준**: 기능별 PR dev + 테스트 3종 + dev-console로 동작 눈 확인
> **공휴일**: 2026-06-03(수) 제외 — 실작업 4일(월·화·목·금)
> **최종 업데이트**: 2026-05-29 (W3 상세 워크플로우 생성)
> **상세**: [workflows/2026-W3_상세-워크플로우.md](../workflows/2026-W3_상세-워크플로우.md)

---

## 직전 W2 마감 요약 (참고)

- B1~B4(노트 GET/POST, 나눔 목록/상세) 100% 완료. B1 #94·B2 #123 머지 / B3+B4 열린 PR(강태오 머지 대기).
- 공지 3건 슬랙 전달, 주간회고(`reports/2026-W2_주간회고.md`), 파일레퍼런스 정정 addendum 완료.
- ⏳ **미완**: W2 마감 문서 7개 commit+push (W3 착수 전 정리에서 처리).

---

## W3 착수 전 정리 (월 오전)

- [ ] W2 마감 문서 7개 commit + push → 열린 PR 자동 갱신
- [ ] dev 최신 pull + B3+B4 PR 머지 상태 확인 (GitHub 웹/슬랙 봇)
- [ ] `dev-console.html` 뼈대 생성 (`static/`, X-Dev-User-Id 입력 + baseURL + 섹션틀)
- [ ] H2 콘솔(`/h2-console`) 접속 1회 확인 (흰 화면이면 DevSecurityConfig frameOptions 1줄)

---

## W3 일자별 작업 (한 브랜치 = 한 기능)

| 일 | 작업 | 브랜치 | 신규/주의 |
| --- | --- | --- | --- |
| **월 6/1** | 나눔 공개 `POST /notes/{id}/share` | `feature/sharing-publish` | 스냅샷 박제 원자성 + 닉네임(GetMemberUseCase) + cross-domain 배선 |
| **화 6/2** | 좋아요 `POST·DELETE /sharing-posts/{id}/like` | `feature/sharing-like` | PostLikeRepo 완성됨. likeCount 동기화·409 중복 |
| **화 6/2** | 공유글 삭제 `DELETE /sharing-posts/{postId}` | `feature/sharing-post-delete` | owner 검증·PUBLISHED/HIDDEN→DELETED |
| (수 6/3) | 🛌 공휴일 | — | — |
| **목 6/4** | 댓글 `POST·GET /comments` + `DELETE /comments/{id}` | `feature/sharing-comment` | **CommentRepository 신규** + ON/OFF 게이트 + commentCount |
| **금 6/5** | 신고 `POST /reports` + W3 마감 | `feature/report-create` | ⚠️ **reports 테이블 V14 신규(이승욱 협의)** + 04 정합(targetType/targetId) |

> 각 기능: 구현 → 테스트 3종 → dev-console 섹션 추가로 눈 확인 → H2로 DB 확인 → commit/push(+PR).

---

## 매일 검증 루틴 (W3 신규)

1. 앱 dev 실행 (`spring.profiles.active=dev`)
2. `http://localhost:8080/dev-console.html` → X-Dev-User-Id 입력 → 그날 기능 버튼 → 응답 status·JSON 확인
3. `http://localhost:8080/h2-console` → 테이블 행 확인 (JDBC `jdbc:h2:mem:qtai;...;MODE=MYSQL`, sa, pw 빈값)

---

## 막힌 부분 / 다른 사람 의존성

### 1. reports 테이블 없음 → V14 신규 (이승욱 협의)
- 마이그레이션 어디에도 reports 없음(V5는 sharing_posts/comments/post_likes만). 신고(금) 착수 전 이승욱과 V14 협의 — V13 닉네임 컬럼 전례와 동일.

### 2. Report 스켈레톤 ↔ 04 명세 충돌
- 스켈레톤 `share_snapshot_id`/PENDING → 04 `targetType`+`targetId`/RECEIVED로 정합 필요.

### 3. B3+B4 PR 머지 대기 (강태오)
- 같은 도메인 작업이라 미머지여도 W3 진행은 가능. 머지되면 베이스 갱신.

### 4. nickname_snapshot 채우기 (W2 부채 해소)
- 발행 로직(월)에서 `members.nickname` 박제로 W2 부채 해소. 이승욱과 책임 공유.

### 5. verses[] 다중 절 = v2
- 절 본문 스냅샷 저장 구조 없음. 다중 절은 v2(07 §19.2).

---

## 다음 세션 Quick Start

```
오늘 W3 월요일 시작
```
(W3 상세 워크플로우 생성 완료. 월요일 = 착수 전 정리 + 나눔 공개)

---

## 참고 문서

- W3 워크플로우: [workflows/2026-W3_상세-워크플로우.md](../workflows/2026-W3_상세-워크플로우.md)
- W2 회고: [reports/2026-W2_주간회고.md](../reports/2026-W2_주간회고.md)
- 파일 레퍼런스(+정정): [reports/2026-05-28_note-sharing-파일레퍼런스_report.md](../reports/2026-05-28_note-sharing-파일레퍼런스_report.md)
- API 계약: `doc/standards/04_API_명세서.md` §4.3.8·§4.4
