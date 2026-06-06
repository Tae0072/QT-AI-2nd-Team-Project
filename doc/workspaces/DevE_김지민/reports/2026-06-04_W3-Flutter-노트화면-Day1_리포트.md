# W3 Flutter 노트 화면 Day1 리포트 (2026-06-04)

> 브랜치: `feature/note-flutter-screens` (origin/dev 최신 기반)
> 워크플로우: [`workflows/2026-W3_Flutter-상세-워크플로우.md`](../workflows/2026-W3_Flutter-상세-워크플로우.md)
> 학습노트: [`study-notes/2026-06-04_Flutter-노트화면-개념정리_학습노트.md`](../study-notes/2026-06-04_Flutter-노트화면-개념정리_학습노트.md)
> 진행 방식: 학습 모드(SKILL 🟢 강 모드)

## 한 일 — 노트 수직 슬라이스(N-01~03) + 5탭 배선

백엔드 W3(나눔 쓰기 + 노트 CRUD) 완료 후, 본인 담당 V1 Flutter 화면 착수. 노트 피처가 Flutter에 전무했던(이승욱이 나눔·마이·온보딩만 구축) 빈 영역이라 최우선으로 구현.

### 신규 `note` 피처 (`flutter-app/lib/features/note/`)
- `models/note_models.dart` — `NoteListItem`·`NoteListResponse`(페이징)·`NoteCreateResponse` + 카테고리 라벨/작성가능목록 헬퍼. 04 §4.3.1/4.3.4 필드만 V1 범위로. (상세 모델은 Day2로 미룸)
- `services/note_repository.dart` — `getNotes`/`create`. 기존 `sharing_repository` 패턴(엔벨로프 `data['data']` 언랩) 복제.
- `providers/note_providers.dart` — repository/카테고리필터(StateProvider)/목록(FutureProvider.autoDispose). 필터 watch → 자동 재조회.

### 화면 3개
- **N-01 목록** — 카테고리 6칩(전체+5종) + `GET /notes` + 빈/에러(`whenOrDefault`) + DRAFT '임시저장' 뱃지 + FAB. 사용자 선택: FAB 항상 표시 → N-02.
- **N-02 카테고리 선택** — 기도/회개/감사 3분기 → `category`를 라우트 arguments로 N-03 전달.
- **N-03 작성** — 제목+본문, **저장+임시저장 두 버튼**(사용자 결정), `POST /notes`(status SAVED/DRAFT), 저장 성공 시 목록 invalidate + `popUntil`로 목록 복귀. 이중저장 방지(`_saving`+`AbsorbPointer`).

### 배선
- `app_router.dart` — noteList/categorySelect/edit 라우트 상수 + onGenerateRoute case 3개(edit는 `settings` 전달로 arguments 보존).
- 홈 5탭화.

### 검증
- `flutter analyze` 무경고.
- 에뮬레이터 레벨1 UI 스모크(`--dart-define=DEV_FORCE_HOME=true`): 홈 5탭·노트 탭·FAB→N-02→N-03 폼·저장 실패 안내까지 동작. (실데이터·저장 왕복은 로그인 필요=레벨2, 추후)

### Git
- 커밋 `5a96d52`(노트 N-01~03 + 배선) → **origin/dev 머지 `f2e96ed`**.

## 협업 이벤트 — dev #233/#232 머지 충돌 해결
머지 직전 dev에 큰 변경 유입:
- `#233` 웜 파스텔 테마 + 5탭 탭바 + 디자인 교체, `#232` QT·성경 실데이터 연결.
- dev가 이미 **5탭(오늘/성경/나눔/노트/마이)** + **실제 `BibleBrowserScreen`** 보유, 단 노트 탭은 빈 placeholder.

→ `home_screen.dart` 충돌. **해결**: dev 구조(테마·성경 실화면·탭순서) 채택 + **노트 placeholder 자리에 내 `NoteListScreen` 연결**, 내가 만든 성경 placeholder는 제거. `app_router.dart`는 자동 머지(노트 라우트 충돌 없음).
- 결과 수확: ① 성경 placeholder 고민 소멸(dev에 실화면 존재) ② 노트 화면도 웜파스텔 테마 자동 적용 ③ dev가 비워둔 노트 탭에 정확히 끼워맞춤.

## 회고 (KPT 요약)
- **Keep**: 기존 패턴(나눔 피처) 복제로 빠르게 골격 완성. 화면 단위 `flutter analyze`로 경고 즉시 차단. 학습 모드 — 라우팅/Provider/MaterialPageRoute 개념을 질문하며 이해하고 진행.
- **Problem**: ① 진행현황 문서는 "Flutter 미착수"였으나 실제론 이승욱이 많이 구축해둠 — 초반 현황 파악에 탐색 필요했음. ② 앱 실데이터 확인이 인증(로그인) 때문에 막힘 — Flutter는 JWT만 보내고 dev의 X-Dev-User-Id를 안 써서 레벨1(UI만)로 제한. ③ dev에 5탭+성경이 이미 들어와 home_screen 충돌 — 다행히 일찍 머지해 작게 해결.
- **Try(내일)**: ① 명세(04/07) 대조 점검·수정 ② N-04부터 — 수정 방식 규칙1로 결정 ③ 실데이터 확인이 필요하면 레벨2(docker compose + 카카오 로그인) 1회.

## 내일(6/5 Day2) 예정
N-04 상세/수정/삭제 · N-01 달력 탭 · 외부공유 · 서식 툴바 · 나눔 보완 · **명세 대조 점검** · **학습 문서** · W3 마감(주간회고·push).
