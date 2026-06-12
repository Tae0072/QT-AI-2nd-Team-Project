# Report - 2026-06-12 알림 timestamp 표시 보강

## 요약

에뮬레이터 알림 목록에서 알림 내용은 표시되지만 시간은 `방금 전` 위주로 보여 실제 시각을 확인하기 어려웠다. 에뮬레이터 timezone은 GMT이고 서버/로컬 운영 기준은 KST라 timezone 없는 `LocalDateTime` 응답을 기기 local time으로 해석하면 미래 시각이 될 수 있다.

## 원인

- 서버 알림 응답의 `createdAt`은 offset 없는 문자열이다.
- Flutter `DateTime.parse`는 offset 없는 문자열을 기기 local timezone으로 해석한다.
- 에뮬레이터 timezone이 GMT이면 KST 서버 시간을 미래 시각처럼 읽을 수 있다.
- 기존 UI는 상대 시간만 표시해 `방금 전`이 반복될 때 실제 시각을 확인할 수 없었다.

## 변경 내용

- `flutter-app/lib/features/mypage/models/notification_response.dart`
  - offset 없는 서버 시간을 KST 기준으로 해석하도록 보정했다.
  - `Z` 또는 `+09:00` 같은 offset이 포함된 응답은 기존 `DateTime.parse` 결과를 사용한다.
- `flutter-app/lib/features/mypage/screens/notification_list_screen.dart`
  - 시간 라벨을 `상대 시간 · HH:mm` 형식으로 변경했다.
  - 미래 timestamp는 `방금 전`으로 숨기지 않고 `M/D HH:mm` 절대 시각으로 표시한다.
- `flutter-app/test/features/mypage/models/notification_response_test.dart`
  - timezone 없는 서버 시간을 KST 기준으로 파싱하는 테스트를 추가했다.
- `flutter-app/test/features/mypage/screens/notification_list_screen_test.dart`
  - `방금 전 · HH:mm` 갱신과 미래 timestamp 절대 표시 테스트를 보강했다.

## 확인 결과

- 알림 row에서 실제 시간이 함께 보인다.
- GMT 에뮬레이터에서도 KST 서버 timestamp를 실제 instant로 해석한다.
- 미래 timestamp가 들어와도 `방금 전`으로 고정되지 않는다.

## 검증 명령

```powershell
flutter test test/features/mypage/models/notification_response_test.dart
```

결과: 성공

```powershell
flutter test test/features/mypage/screens/notification_list_screen_test.dart
```

결과: 성공

```powershell
flutter analyze lib/features/mypage/models/notification_response.dart lib/features/mypage/screens/notification_list_screen.dart test/features/mypage/models/notification_response_test.dart test/features/mypage/screens/notification_list_screen_test.dart
```

결과: 성공, `No issues found`

## 후속 권장

서버 API 계약은 장기적으로 `LocalDateTime` 대신 `OffsetDateTime` 또는 UTC instant 문자열을 내려주는 편이 더 안전하다. 이번 수정은 현재 계약을 유지하면서 모바일 표시 오류를 막는 클라이언트 보정이다.
