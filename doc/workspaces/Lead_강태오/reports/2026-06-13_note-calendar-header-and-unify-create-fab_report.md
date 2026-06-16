# 리포트 — 캘린더 헤더 우측 정렬·작성 FAB 통일 (2026-06-13)

## 요약
기록 캘린더의 조작 버튼(오늘·week·이전/다음달)을 오른쪽으로 모으고, 카테고리별로 달랐던 작성 버튼을 모두 동일한 둥근 + FAB으로 통일했다.

## 처리 항목
| # | 유형 | 내용 |
|---|------|------|
| 1 | change | 캘린더 헤더: 년월은 왼쪽, 오늘·week·이전·다음 버튼은 오른쪽으로 묶음 |
| 2 | change | 작성 FAB: 기도/회개/감사 알약 → 전체와 동일한 둥근 + FAB(빠른 작성 라우팅 유지) |

## 변경 파일
- lib: `widgets/meditation_calendar.dart`, `screens/note_list_screen.dart`
- test: `screens/note_list_screen_test.dart`(기도 칩 FAB 테스트 갱신)

## 검증
- `flutter analyze` → No issues.
- `flutter test` → **300개 전부 통과**.
