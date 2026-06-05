# 김지민 W3 Flutter 상세 워크플로우 (목·금: 2026-06-04 ~ 06-05)

> **상위 워크플로우**: [`김지민_W1-W5_워크플로우.md`](../김지민_W1-W5_워크플로우.md) §W3·§W4
> **목표**: 본인 담당 V1 Flutter 화면 전체 구현 — 노트(N-01~04) 중심 + 달력·외부공유·서식툴바 + 나눔(S-01~03) 검토·보완
> **완료 기준**: 노트 작성→목록→상세→수정→삭제 흐름 동작 + 달력/외부공유 연결 + flutter analyze 무경고
> **구현 방식**: 학습 모드(SKILL 🟢 강 모드 — 선택지 먼저, 블록마다 이유, 개념 확인, 속도는 본인 조율)
> **브랜치**: `feature/note-flutter-screens` (origin/dev 최신 기반)
> **사용자 결정**: V1 화면 전체 포함(이연 없음) · 학습 모드 유지 · 나눔은 검토 후 필요시 보완

> ⚠️ **현실성 메모** — 학습 모드 + 전체 범위 = 2일치고 공격적. **데모 필수 핵심(노트 CRUD)을 Day1에 먼저 안착**시키고, 달력·외부공유·서식툴바·나눔보완을 Day2에 가치순으로 쌓는다. 밀리면 뒤(서식툴바·나눔보완)부터 압축.

---

## 탐색으로 확인된 현실 (착수 전제)

- **Flutter 앱은 이미 존재**(`flutter-app/`, `qtai_app`) — 아키텍처·공통기반 견고(Riverpod 2.6 + Dio 5.7 + 인터셉터 + `common_widgets`). **전부 이승욱 구축**.
- **`note/` 피처 디렉토리 자체가 없음** → 노트 화면(N-01~04)이 김지민의 진짜 빈 영역. **최우선.**
- **S-01/S-02/S-03(나눔 피드·상세·작성)은 이미 이승욱 구현**(#190, #229). `sharing_repository.dart`에 댓글·신고가 `// TODO`로 남음 → "보완" 대상.
- **백엔드 의존성 전부 가용**: 노트 CRUD 6개·`GET /note-categories`·`GET /me/meditation-calendar`(이지윤 #138)·나눔 댓글/신고 모두 완료.
- 디자인(Figma) 미확정 → **기존 이승욱 화면 스타일을 사실상 기준**으로 따른다(별도 디자인 대기 안 함).

---

## 재사용할 기존 자산 (새로 만들지 말 것)

| 자산 | 경로 | 용도 |
|---|---|---|
| 엔벨로프 언랩 Repository | `flutter-app/lib/features/sharing/services/sharing_repository.dart` | `response.data['data']` 언랩 복제 |
| Provider 패턴 | `flutter-app/lib/features/sharing/providers/sharing_providers.dart` | repositoryProvider + filterProvider + FutureProvider.autoDispose |
| 화면 패턴(필터칩+목록+빈/에러) | `flutter-app/lib/features/sharing/screens/sharing_feed_screen.dart` | N-01 목록 골격 |
| 공통 상태 위젯 | `flutter-app/lib/core/widgets/common_widgets.dart` | `whenOrDefault`/`LoadingView`/`ErrorView`/`EmptyView` |
| Dio provider | `flutter-app/lib/core/network/api_client.dart` (`dioProvider`) | Repository 주입 |
| 라우팅 | `flutter-app/lib/routes/app_router.dart` | 노트 라우트 추가 |
| 홈 탭 | `flutter-app/lib/features/home/screens/home_screen.dart` | 노트 진입점 |

백엔드 계약(필드명) SSoT: `doc/standards/04_API_명세서.md` §4.3(노트)·§4.6.2(달력). 요청 DTO 실측(`CreateNoteRequest`): category/qtPassageId/title/body/4섹션/verseIds/status/visibility.

---

## 운영 규칙 (학습 모드)

- 규칙1 — 파일 착수 전 선택지 제시 후 진행(코드 먼저 X)
- 규칙2 — 코드 블록마다 "왜 이렇게 짰는지" 한 줄
- 규칙4 — 화면 완료 시 "핵심 개념 한 줄" 확인
- 규칙5 — 단계 끝 "다음 갈까?" → "응" 후 진행
- 검증 — 화면 단위로 `cd flutter-app && flutter analyze` 무경고 유지

---

## Day 1 (목 6/4) — 노트 수직 슬라이스 핵심 [데모 필수] ✅ 완료

> 목표: **노트 작성 → 목록에서 확인** 흐름 동작 = 데모 안전선.

### 1. note 피처 골격 `flutter-app/lib/features/note/`
- [x] `models/note_models.dart` — `NoteListItem`·`NoteListResponse`(페이징)·`NoteCreateResponse` + 카테고리 라벨 헬퍼 (04 §4.3.1/4.3.4). ※ `NoteDetail`/`getDetail` 등은 Day2(N-04)로 미룸(안 쓸 코드 미리 안 만듦)
- [x] `services/note_repository.dart` — `getNotes/create` (sharing 복제, 엔벨로프 언랩)
- [x] `providers/note_providers.dart` — `noteRepositoryProvider`·`noteCategoryFilterProvider`·`notesProvider(autoDispose)`

### 2. N-01 노트 목록 `screens/note_list_screen.dart`
- [x] 카테고리 탭 6칩(전체 + 5종) — `_CategoryChip`
- [x] `GET /api/v1/notes` 연동 + 빈/에러(`whenOrDefault`) + DRAFT '임시저장' 뱃지·날짜
- [x] 신규 작성 FAB → N-02 (사용자 선택 ①: 항상 표시)
- [ ] 달력 토글 자리 — Day2로

### 3. N-02 카테고리 선택 `screens/note_category_select_screen.dart`
- [x] 기도/회개/감사 3분기 → N-03에 `category` arguments 전달

### 4. N-03 개인 노트 작성 `screens/note_edit_screen.dart`
- [x] 제목+본문 1섹션, **저장 + 임시저장 둘 다**(사용자 결정) / 자동저장 없음
- [x] `POST /notes`(category·qtPassageId=null·status·visibility) → 목록 invalidate 후 popUntil 목록

### 5. 배선 + 진입점
- [x] `app_router.dart`에 noteList/categorySelect/edit 라우트 + case 추가
- [x] 홈 진입점 — **5탭(QT/노트/성경/나눔/마이)** (사용자 결정). ※ dev #233 머지로 최종 **오늘/성경/나눔/노트/마이** 순 채택, 노트 탭에 NoteListScreen 연결

### Day1 검증
- [x] `flutter analyze` 무경고
- [x] 에뮬레이터 레벨1 UI 스모크(DEV_FORCE_HOME) — 홈 5탭·노트 탭·FAB→N-02→N-03 폼·저장 실패 안내까지 동작 확인(실데이터·저장은 로그인 필요=레벨2, 추후)

### Day1 커밋·머지 (완료)
- [x] Day1 작업 커밋(5a96d52) → **origin/dev 머지**(f2e96ed)
- [x] 충돌 해결: dev #233(웜 파스텔 테마 + 5탭 + 실제 `BibleBrowserScreen`) 채택 + **노트 placeholder 자리에 내 NoteListScreen 연결**, 내 성경 placeholder 제거
- [x] 수확: 성경 화면이 dev에 이미 생겨 placeholder 불필요해짐 / 노트 화면도 웜파스텔 테마 자동 적용

---

## Day 2 (금 6/5) — 상세/수정/삭제 + 달력 + 외부공유 + 서식툴바 + 나눔보완 + W3 마감

> 🔖 내일 추가 요청(사용자):
> - **A. 명세 대조 점검·수정** — 04 API/07 요구사항과 다르게 구현된 부분 체크 후 수정 (N-01~03 우선)
> - **B. 학습 문서** — 그날 구현한 플러터 개념/위젯/속성 정리 (오늘처럼 study-notes에)
> 💡 N-04 첫 선택(보류): 수정 방식 ① N-03 재사용(수정 모드) vs ② 상세 내 편집 — 내일 규칙1로 결정

### 6. N-04 상세/수정/삭제 `screens/note_detail_screen.dart` [데모 필수]
- [ ] `GET /notes/{id}` 상세 / 수정(N-03 재사용·`PATCH`) / 삭제(`DELETE` → 목록·달력 invalidate)
- [ ] note_verses는 있으면 표시만(작성 v1 생략)

### 7. N-01 달력 탭 `widgets/meditation_calendar.dart`
- [ ] 목록↔달력 토글 + 월 캘린더(이전/다음 달)
- [ ] `GET /me/meditation-calendar?month=YYYY-MM` 연동(백엔드 #138 완성, 호출만)
- [ ] 멀티 카테고리 인디케이터 + 스트릭(🔥 Flutter 계산) + 폴백(달력 영역만 재시도)
- [ ] 막히면 Mock JSON 우회 후 실연동

### 8. 외부 공유 (N-04 진입)
- [ ] `pubspec.yaml`에 `share_plus` 추가
- [ ] 텍스트 공유(`Share.share`)
- [ ] 카드 이미지(`RepaintBoundary`+`toImage`→임시파일→`shareXFiles`)
- [ ] 공유 바텀시트(텍스트/카드 분기)

### 9. 서식 편집 툴바 `widgets/note_format_toolbar.dart` (N-03·N-04)
- [ ] 굵게·색상·크기·들여쓰기·하이라이트 — ⚠️ v1 간소화(범위는 규칙1로 결정)

### 10. 나눔 검토 후 보완 (필요시)
- [ ] `sharing_repository.dart` 댓글(`POST/GET /comments`)·신고(`POST /reports`) TODO 연결
- [ ] S-02에 댓글 리스트/입력·신고 바텀시트 배선 (백엔드 완료됨)

### 11. W3 마감
- [ ] 이 워크플로우 체크 + `todos/진행중-todo.md` + `reports/2026-06-05_*.md` + `reports/2026-W3_주간회고.md`
- [ ] commit + push (PR은 단위 완료 시)

---

## 검증 방법 (E2E)

1. 정적: `cd flutter-app && flutter analyze`(무경고), 가능 시 `flutter test`
2. 구동: qtai-server `spring.profiles.active=dev` + 에뮬레이터, 또는 dev-console로 시드 후 조회
3. 흐름: 노트(카테고리→작성→목록→상세→수정→삭제) / 달력(월이동→인디케이터→탭이동) / 외부공유 / 나눔 댓글
4. Day1 종료 시 "작성→목록"이 반드시 동작(데모 안전선)

---

## 진행률 (자동 계산)

> 매일 마무리 시 갱신. `완료 / 총 체크박스`.

- Step 0(브랜치·문서): ✅ 완료 (2026-06-04)
- Day1(노트 N-01~03 + 5탭 + analyze + 앱검증 + 커밋 + dev머지): ✅ 완료 (2026-06-04)
- Day2(N-04·달력·외부공유·서식툴바·나눔보완·명세대조·학습문서·W3마감): ✅ 완료 (2026-06-05) — 전 항목 구현 + `flutter analyze` 전체 무경고 + 커밋. 명세대조에서 설교노트 절 손실 버그 1건 차단. 패키지 3종(table_calendar/share_plus/path_provider) 추가→회의 안건. push·PR은 김지민 직접.

---

## 참고 문서

- 요구사항: `doc/standards/07_요구사항_정의서.md` §F-16(자유 노트)·§6.4.1(@멘션, v2)·§F-10(나눔)
- API 계약: `doc/standards/04_API_명세서.md` §4.3(노트)·§4.4(나눔)·§4.6.2(달력)
- 화면: `06_화면_기능_정의서.md` N-01~04 / S-01~03
- 백엔드 워크플로우: `workflows/2026-W3_상세-워크플로우.md`
- 학습 규칙: `skills/SKILL.md`
- 구현 계획(원본): `C:\Users\G\.claude\plans\misty-tumbling-sphinx.md`
