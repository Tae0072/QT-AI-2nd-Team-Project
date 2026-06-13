# 리포트 — 노트 지우개·그림만 저장·작성 +버튼 복구 (2026-06-13)

## 요약
손그림에 지우개를 추가하고, 본문 없이 그림만으로도 저장/임시저장이 되게 했으며, 기록탭에서 사라졌던 작성 버튼을 둥근 +버튼(챗봇 런처풍)으로 항상 보이게 복구했다.

## 처리 항목
| # | 유형 | 내용 |
|---|------|------|
| 1 | feat | 지우개: 닿은 획 단위 삭제(펜과 상호 배타 토글) |
| 2 | feat | 본문 없이 손그림만 있어도 저장/임시저장 허용(빈 본문이면 '그림 노트' 전송) |
| 3 | change | 기록탭 작성 FAB 항상 표시(QT/설교 포함) + 둥근 액센트 +버튼으로 변경 |

## 변경 파일
- lib: `widgets/note_drawing_layer.dart`, `widgets/qt_note_format_toolbar.dart`, `widgets/note_rich_text_editor.dart`, `screens/note_edit_screen.dart`, `screens/qt_note_editor_screen.dart`, `screens/note_list_screen.dart`
- test: `widgets/note_drawing_and_page_mode_test.dart`(지우개), `screens/note_edit_screen_test.dart`(그림만 저장), `screens/note_list_screen_test.dart`(FAB 유지로 갱신)

## 검증
- `flutter analyze` → No issues.
- `flutter test` → **300개 전부 통과**.

## 비고
- 기록탭 FAB이 QT·설교 칩에서 숨겨지던 기존 설계를 Lead 결정으로 "항상 표시"로 변경(요구사항 변경).
- "AI 챗봇 버튼"이 코드에 없어 둥근 액센트 FAB로 해석. 정식 디자인이 정해지면 색/아이콘 교체만 필요.
