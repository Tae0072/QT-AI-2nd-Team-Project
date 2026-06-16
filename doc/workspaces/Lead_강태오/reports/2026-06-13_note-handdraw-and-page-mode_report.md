# 리포트 — 노트 손그림 + 원고/일반 페이지 모드 (2026-06-13)

## 요약
노트에 펜(손가락·S펜·애플펜슬) 그림 기능과 공책 줄 "원고모드"를 추가했다. 모든 노트(자유·QT 묵상)에 공통 적용. 그림과 모드는 이 기기에 로컬 저장한다(백엔드 변경 없음).

## 사용자 결정
- 그림 저장: 이 기기 로컬 / 그리기: 글 위 자유 캔버스 / 페이지 모드: 노트마다 저장.

## 추가·변경 파일
| 유형 | 파일 |
|------|------|
| 신규 | `models/note_drawing.dart` (NotePageMode, DrawingStroke) |
| 신규 | `services/note_canvas_store.dart` (로컬 저장: prefs+파일) |
| 신규 | `widgets/note_drawing_layer.dart` (줄 배경+그리기 레이어) |
| 신규 | `test/.../note_drawing_and_page_mode_test.dart` |
| 변경 | `widgets/qt_note_format_toolbar.dart` (펜/모드 버튼) |
| 변경 | `widgets/note_rich_text_editor.dart` (Stack 합성) |
| 변경 | `providers/note_providers.dart` (store provider) |
| 변경 | `screens/note_edit_screen.dart`, `screens/qt_note_editor_screen.dart` (영속화) |

## 동작
- 좌측 세로 툴바 끝에 **원고/일반 전환·펜·획 실행취소·그림 전체 지우기** 버튼.
- 펜을 켜면 본문 위에 자유롭게 그리고, 끄면 글 입력으로 돌아간다(그림은 계속 보임).
- 손그림 좌표는 0~1 비율로 저장 → 화면 크기·회전이 달라도 같은 위치에 복원.
- 원고모드는 공책처럼 가로 줄 + 왼쪽 여백선을 본문 뒤에 그린다.
- 모드·그림은 노트(QT는 qtPassageId)별로 기기에 저장 → 다시 열면 그대로.

## 검증
- `flutter analyze` → No issues.
- `flutter test` → **298개 전부 통과**(신규 5개: 모델 왕복, 모드 enum, 원고 줄 표시, 펜 획 추가, 기존 획 표시).

## 한계
- 로컬 저장이라 기기 간 공유·재설치 후 유지·서버 '나눔' 반영은 안 됨(원하면 백엔드 작업 별도 필요).
