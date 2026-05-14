# DevD 이승욱 — W1 첫 작업 가이드

## 담당 (DECISIONS.md §0 — 2026-05-14 재배치)
Bible 도메인 → Flutter → 인증 → 관리자 페이지. **Flutter 빌드 책임자 (시연 6/17)**.
이지윤과 페어, 김지민과 Bible팀 3인 1조.

## 첫 PR 권장 순서

### 1. Journal 도메인 부팅
- 위치: `services/bible-service/journal/`
- 채워 넣을 곳:
  - `journal/api/JournalController.java` — 오늘 DRAFT 멱등 생성/조회 + PATCH 4필드 + DELETE + 이벤트 로그.
  - PATCH 시 `journal.updated` 이벤트 append 로직 (sequence 부여: `SELECT MAX(sequence)+1 FOR UPDATE`).
  - DELETE 시 soft delete + `journal.deleted` append.
- 검증:
  ```bash
  curl -X POST http://localhost:8082/api/v1/journals/today \
       -H "Authorization: Bearer <JWT>" \
       -d '{"qtDate":"2026-05-14","bookCode":"GEN","chapter":1,"verse":1}'
  ```

### 2. ai.session.completed 컨슈머
- 위치: `services/bible-service/kafka/AiSessionCompletedConsumer.java`
- 동작:
  1. envelope.data.userId + qtDate로 오늘 Journal 조회
  2. `ai_session_id`, `ai_summary` 채워서 update (**새 Journal 생성 금지**)
  3. `journal_inbox_keys`에 idempotencyKey 적재. UNIQUE 위반 catch + skip.

### 3. Flutter 빌드 검증 (W1 후반)
- 김지민 작업 결과로 `apps/mobile/`이 부트 가능해지면 본인은 Android emulator + iOS simulator 양쪽 빌드 통과 책임.

## 금지
- 자유 본문 `POST /api/v1/journals` 만들지 않기 — 오늘 QT DRAFT는 `POST /api/v1/journals/today`만
- AI 컨슈머가 새 Journal 생성 — 절대 금지 (오늘 Journal에 attach만)
- `JOURNAL_EVENTS` 수정/삭제 — append only

## 산출물
- `workspaces/DevD_이승욱/reports/W1_journal_bootup.md`
