# Workflow - 2026-06-12 notification-relative-time-refresh

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | M-02 / notification |
| 트리거 | 모바일 알림 목록에서 새 알림 시간이 화면을 계속 열어두면 `방금 전`으로 고정됨 |
| 기준 파일 | `flutter-app/lib/features/mypage/screens/notification_list_screen.dart`, `flutter-app/test/features/mypage/screens/notification_list_screen_test.dart` |

## 작업 목표

알림 목록 화면이 열려 있는 동안 상대 시간 라벨을 주기적으로 다시 계산한다. 새 알림이 `방금 전`으로 표시된 뒤 1분 이상 지나도 화면이 재빌드되지 않으면 사용자는 시간이 멈춘 것처럼 느낀다.

## 범위

- 알림 목록 화면에 UI 갱신용 timer를 추가한다.
- timer는 API 재조회 없이 상대 시간 텍스트만 다시 계산하게 한다.
- 화면 dispose 시 timer를 정리한다.
- `방금 전 -> 1분 전` 전환을 widget test로 검증한다.
- 작업 리포트를 작성한다.

## 제외 범위

- 알림 목록 API 응답 구조 변경
- 서버 알림 생성 시간 정책 변경
- 푸시/실시간 구독 구현
- 전체 앱 공통 상대 시간 컴포넌트화

## 구현 순서

1. `NotificationListScreen`을 stateful consumer widget으로 전환한다.
2. `initState`에서 30초 주기 timer를 시작한다.
3. `dispose`에서 timer를 cancel한다.
4. 기존 `_formatDate`는 현재 시간을 다시 읽어 상대 시간 라벨을 계산하게 유지한다.
5. widget test에서 초기 `방금 전`이 timer tick 이후 `1분 전`으로 바뀌는지 확인한다.
6. Flutter 테스트와 analyze를 실행한다.
7. report를 작성한다.

## 수용 기준

- 알림 목록을 계속 열어둬도 `방금 전`이 1분 후 `1분 전`으로 바뀐다.
- timer tick은 알림 API를 재호출하지 않는다.
- 화면을 벗어나면 timer가 정리된다.
- 기존 알림 내용 표시와 읽음 처리 흐름은 유지된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 Flutter 화면과 해당 widget test에 한정되어 있다.
- timer 생명주기와 화면 렌더링을 함께 확인해야 하므로 직접 순차 처리하는 편이 안전하다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 전체 작업을 직접 수행한다.

## 검증 계획

- `flutter test test/features/mypage/screens/notification_list_screen_test.dart`
- `flutter test test/features/mypage/models/notification_response_test.dart`
- `flutter analyze lib/features/mypage/screens/notification_list_screen.dart test/features/mypage/screens/notification_list_screen_test.dart`
