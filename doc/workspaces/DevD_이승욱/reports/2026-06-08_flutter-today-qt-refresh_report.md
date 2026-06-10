# 2026-06-08 오늘 QT onRefresh 수정 — 결과 보고

## 요약
오늘 QT 화면의 당겨서-새로고침이 빈 콜백으로 무동작이던 버그를 provider refresh로 연결해 수정. 본 PR은 코드 1파일을 포함한다.

## 산출물

| 파일 | 설명 |
|------|------|
| `today_qt_screen.dart` | `RefreshIndicator`의 no-op `onRefresh` → `ref.refresh(todayQtPassageProvider.future)` 콜백 주입 |

## 변경 성격
- 단일 결함 수정. `_TodayQtContent`(StatefulWidget)에 `onRefresh` 콜백 필드 추가, 상위 `ConsumerWidget`의 `ref`로 새로고침 트리거(완료까지 대기). AppBar 새로고침 버튼과 동일 동작
- 당초 동반하려던 `bible_browser` 위젯 분리는 dev #250 전면 재작성으로 폐기(별도 PR 불요)

## 검증
- `flutter analyze` — No issues found
- `flutter test .../today_qt_screen_test.dart` — 3건 통과

## 미해결
- PR 머지 대기
