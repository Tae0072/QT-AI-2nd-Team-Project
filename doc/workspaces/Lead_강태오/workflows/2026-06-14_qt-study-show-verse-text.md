# 2026-06-14 QT 해설 — 절 번호 옆에 절 본문 표시

## 요청
QT 해설 페이지의 절별 해설에서 각 절 번호 옆에 그 절의 본문을 함께 보여달라.
(QT의 9:1 = 9장 1절 = 해설의 첫 절. 절 번호만 보이고 본문이 안 보였음)

## 원인
- `today_qt_screen`에서 해설로 넘길 때 `verseLabels`를 `verse.verseNo`("1")만 넘기고 본문은 안 넘겼다.
- `qt_study_content_screen`의 절별 해설 항목은 절 번호 라벨만 표시했다.

## 구현(프론트)
- `today_qt_screen`: 해설 args에
  - `verseLabels`를 `"{chapterNo}:{verseNo}"`(예: 9:1)로 변경(오늘 QT 표기와 일치),
  - `verseTexts`(verseId→한글 본문) 추가.
- `QtStudyContentArgs`: `verseTexts` 필드 추가(기본값 빈 맵 → 기존 호출 호환).
- `_ExplanationItem`: 절 번호(장:절) + 그 절 본문을 한 줄(Row)로 표시.
- 검증용 출처(sourceLabel) 미노출 정책은 그대로 유지.

## 검증
- `flutter analyze` 무이슈, `flutter test` 302개 통과(해설 테스트를 장:절+본문 기준으로 갱신).

## Git/PR
- 브랜치 `feature/qt-study-show-verse-text` → PR 대상 `dev`.
