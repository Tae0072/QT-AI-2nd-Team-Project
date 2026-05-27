# 진행 중 Todo — W2 화요일 마감 (PR #92 머지 대기)

> **현재 브랜치**: `feature/note-freenote-impl`
> **목표**: PR #92 강태오 수동 머지 완료 후 W2 수요일 B2(POST /api/v1/notes) 시작
> **최종 업데이트**: 2026-05-26 (화요일 마감 시점)

---

## 진행 상황 한눈에

```
[████████████████░░░░░░░░░░░░░░░░░░░░░░] W2 화요일 완료 (16/49 = 33%)

✅ 완료     ⏳ 대기     ⬜ 예정
```

| W2 | 상태 |
| --- | --- |
| 월(공휴일) | — |
| 화(B1 GET /notes) | ✅ 완료 (PR #92 머지 대기) |
| 수(B2 POST /notes) | ⬜ 내일 시작 예정 |
| 목(B3 GET /sharing-posts) | ⬜ |
| 금(B4 GET /sharing-posts/{id} + W2 마무리) | ⬜ |

---

## 진행 중 — 대기 항목

### ⏳ PR #92 머지 (강태오 수동)

- **현재 상태**: 🟢 Open / Claude 자동 리뷰 2회 — BaseEntity.deletedAt BLOCK 유지 + WARN/INFO 모두 처리
- **블로커**: Claude가 BaseEntity 변경을 두 번 BLOCK으로 잡음 → 자동 머지 불가 → 강태오 수동 머지 필요
- **합의 사실**: 이승욱과 사전 협의 완료 (소프트 삭제 표준 패턴, F-04·F-10·F-13 공통 필요)
- **수동 머지 결정 이유**: 옵션 ①(Note 전용 이동)은 W3 나눔 PR에서 SharingPost·Comment 등에도 deletedAt 반복 추가 → 부채 누적. 옵션 ②(BaseEntity 유지 + 수동 머지)가 장기적으로 깔끔.
- **블로커 알림**: 강태오에게 PR #92 수동 머지 요청 (저녁 카톡/슬랙 — 내일 머지 가능 시점)

### ⏳ Flyway V6 충돌 (별도 이슈, 김지민 외)

- `V6__create_ai_generation_logging.sql` (강상민 추정) ↔ `V6__create_member_auth_providers.sql` (이승욱 추정) 같은 버전 점유
- dev CI 빌드 실패 중
- 김지민 영역엔 영향 X (`@ActiveProfiles("test")`로 격리됨)
- **블로커 알림**: 슬랙 `#bugs` 또는 `#개발-리뷰`에 V6 충돌 알림 (강상민/이승욱 hotfix PR 필요)

---

## 내일 (수요일) Quick Start

### 1️⃣ 첫 명령 한 줄

```
PR #92 머지 확인하고 W2 수요일 시작
```

→ Claude가 자동으로:
1. PR #92 머지 여부 확인 (git fetch + log 또는 GitHub)
2. 머지 됐으면: 이 todo 파일 + W2 워크플로우 갱신
3. 워크플로우 §수요일 (B2 POST /api/v1/notes 자유 노트 생성) 읽고 TodoWrite로 쪼개기
4. 첫 "선택 필요" (DTO 검증 위치) 선택지 제시

### 2️⃣ B2 시작 조건

- Note Entity는 이미 dev에 있음 (PR #51 머지됨) → 시작 가능
- 의존성: B1(PR #92)이 머지 안 돼도 작업 자체는 가능. 단, B2가 B1 위에 쌓이므로 B1 머지 우선

### 3️⃣ B2 작업 미리보기 (워크플로우 §수요일 11항목)

| 분류 | 항목 수 |
| --- | --- |
| 선택 필요 (DTO 검증 위치 ①/②/③) | 1 |
| Use Case / Service (CreateNoteUseCase 본문 + create() 메서드 + Note.Builder 호출) | 3 |
| Controller (@PostMapping + @Valid + 201) | 2 |
| 테스트 3종 | 3 |
| 핵심 개념 확인 (201 vs 200, @Valid 예외) | 2 |
| 마무리 (테스트 + commit + push + PR) | 1 |

**예상 작업 시간**: 화요일보다 가벼움 (~3~4시간)

---

## 막힌 부분 / 다른 사람 의존성

### 1. 강태오 (Lead) — PR #92 수동 머지

- 슬랙/카톡으로 미리 요청 완료 예정
- 내일 오전 안에 머지 처리 가능 여부 확인

### 2. 이승욱 — BaseEntity 협의 확인 + V6 격리

- PR #92 코멘트로 "BaseEntity 협의 OK" 확인 부탁
- (선택) 자기 V6__create_member_auth_providers.sql을 V7로 rename 검토

### 3. 강상민 (또는 이승욱) — V6 충돌 hotfix

- 슬랙 알림 완료 예정
- 김지민 작업에 직접 영향 없음 (격리됨)

---

## 참고 문서

- 이번 작업 리포트: [reports/2026-05-26_GET-notes-본문구현_리포트.md](../reports/2026-05-26_GET-notes-본문구현_리포트.md)
- W2 워크플로우: [workflows/2026-W2_상세-워크플로우.md](../workflows/2026-W2_상세-워크플로우.md)
- PR #92: https://github.com/Tae0072/QT-AI-2nd-Team-Project/pull/92
