# 2026-06-13 노트 지우개 + 그림만 저장 + 기록탭 작성 버튼 복구

## 배경(사용자 요청)
1. 그리기에 지우개 기능 추가.
2. 그림만 그려도 저장/임시저장 가능하게.
3. 기록탭 작성 버튼이 사라짐 → 마이(하단) 위에 둥근 + 버튼(AI 챗봇 런처풍)으로 복구, 노트 작성 기능.

## 원인
- 기록탭 FAB은 `showFab = !selectionMode && !tabAuthoredCategories.contains(category)`라 QT·설교 칩에서 의도적으로 숨겨졌다 → 사용자가 그 맥락에서 "사라졌다"고 느낌. (Lead 결정으로 항상 표시로 변경)

## 구현
- **지우개**: `NoteDrawingLayer`에 `eraserEnabled` 추가. 지우개 모드에서 터치 지점 반경(18px+선두께/2) 안을 지나는 획을 통째로 지운다. 툴바에 '지우개' 토글(`Icons.auto_fix_normal`). 펜과 상호 배타(`_togglePen`/`_toggleEraser`). 펜·지우개 중엔 핀치 줌 끔.
- **그림만 저장**: `note_edit_screen`·`qt_note_editor_screen` 저장 검증 완화 — 본문이 비어도 손그림(`_strokes`)이 있으면 저장/임시저장 허용. 본문이 비고 그림만 있으면 서버 본문 필수 조건을 위해 안내용 본문 '그림 노트'를 전송.
- **기록탭 작성 +버튼**: `showFab = !selectionMode`(항상 표시). 일반 진입 FAB을 둥근 액센트(`accentDot`+흰색 +) `FloatingActionButton`(CircleBorder)으로 변경 — 챗봇 런처풍. 기존 작성 흐름 유지(기도/회개/감사 칩=직행 알약, 그 외=N-02 카테고리 선택).

## 검증
- `flutter analyze` → No issues.
- `flutter test` → **300개 전부 통과**(신규: 지우개 1, 그림만 저장 1; 갱신: QT/설교 FAB 유지 1).

## Git/PR
- 브랜치 `feature/note-eraser-drawonly-save-and-create-fab` → PR 대상 `dev`. 선행 #596(손그림·원고모드).

## 비고
- "AI 챗봇 버튼"이 코드에 별도로 없어, 둥근 떠 있는 액센트 FAB(런처풍)으로 해석해 구현. 실제 디자인이 따로 있으면 색/아이콘만 교체하면 됨.
