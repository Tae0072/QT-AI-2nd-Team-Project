# 리포트 — 노트 서식 툴바 좌측 세로 패널 + 수정 저장 실패 수정 (2026-06-13)

## 요약
태블릿에서 보고된 노트 편집 버그 2건과 UI 요청 2건을 한 PR(#595)로 묶어 처리했다. 모든 노트(자유 노트·QT 묵상 노트)에 공통 적용.

## 처리한 항목
| # | 유형 | 내용 |
|---|------|------|
| 1 | fix | 노트/QT묵상 노트 수정 저장 실패(서버 `INVALID_INPUT` C0002) — PATCH 본문에 필수값 `category`(+`qtPassageId`) 재전송 |
| 2 | fix | 저장/임시저장 버튼이 태블릿 네비게이션 바에 가림 — `SafeArea(bottom)`로 위로 올림 |
| 3 | feat | 서식 툴바를 본문 왼쪽 세로 패널로 이동(모든 노트 기본) |
| 4 | change | 기록 목록 나눔 배지 '나눔' → '나눔공개' |

## 근본 원인 (항목 1)
서버 `PATCH /notes/{id}`의 `normalize()`는 `category == null`이면 `INVALID_INPUT`을 던진다(MEDITATION은 `qtPassageId` 필수). 프런트 `update()` PATCH 본문에 `category`가 빠져 있어 모든 수정 저장이 실패했다. 편집 진입 시 불러온 노트의 `category`/`qtPassageId`를 보관해 저장 때 그대로 다시 전송하도록 고쳤다.

## 변경 파일 (10)
- lib: `note_repository.dart`, `note_edit_screen.dart`, `qt_note_editor_screen.dart`, `note_card.dart`, `note_rich_text_editor.dart`, `qt_note_format_toolbar.dart`
- test: `note_repository_test.dart`, `note_edit_screen_test.dart`, `qt_note_editor_screen_test.dart`, `note_card_test.dart`

## 검증
- `flutter analyze lib/features/note` → No issues found.
- `flutter test test/features/note` → **78개 전부 통과**.
- `flutter test .../bible/screens/today_qt_screen_test.dart` → 통과.

## 설계 메모
- 세로 툴바는 `SingleChildScrollView + Column`(즉시 빌드)으로 구현 — `ListView`는 화면 밖 버튼을 지연 생성해 테스트 find/탭이 불안정했다.
- 가로(`top`/`bottom`) 배치 경로는 보존해 회귀 위험을 줄였다.

## 후속
- 실기기(태블릿) 확인 후 dev 머지.
- 백엔드 `admin-server` 동기화 대상 아님(프런트 전용 변경).
