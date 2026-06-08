# 2026-06-08 오늘 QT 당겨서-새로고침(onRefresh) 수정

## 목표
오늘 QT 화면의 `RefreshIndicator`가 빈 콜백(`() async {}`)이라 당겨서 새로고침이 아무 동작도 하지 않던 결함을 고친다.

## 배경
- 처음에는 `refactor/flutter-bible-widgets`로 성경 브라우저 위젯 분리 + onRefresh 수정을 함께 진행했으나, 작업 중 dev #250(QT 노트/스터디 플로우 연동)이 `bible_browser_screen`을 휠 피커 UI로 전면 재작성 → 위젯 분리분은 대상 화면이 사라져 폐기.
- 여전히 유효한 onRefresh 수정(dev에 버그 잔존)만 분리해 본 브랜치로 재구성.

## 작업 내용
1. `today_qt_screen`의 `_TodayQtContent`(StatefulWidget)에 `onRefresh` 콜백 필드 주입
2. 상위 `TodayQtScreen`(ConsumerWidget)에서 `ref.refresh(todayQtPassageProvider.future)` 전달 — 새로고침 완료까지 대기
3. `RefreshIndicator.onRefresh`를 `widget.onRefresh`로 연결 (AppBar 새로고침 버튼과 동작 일치)

## 범위
- 브랜치: `fix/flutter-today-qt-refresh` (base: dev@99439a4)
- 변경: 1파일 (`today_qt_screen.dart`)
- 관련: 오늘 QT(today)

## 검증
- `flutter analyze` — No issues found
- `flutter test test/features/bible/screens/today_qt_screen_test.dart` — 3건 통과

## 미해결
- PR 머지 대기

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
