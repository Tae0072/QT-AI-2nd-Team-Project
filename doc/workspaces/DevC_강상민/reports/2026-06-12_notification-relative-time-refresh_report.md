# Report - 2026-06-12 알림 상대 시간 갱신

## 요약

알림 목록에서 새 알림 시간이 `방금 전`으로 표시된 뒤 화면을 계속 열어두면 시간이 갱신되지 않았다. 알림 목록 화면은 최초 렌더링 시 `DateTime.now()`로 상대 시간을 계산하지만, 이후 화면을 다시 그릴 트리거가 없어 `방금 전`이 고정됐다.

## 원인

- `NotificationListScreen`은 `ConsumerWidget`으로 한 번 렌더링된 뒤 상대 시간만 갱신하는 timer가 없었다.
- `DateTime.now()`는 build 시점에만 다시 계산된다.
- 새 알림이 1분을 넘어도 provider invalidate, 새로고침, 화면 재진입이 없으면 `방금 전` 텍스트가 유지됐다.

## 변경 내용

- `flutter-app/lib/features/mypage/screens/notification_list_screen.dart`
  - `NotificationListScreen`을 `ConsumerStatefulWidget`으로 전환했다.
  - 화면이 열려 있는 동안 30초마다 `setState`로 상대 시간 라벨만 다시 계산한다.
  - 화면 dispose 시 timer를 cancel한다.
  - 테스트에서 결정적으로 검증할 수 있게 현재 시각 provider callback을 주입 가능하게 했다.
- `flutter-app/test/features/mypage/screens/notification_list_screen_test.dart`
  - `방금 전`으로 시작한 알림이 timer tick 이후 `1분 전`으로 바뀌는 widget test를 추가했다.

## 확인 결과

- 알림 목록을 계속 열어둔 상태에서도 상대 시간 텍스트가 갱신된다.
- timer tick은 API를 다시 호출하지 않고 화면 텍스트만 다시 계산한다.
- 기존 알림 제목/본문 표시 흐름은 유지된다.

## 검증 명령

```powershell
flutter test test/features/mypage/screens/notification_list_screen_test.dart
```

결과: 성공

```powershell
flutter test test/features/mypage/models/notification_response_test.dart
```

결과: 성공

```powershell
flutter analyze lib/features/mypage/screens/notification_list_screen.dart test/features/mypage/screens/notification_list_screen_test.dart
```

결과: 성공, `No issues found`
