# 리포트 — 개발자 모드 알림 보내기(기기 테스트) (2026-06-14)

## 요약
개발자 모드에 "알림 보내기(테스트)" 섹션을 추가해, 내 기기로 좋아요·댓글·돌파·일반 테스트 알림을 즉시 보내 OS 알림 동작을 확인할 수 있게 했다. 외부 푸시(FCM) 없이 로컬 알림으로 구현.

## 추가·변경 파일
- 신규 `core/notifications/local_notification_service.dart`(공용 로컬 알림 서비스)
- 변경 `features/dev/dev_mode_screen.dart`(테스트 버튼 5종)
- 변경 `pubspec.yaml`/`pubspec.lock`(flutter_local_notifications 19.5.0)
- 변경 `android/app/src/main/AndroidManifest.xml`(POST_NOTIFICATIONS), `android/app/build.gradle.kts`(코어 라이브러리 디슈가링)
- 신규 `test/features/dev/dev_mode_notification_test.dart`

## 검증
- `flutter analyze` → No issues.
- `flutter test` → **301개 전부 통과**.
- 실제 기기 알림 표시는 단말에서 확인 필요(처음 1회 알림 권한 허용).

## 비고
- 좋아요/댓글/마일스톤 알림의 서버 자동 발생(이벤트 연동)은 백엔드 작업이 필요한 별도 단계. 본 PR은 기기 테스트용 로컬 발송 + 재사용 서비스(`showLike`/`showComment`/`showMilestone`)까지 제공.
