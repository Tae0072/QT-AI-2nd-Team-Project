# 2026-06-14 기록 목록 타임라인 구분바

## 요청
기록 페이지 목록을 (예시 사진처럼) 좌측에 점+세로 연결선이 있는 타임라인 형태로 구분.

## 구현(프론트)
- `note_list_screen`의 목록을 `ListView.separated` → `ListView.builder`로 바꾸고, 각 항목을 `IntrinsicHeight + Row[타임라인 레일, 카드]`로 감쌌다.
- 신규 `_NoteTimelineRail` + `_TimelineRailPainter`(CustomPaint): 항목마다 점을 찍고, 점들을 세로선으로 연결(첫 항목은 위쪽 선 생략, 마지막 항목은 아래쪽 선 생략). 색은 `appColors.hairline`(선)·`text2`(점).
- 항목 간격(10px)을 카드 하단 패딩으로 옮겨 세로선이 끊기지 않고 이어지게 함.

## 검증
- `flutter analyze` 무이슈, `flutter test` 302개 통과(기존 note_list 테스트 12개 포함 — NoteCard 탭/선택 동작 유지).

## Git/PR
- 브랜치 `feature/note-list-timeline-rail` → PR 대상 `dev`.
