# Report - 2026-06-12 알림 목록 내용 표시 계약 수정

## 요약

모바일 알림 목록에서 알림 row와 시간은 보이지만 제목/본문이 비어 보이는 문제가 있었다. 서버 `NotificationResponse`는 `title`, `body`, `read`를 내려주는데 Flutter 모델은 `message`만 읽고 있어 실제 알림 내용이 빈 문자열로 렌더링됐다.

## 원인

- 서버 계약: `id`, `type`, `title`, `body`, `linkType`, `linkId`, `read`, `readAt`, `createdAt`
- Flutter 기존 모델: `id`, `type`, `message`, `readAt`, `createdAt`
- 화면은 `item.message`만 title로 표시했다.
- 따라서 서버가 정상적으로 `title/body`를 보내도 앱에서는 내용을 읽지 못했다.

## 변경 내용

- `flutter-app/lib/features/mypage/models/notification_response.dart`
  - `NotificationItem`에 `title`, `body` 필드를 추가했다.
  - 읽음 여부는 우선 `read` boolean을 사용하고, 구형 응답 호환을 위해 `readAt` fallback을 유지했다.
  - 구형 `message` 응답은 `title` fallback으로 처리했다.
- `flutter-app/lib/features/mypage/screens/notification_list_screen.dart`
  - 알림 row 제목은 `title`을 표시한다.
  - `body`가 있으면 보조 텍스트로 최대 2줄 표시하고, 그 아래에 시간을 표시한다.
- `flutter-app/test/features/mypage/models/notification_response_test.dart`
  - 서버 계약 `title/body/read` 파싱 테스트를 추가했다.
  - `readAt` fallback과 `message` fallback을 검증했다.

## 확인 결과

- 알림 목록 모델은 서버 계약의 `title/body/read`를 정상 파싱한다.
- 기존 `message`만 있는 응답도 빈 알림으로 보이지 않는다.
- 화면은 제목과 본문을 분리해서 표시한다.

## 검증 명령

```powershell
flutter test test/features/mypage/models/notification_response_test.dart
```

결과: 성공

```powershell
flutter test test/features/mypage/services/mypage_repository_test.dart
```

결과: 성공

```powershell
flutter analyze lib/features/mypage/models/notification_response.dart lib/features/mypage/screens/notification_list_screen.dart test/features/mypage/models/notification_response_test.dart
```

결과: 성공, `No issues found`

## 참고

처음에 모델 테스트와 repository 테스트를 병렬 실행했을 때 Flutter tool이 `build/unit_test_assets/NativeAssetsManifest.json` 생성 경합으로 크래시했다. 코드 실패가 아니라 동일 Flutter 프로젝트에서 `flutter test`를 동시에 실행한 실행 방식 문제였고, 단독 재실행 시 통과했다.
