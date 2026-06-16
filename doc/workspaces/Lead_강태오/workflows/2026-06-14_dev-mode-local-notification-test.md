# 2026-06-14 개발자 모드 알림 보내기(기기 테스트)

## 배경(사용자 요청)
- 개발자 모드에서 내 기기로 알림을 보내 좋아요·댓글·돌파 알림이 실제로 오는지 테스트할 수 있게 해달라.

## 구현
- 의존성 추가: `flutter_local_notifications: ^19.0.0`(해석 19.5.0). 외부 푸시(FCM) 없이 기기 로컬 OS 알림.
- 신규 `core/notifications/local_notification_service.dart`: 싱글턴. 첫 호출 시 1회 초기화(Android13+ 권한 요청·채널 생성, iOS 권한 요청). `show()` 즉시 알림 + 프리셋(`showLike`/`showComment`/`showMilestone`). 실패는 삼켜 앱 흐름 보호.
- `features/dev/dev_mode_screen.dart`: "알림 보내기(테스트)" 섹션 + 버튼 5종(좋아요·댓글·100 돌파·1000 돌파·테스트 알림). 누르면 기기로 알림을 보내고 결과 스낵바 안내.
- Android 설정: `POST_NOTIFICATIONS` 권한(매니페스트), `build.gradle.kts`에 코어 라이브러리 디슈가링(+`desugar_jdk_libs:2.1.4`).

## 검증
- `flutter analyze` → No issues.
- `flutter test` → **301개 통과**(개발자 모드 알림 버튼 렌더 테스트 1 추가).
- 기기 실제 알림 표시는 사용자 단말에서 확인 필요(Android 13+/iOS 권한 허용 후).

## 비고
- 좋아요/댓글/마일스톤(100·1000) 알림의 "서버 발생→자동 알림"은 백엔드 이벤트 작업이 필요한 별도 단계. 이번 PR은 그 알림들을 기기에서 즉시 테스트할 수 있는 로컬 발송 + 재사용 가능한 서비스까지 제공.

## Git/PR
- 브랜치 `feature/dev-mode-local-notification-test` → PR 대상 `dev`.
