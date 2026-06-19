# 2026-06-19 마이페이지 알림을 기기(OS) 로컬 알림으로 안정화

- 작성: 강태오(Lead, AI 보조)
- 대상: 구현 `QT-AI-2nd-Team-Project` (Flutter 앱)
- 브랜치: `feature/mypage-os-notifications` → PR 대상 `dev`
- 관련 F-ID: **F-05**(인앱 알림: 좋아요·댓글·신고 처리 결과·시스템 공지)
- 기준 문서: `07_요구사항_정의서.md` §6.6 F-05, `CLAUDE.md` 코드 규칙

## 1. 배경 / 증상

마이페이지 알림(F-05)은 서버 `notifications`에 쌓이고, `NotificationPoller`가 앱 실행 중 20초마다 미읽음을 조회해 기기 OS 로컬 알림(배너)으로 띄운다. 그러나 안드로이드에서 **"새 알림 일부만 가끔 뜨는"** 증상이 있었다.

원인(기존 폴러):
1. `_lastSeenId`/`_primed`가 **메모리에만** 있어, 앱을 껐다 켜면 매번 "기준선"만 다시 잡고 그 사이 도착한 알림을 건너뜀.
2. 첫 폴에서 기존 미읽음을 모두 기준선 처리(배너 생략) → 이미 쌓인 알림은 영영 안 뜸.
3. 새 알림이 2건 이상이면 **하나의 요약**으로 묶어 띄움 → 개별 알림이 각각 울리지 않음.

## 2. 변경 내용 (Flutter only, 테마/설정 파일 미수정)

- `lib/core/notifications/notification_poller.dart` 재작성
  - "무엇을 띄울지" 판단을 **순수 함수 `decideNotificationsToShow()` + `PollDecision`** 로 분리(테스트 용이).
  - "마지막으로 처리한 알림 id"를 **기기(SharedPreferences `notif_last_seen_id`)에 저장** → 앱 재시작에도 누락 없음.
  - **최초 실행(저장값 없음) 1회만** 기준선을 잡아 과거 알림 폭주 방지, 이후엔 새 알림을 모두 띄움.
  - 새 알림을 **개별 배너**로 띄움(한 번에 5건 초과 시 5건 + "더 N건" 요약).
  - 새 알림이 없는 주기엔 저장값을 덮어쓰지 않음(불필요한 디스크 쓰기 방지).
- `test/core/notifications/notification_poller_logic_test.dart` 신규 — 순수 로직 5케이스.
- `doc/프로젝트 문서/07_요구사항_정의서.md` F-05 §6.6 문구 정합(아래 §4).

권한/플랫폼은 이미 정상이라 그대로 둠:
- 안드로이드: `AndroidManifest.xml`에 `POST_NOTIFICATIONS` 선언 + `build.gradle.kts` 코어 라이브러리 디슈가링 + `LocalNotificationService`가 Android13 권한 요청·채널 생성.
- 권한 요청은 설정 "알림 수신" ON 시점과 폴러 시작 시 `ensurePermission()`로 보장(idempotent).

## 3. 검증

```
flutter analyze lib/core/notifications/notification_poller.dart test/core/notifications/notification_poller_logic_test.dart
  → No issues found!
flutter test test/core/notifications/notification_poller_logic_test.dart
  → All tests passed! (5)
```

## 4. 요구사항(F-05) 정합 — Lead 결정(2026-06-19)

기존 F-05 문구 "실시간 푸시 알림은 제공하지 않고"는 이번 동작과 충돌 소지가 있어 다음과 같이 명확화함(Lead 승인):

- 실시간 **FCM 서버 푸시**는 v1 제공하지 않음(앱 종료 중 수신은 범위 밖).
- 앱 내 로그형 알림을 제공하고, **앱 실행 중에는 기기(OS) 로컬 알림 배너로도 띄움**.
- 앱을 다시 열면 폴링으로 누락분을 배너로 띄움.

> SSoT: 문서 저장소(`2nd-Team-Project`)의 07 요구사항도 동일하게 동기화 필요(후속 docs PR).

## 5. 남은 한계 / 후속

- 앱이 **완전히 종료된 동안**의 실시간 알림은 FCM 없이는 불가(MVP 범위 밖). 필요 시 별도 결정으로 FCM 도입.
- 백엔드(알림 생성)는 무변경 — 폴러는 서버 알림 4종을 종류 구분 없이 모두 브리지하므로 "모든 알림" 요건 충족.
