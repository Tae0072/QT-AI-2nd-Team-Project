# 진행 중 Todo — W2 수요일 마감

> **현재 브랜치**: `bugfix/note-category-error-codes`
> **목표**: 에러코드 개선 PR 생성 + 내일 나눔 도메인 시작
> **최종 업데이트**: 2026-05-27 (수요일 마감 시점)

---

## 진행 상황 한눈에

```
[██████████████████████████░░░░░░░░░░░░] W2 수요일 완료 (27/49 = 55%)

✅ 완료     ⏳ 대기     ⬜ 예정
```

| W2 | 상태 |
| --- | --- |
| 월(공휴일) | — |
| 화(B1 GET /notes) | ✅ 완료 (PR #94 머지 완료) |
| 수(B2 POST /notes + 에러코드 개선) | ✅ 학습+구현 완료, PR 대기 (GitHub 500) |
| 목(B3 GET /sharing-posts) | ⬜ 내일 시작 예정 |
| 금(B4 GET /sharing-posts/{id} + W2 마무리) | ⬜ |

---

## 오늘 완료한 항목

### ✅ B2 POST /api/v1/notes 학습 + 구현

- DTO 검증 위치 결정 (③번 — @Valid + Service 분리)
- CreateNoteUseCase + NoteCreateRequest + NoteResponse 작성
- NoteService.create() + 카테고리별 switch 검증
- NoteController POST (201 Created)
- 테스트 3종 (단위 8건 + 컨트롤러 2건 + 통합 1건)
- 빌드+테스트 전체 통과
- **이지윤 CRUD PR(#97, #113, #116)과 겹침 발견 → merge 취소**

### ✅ dev 코드 리딩 + 비교

- create 비교: 내 에러코드 분리가 더 나음, @Valid는 이미 동일, 응답 필드는 내일
- PATCH 학습: 찾기→인가→삭제확인 3단계 + delete-and-reinsert 패턴
- DELETE 학습: 소프트 삭제 + 멱등성

### ✅ 에러코드 개선 PR

- ErrorCode N0003~N0007 추가
- NoteService.validateForSave() + normalize() 에러코드 교체
- NoteServiceTest 3건 에러코드 매칭 수정
- commit + push 완료 (`bugfix/note-category-error-codes`)

---

## 대기 항목

### ⏳ PR 생성 — GitHub 500 에러

- push 완료. GitHub 서버 복구 후 PR 생성 예정
- PR 본문 준비 완료 (리포트에 첨부)

---

## 내일 (목요일) Quick Start

### 1️⃣ 첫 명령

```
오늘 W2 목요일 시작
```

### 2️⃣ 내일 작업 목록

| # | 작업 | 예상 시간 |
| --- | --- | --- |
| 1 | PR 생성 (GitHub 500 해소 후) | 5분 |
| 2 | 개선 3: NoteSaveResponse → API 명세 응답 필드 확장 | 1시간 |
| 3 | B3: GET /api/v1/sharing-posts 나눔 목록 조회 | 3~4시간 |

### 3️⃣ B3 시작 조건

- 이승욱 SharingPost Entity 머지 여부 확인 필요
- 미머지면 B4로 순서 변경 또는 Entity 직접 생성

---

## 막힌 부분 / 다른 사람 의존성

### 1. GitHub 500 에러 — PR 생성 대기

- 코드 push는 완료. 서버 복구 대기

### 2. 이지윤 — 역할 겹침 전달 완료

- 노트 CRUD PR #97에서 김지민 담당 영역 포함
- 슬랙으로 확인 요청 전달 완료

### 3. 이승욱 — SharingPost Entity

- 내일(목) B3 시작 전 머지 여부 확인

---

## 참고 문서

- 오늘 리포트: [reports/2026-05-27_note-error-code-improvement_report.md](../reports/2026-05-27_note-error-code-improvement_report.md)
- W2 워크플로우: [workflows/2026-W2_상세-워크플로우.md](../workflows/2026-W2_상세-워크플로우.md)
- 이지윤 workflow: dev 브랜치 `doc/workspaces/DevA_이지윤/workflows/2026-05-27_note-domain-completion.md`
