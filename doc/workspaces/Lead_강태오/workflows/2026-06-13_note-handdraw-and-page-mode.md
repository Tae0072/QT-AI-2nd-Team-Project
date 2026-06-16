# 2026-06-13 노트 손그림(펜) + 원고/일반 페이지 모드

## 배경
사용자 요청 2건:
1. 노트에 손가락·S펜·애플펜슬로 그린 그림을 그대로 저장.
2. 노트 페이지를 공책처럼 줄 그어진 "원고모드"와 "일반모드" 중 선택.

## 결정(사용자 확인)
- 그림 저장: **이 기기에 로컬 저장**(서버 노트 본문은 글자만 저장 가능 → 백엔드 변경 없이 진행).
- 그리기 형태: **글 위 자유 그리기 캔버스**(본문 위 오버레이).
- 페이지 모드: **노트마다 저장**(다시 열면 그 모드).

## 구현
- 신규 `models/note_drawing.dart`: `NotePageMode`(plain/manuscript), `DrawingStroke`(좌표를 0~1 비율로 저장 → 화면 크기/회전에도 위치 유지).
- 신규 `services/note_canvas_store.dart`: 로컬 저장소. 모드=`SharedPreferences`, 손그림 획=앱 문서폴더 JSON 파일. 키 `qt:<qtPassageId>` 또는 `note:<noteId>`. 플러그인 미탑재(테스트) 등 실패에도 앱 흐름을 막지 않도록 best-effort(예외 무시).
- 신규 `widgets/note_drawing_layer.dart`: `RuledLinesPainter`(원고 줄+여백선), `NoteDrawingLayer`(펜 제스처 캡처+`CustomPainter`. 펜 off면 입력 통과·그림은 계속 표시). 외부 의존성 추가 없음.
- `widgets/qt_note_format_toolbar.dart`: 콜백이 주어질 때만 보이는 버튼 4종 추가(원고/일반 전환, 펜, 획 실행취소, 그림 전체 지우기) + 구분선.
- `widgets/note_rich_text_editor.dart`: 본문을 `Stack`으로 감싸 [원고 줄 배경] → [QuillEditor] → [손그림 오버레이] 순서로 쌓음. 펜 모드 중에는 핀치 줌 비활성(그리기 충돌 방지). 펜 색은 텍스트 색상 선택을 공유, 두께 3.
- `providers/note_providers.dart`: `noteCanvasStoreProvider` 추가.
- `screens/note_edit_screen.dart`·`screens/qt_note_editor_screen.dart`: 모드/획 상태 + 로컬 로드·저장 연결. 편집 모드 로딩 스피너는 본문 로딩만 기다리고, 캔버스(모드·손그림)는 `unawaited`로 비동기 로드(스피너 멈춤 방지).

## 검증
- `flutter analyze` → No issues.
- `flutter test` → **298개 전부 통과**(신규 그림/모드 테스트 5개 포함).

## 한계·후속
- 그림은 이 기기에만 저장(다른 기기·재설치 시 사라짐, 서버 '나눔'에 안 올라감). 기기 간 공유는 별도 백엔드 작업 필요.

## Git/PR
- 브랜치 `feature/note-handdraw-and-page-mode` → PR 대상 `dev`. 선행: #595(좌측 세로 툴바).
