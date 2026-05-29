# 진행 중 Todo — W2 목요일 마감 (B4까지 완료)

> **현재 브랜치**: `feature/sharing-posts`
> **목표**: 나눔 조회(B3+B4) PR 생성 + 공지 발송 → 금요일 W3 시작
> **최종 업데이트**: 2026-05-28 (목요일 마감, B4까지 완료)

---

## 진행 상황 한눈에

```
[██████████████████████████████████████] W2 백엔드 B1~B4 완료 (49/49 = 100%)

✅ 완료     ⏳ 대기     ⬜ 예정
```

| W2 | 상태 |
| --- | --- |
| 월(공휴일) | — |
| 화(B1 GET /notes) | ✅ 완료 (PR #94 머지) |
| 수(B2 POST /notes + 에러코드 개선) | ✅ 완료 (PR #123 머지) |
| 목(04 반영 + B3 목록 + B4 상세) | ✅ 구현+테스트 완료, PR 대기 |
| 금(원래 B4 계획) | ✅ 목요일에 당겨 완료 → 금요일은 PR·공지·W3 |

---

## 오늘 완료한 항목

### ✅ 에러코드/DTO 분리 PR dev 머지 (#123)
- 봇 리뷰 BLOCK 2회 대응(OpenAPI 분리 / 04 API 명세 에러코드 반영)

### ✅ 04 API 명세 에러코드 반영 (강태오 협의)
- §6.2 + §4.3.4 + §4.3.6에 N0004~N0007 반영
- 리포트: `reports/2026-05-28_04-api-error-code-reflection_report.md`

### ✅ B3 나눔 피드 목록 (GET /api/v1/sharing-posts, F-10)
- 이승욱 협의 후 V13: `nickname_snapshot` + `snapshot_verse_label` 추가
- DTO + Repository 2개 + Service + Controller
- N+1 방지(likedByMe 배치) + 정렬 화이트리스트 + q 이스케이프 + category/q 필터
- 테스트 11건

### ✅ B4 나눔 상세 (GET /api/v1/sharing-posts/{postId}, F-10)
- GetSharingPostUseCase + getDetail (404 + likedByMe + ownedByMe)
- 상세 DTO(SharingPostResponse + VerseSnapshotDetail + VerseLine)
- findByIdAndStatus(PUBLISHED만) → HIDDEN/DELETED/없는 글 404
- verses[]는 빈 배열(v2 이관 — 07 §19.2, 이지윤·이승욱 영역)
- 테스트 +6건 → 나눔 조회 전체 17건
- 리포트: `reports/2026-05-28_B3-나눔피드-조회_리포트.md` (B3+B4)

---

## 대기 항목

### ⏳ 나눔 조회 PR 생성 (B3+B4 한 PR)
- 코드·테스트 완료. PR 본문 보강 후 생성. `feature/sharing-posts` 한 브랜치 = 나눔 조회 기능

### ⏳ 팀 공지 3건 (미발송)
- V13 DB 변경(#decisions) / 이승욱 발행로직 / 이지윤 신규파일 — 초안은 B3 리포트에 있음

---

## 내일 (금요일) Quick Start

### 1️⃣ 첫 명령

```
오늘 W3 금요일 시작
```
(W2 B1~B4는 끝. 금요일부터 W3)

### 2️⃣ 내일 작업 목록

| # | 작업 | 예상 |
| --- | --- | --- |
| 1 | 나눔 조회 PR(B3+B4) 본문 보강 + 생성 | 30분 |
| 2 | 공지 3건 발송 | 10분 |
| 3 | W2 마무리 자기 점검 + 주간 회고 | 30분 |
| 4 | **W3 시작 — 브랜치 분리** | 본작업 |

### 3️⃣ W3 브랜치 분리 계획 (한 브랜치 = 한 기능)

| 브랜치 | 못다한 기능 |
| --- | --- |
| `feature/note-*` | 노트 미완: 노트→나눔 공유(POST /notes/{id}/share §4.3.8), JournalEvent(이벤트 이력) 등 |
| `feature/sharing-*` | 나눔 쓰기: publish / 좋아요(like) / 댓글(comment) — 전부 스켈레톤 |
| (보류) | 토큰 공유(Share) 갈래 — 담당·우선순위 협의 필요 |

> 인벤토리 상세는 B3 리포트 + 2026-05-28 스캔 결과 참조.

---

## 막힌 부분 / 다른 사람 의존성

### 1. 발행 로직(PublishNoteUseCase) 미구현
- 나눔 피드에 실제 데이터가 쌓이려면 발행 흐름 필요 (W3 sharing 브랜치)
- nickname_snapshot은 발행 시 반드시 채워야 함 (이승욱과 책임 공유)

### 2. verses[] 다중 절 = v2
- 절 본문 스냅샷 저장 구조 없음. 다중 절 선택은 v2(07 §19.2). 이지윤(절 데이터)·이승욱(설정) 협의 필요

---

## 참고 문서

- 오늘 리포트(B3+B4): [reports/2026-05-28_B3-나눔피드-조회_리포트.md](../reports/2026-05-28_B3-나눔피드-조회_리포트.md)
- 오늘 리포트(04 반영): [reports/2026-05-28_04-api-error-code-reflection_report.md](../reports/2026-05-28_04-api-error-code-reflection_report.md)
- W2 워크플로우: [workflows/2026-W2_상세-워크플로우.md](../workflows/2026-W2_상세-워크플로우.md)
