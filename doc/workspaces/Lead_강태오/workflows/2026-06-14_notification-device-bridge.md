# 알림이 기기 배너로 안 뜨는 문제 수정 (2026-06-14)

## 1. 요청
"설정의 알림이 개발자 모드의 '알림 보내기 테스트'처럼 실제로 기기에 알림이 가도록 해 달라."

## 2. 원인
- 서버 알림(`notifications`)을 기기 OS 배너로 띄우는 브리지 `NotificationPoller`는 구현돼 있었으나,
  `notificationPollerProvider`가 **어디서도 watch되지 않아 폴러가 한 번도 시작되지 않았다.**
  → 개발자 모드 테스트 버튼(LocalNotificationService 직접 호출)만 배너가 떴고, 실제 좋아요·댓글·공지
  알림은 서버에만 쌓이고 기기로는 오지 않았다.
- 설정 화면의 "알림 수신" 스위치는 서버 값만 저장하고, 폴러를 제어하거나 OS 알림 권한을 요청하지 않았다.

## 3. 수정
1. `main.dart` — 로그인 상태에서 `notificationPollerProvider`를 watch해 폴러를 시작.
   - 단, **실제 `main()` 구동에서만**(`_launchedFromMain`) 시작 → 위젯 테스트(main 미경유)는
     주기 Timer를 만들지 않아 타이머 누수/플러그인 LateError가 없다.
2. `NotificationPoller._tick` — 설정 `notificationEnabled`가 꺼져 있으면 배너를 띄우지 않음.
   첫 알림 전에 `LocalNotificationService.ensurePermission()`으로 OS 권한을 보장.
3. `LocalNotificationService`
   - `ensurePermission()` 공개 메서드 추가(초기화+권한 요청, idempotent).
   - `_ensureInitialized()`를 try/catch로 감싸 **어떤 환경에서도 throw하지 않게**(테스트 안전).
4. `settings_screen` — 알림 스위치를 켜면 즉시 `ensurePermission()` 호출(개발자 테스트와 동일 경로).

## 4. 동작
- 로그인 + 설정 알림 ON → 폴러가 20초마다 미읽음 알림 조회 → 새 알림을 기기 배너로 표시.
- 설정 알림 OFF → 배너 미표시(폴링은 돌되 표시만 차단).
- 첫 조회는 기준선만 잡아 기존 미읽음 폭주를 막는 기존 정책 유지.

## 5. 검증
- flutter analyze 무이슈, flutter test 전체 GREEN(인증 상태로 QTAIApp을 띄우는 today_qt 테스트 포함).

## 6. 한계/후속
- v1은 FCM 푸시가 없어 **앱이 포그라운드로 떠 있는 동안**만 배너가 보장된다(백그라운드/종료 시 OS가
  폴링을 보장하지 않음). 백그라운드 푸시는 추후 FCM 도입 시 별도 작업.
