# Workflow - 2026-06-12 notification-timestamp-visibility

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | M-02 / notification |
| 트리거 | 에뮬레이터 알림 목록에서 내용은 보이지만 시간이 `방금 전` 위주로만 보여 실제 시간이 확인되지 않음 |
| 기준 파일 | `flutter-app/lib/features/mypage/models/notification_response.dart`, `flutter-app/lib/features/mypage/screens/notification_list_screen.dart` |

## 작업 목표

알림 목록에서 서버 timestamp를 기기 timezone에 따라 잘못 해석하지 않게 하고, 사용자에게 상대 시간과 실제 시각을 함께 보여준다.

## 범위

- timezone 없는 서버 `LocalDateTime` 응답을 KST 기준 시각으로 파싱한다.
- 상대 시간 라벨에 절대 시각을 함께 표시한다.
- 미래 timestamp가 들어오면 `방금 전`으로 숨기지 않고 절대 시각을 표시한다.
- 모델/화면 테스트를 보강한다.
- 작업 리포트를 작성한다.

## 제외 범위

- 서버 DTO 타입을 `OffsetDateTime`으로 바꾸는 계약 변경
- DB timezone 설정 변경
- 알림 상세 화면 추가
- 공통 날짜 유틸 전체 개편

## 구현 순서

1. `NotificationItem.fromJson`의 `createdAt` 파싱을 서버 KST LocalDateTime 기준으로 보정한다.
2. `NotificationListScreen`의 시간 라벨을 `상대 시간 · HH:mm` 형식으로 변경한다.
3. 미래 timestamp는 상대 시간 대신 절대 날짜/시각만 표시한다.
4. 모델 파싱 테스트와 화면 테스트를 보강한다.
5. Flutter 테스트와 analyze를 실행한다.
6. report를 작성한다.

## 수용 기준

- 에뮬레이터 timezone이 GMT여도 서버 KST timestamp를 실제 시각으로 해석한다.
- 알림 row에서 실제 시각이 보인다.
- 미래 timestamp가 `방금 전`으로 고정되지 않는다.
- 기존 제목/본문/읽음 처리 흐름은 유지된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 알림 모델과 알림 목록 화면에 한정되어 있다.
- timestamp 파싱과 UI 표시가 같은 원인에 묶여 있어 순차 검증이 안전하다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 전체 작업을 직접 수행한다.

## 검증 계획

- `flutter test test/features/mypage/models/notification_response_test.dart`
- `flutter test test/features/mypage/screens/notification_list_screen_test.dart`
- `flutter analyze lib/features/mypage/models/notification_response.dart lib/features/mypage/screens/notification_list_screen.dart test/features/mypage/models/notification_response_test.dart test/features/mypage/screens/notification_list_screen_test.dart`
