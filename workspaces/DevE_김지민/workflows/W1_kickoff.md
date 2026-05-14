# DevE 김지민 — W1 첫 작업 가이드

## 담당 (DECISIONS.md §0 — 2026-05-14 재배치)
Bible 도메인 → Flutter → 인증 → 관리자 페이지. 이지윤·이승욱과 Bible팀 3인 1조.
Flutter 모바일 핵심 5 화면 owner.

## 첫 PR 권장 순서

### 1. Flutter 부팅 검증
- 위치: `apps/mobile/`
- 현재 들어 있는 것:
  - `lib/main.dart` — ProviderScope + MaterialApp.router
  - `lib/core/router/app_router.dart` — 5 라우트 (/today /bible /journal /ai /login)
  - `lib/core/network/dio_client.dart` — JWT 인터셉터 자리
  - 각 화면 스켈레톤: today_qt_screen / journal_editor_screen / journal_list_screen / ai_chat_screen / bible_reader_screen / login_screen
- 검증:
  ```bash
  cd apps/mobile
  flutter pub get
  flutter run -d emulator-5554
  ```

### 2. 오늘 QT 화면 완성
- `features/today_qt/today_qt_screen.dart` 의 `todayQtProvider`로 `/api/v1/qt/today` 호출.
- 본문 → 한 줄 요약 → 배경 → 어려운 단어 → AI/노트 버튼 순서 (목업 `docs/mockups/today_qt.html` 참조).

### 3. 묵상 노트 자동 저장
- `features/journal/journal_editor_screen.dart` 의 debounce 600ms 자동 저장 로직 확정.
- **저장 버튼 없음, 글자 수 제한 노출 없음** (MVP 결정).
- PATCH `/api/v1/journals/{id}` 호출. 응답 후 상단 indicator `자동 저장됨`.

### 4. AI SSE 클라이언트
- `features/ai_session/ai_chat_screen.dart` 의 `_send()` 에서 `flutter_client_sse` 로 POST `/ai/sessions/{id}/turns` 호출.
- 이벤트 4종 처리: `token` → 마지막 assistant 메시지에 delta append, `sources` → 칩, `turn_completed` → 입력 활성화, `end`/`error`.
- 강상민과 페어 합의 — 이벤트 키 변경 시 양쪽 동시 PR.

### 5. UI 디자인 참조
- 정적 목업: `docs/mockups/index.html`을 브라우저로 열면 3개 화면(오늘 QT · 묵상 노트 · AI 대화) 미리보기.

## 금지
- `shared_preferences` / `localStorage`에 토큰 저장 — `flutter_secure_storage` 단독
- 직접 YouTube URL 입력 · 가사·음원 저장 — MVP 제외
- 별도 홈 화면을 첫 화면으로 강제 — 첫 화면은 `/today`
- Journal 저장 버튼 / 사용자 노출 글자 수 제한 추가

## 산출물
- `workspaces/DevE_김지민/reports/W1_flutter_bootup.md`
