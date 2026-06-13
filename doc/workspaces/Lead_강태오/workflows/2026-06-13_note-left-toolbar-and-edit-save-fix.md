# 2026-06-13 노트 서식 툴바 좌측 세로 패널 + 수정 저장 실패 수정

## 배경
- 태블릿에서 노트(기도 등)·QT 묵상 노트를 수정 후 저장하면 "저장할 수 없다"는 오류가 떴다.
- 저장/임시저장 버튼이 태블릿 시스템 네비게이션 바에 묻혀 누르기 어려웠다.
- 기록 목록의 나눔 배지가 단순히 '나눔'이라 의미가 모호했다.
- 서식 도구(툴바)가 본문 아래/위 가로 바라 화면을 위아래로 차지했고, 사용자가 본문 왼쪽 세로 패널로 옮기길 원했다.

## 변경
- **fix: 수정 저장 실패(C0002 INVALID_INPUT)** — 서버 `PATCH /notes/{id}`는 `category`(MEDITATION이면 `qtPassageId`)를 필수로 요구하는데 프런트 PATCH 본문에서 빠져 있었다.
  - `note_repository.dart` `update()`에 `required String category`, `int? qtPassageId` 추가, PATCH 본문에 재전송.
  - `note_edit_screen.dart`: 편집 진입 시 불러온 `detail.category`/`detail.qtPassageId`를 보관해 저장 시 그대로 다시 전송.
  - `qt_note_editor_screen.dart`: 초안 수정 저장 시 `category: 'MEDITATION'`, `qtPassageId` 전송.
- **fix: 저장 버튼이 네비게이션 바에 가림** — `note_edit_screen.dart` 본문을 `SafeArea(top: false)`로 감싸 버튼을 시스템 내비게이션 위로 올림.
- **feat: 서식 툴바 좌측 세로 패널** — 모든 노트의 서식 도구를 본문 왼쪽 세로 패널로 이동.
  - `qt_note_format_toolbar.dart`: `Axis axis` 파라미터 추가. 세로 모드는 폭 52의 `SingleChildScrollView + Column`(모든 버튼 즉시 빌드, 길면 스크롤). 서체는 글꼴 아이콘, 글씨 크기는 아이콘+숫자로 좁게 표시.
  - `note_rich_text_editor.dart`: `NoteRichTextToolbarPlacement.left` 추가하고 기본값으로 지정. left일 때 `Row[세로툴바][본문]` 레이아웃.
  - `qt_note_editor_screen.dart`: 기존 `top` → `left`.
- **change: 나눔 배지 라벨** — `note_card.dart` '나눔' → '나눔공개'.

## 안전 확인
- 세로 툴바는 `ListView`(지연 빌드) 대신 `SingleChildScrollView + Column`을 써, 화면 밖 버튼도 위젯 트리에 존재하도록 함(테스트 find/접근 안정).
- `top`/`bottom` 가로 배치 코드 경로는 그대로 유지(회귀 방지).

## 검증
- `flutter analyze lib/features/note` → No issues found.
- `flutter test test/features/note` → 78개 전부 통과.
- `flutter test test/features/bible/screens/today_qt_screen_test.dart` → 통과.

## Git/PR
- 브랜치 `feature/note-left-toolbar-and-edit-save-fix` → PR **#595** 대상 `dev`.
- 관련: #593(노트 SHARED/PRIVATE 백엔드), #594(에디터 flutter_quill 교체)의 후속 UI 수정.
