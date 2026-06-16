# Workflow - 2026-06-12 notification-list-content-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | M-02 / notification |
| 트리거 | 모바일 알림 목록에서 알림 row와 시간은 보이지만 제목/내용이 표시되지 않음 |
| 기준 파일 | `flutter-app/lib/features/mypage/models/notification_response.dart`, `flutter-app/lib/features/mypage/screens/notification_list_screen.dart`, `qtai-server/service-user/src/main/java/com/qtai/domain/notification/api/dto/NotificationResponse.java` |

## 작업 목표

사용자 앱 알림 목록 화면에서 서버가 내려주는 알림 제목과 본문을 표시한다. 현재 서버 응답 계약은 `title`, `body`, `read`인데 Flutter 모델과 화면은 `message` 중심으로 읽고 있어 실제 알림 내용이 빈 문자열로 렌더링된다.

## 범위

- Flutter 알림 응답 모델을 서버 계약의 `title`, `body`, `read` 필드에 맞춘다.
- 알림 목록 row에서 제목을 기본 표시하고, 본문이 있으면 시간과 함께 보조 텍스트로 표시한다.
- 기존 `message` 응답이 남아 있는 로컬/구버전 데이터에 대해서는 fallback만 유지한다.
- 모델 파싱 회귀 테스트를 추가한다.
- Flutter 테스트를 실행한다.
- 작업 리포트를 작성한다.

## 제외 범위

- 알림 발송 정책 변경
- 관리자 공지/알림 API 변경
- FCM 푸시 구현
- 알림 상세 화면 추가
- 모바일 전체 디자인 개편

## 구현 순서

1. `NotificationItem` 모델에 `title`, `body`, `read` 필드를 추가하고 `message`는 표시용 fallback으로 정리한다.
2. `NotificationListScreen`에서 `item.title`과 `item.body`를 사용해 알림 내용을 렌더링한다.
3. `notification_response_test.dart`를 추가해 `title/body/read` 서버 응답 파싱을 검증한다.
4. 알림 목록 관련 Flutter 테스트를 실행한다.
5. report를 작성한다.

## 수용 기준

- 서버 응답의 `title`이 알림 row 제목으로 표시된다.
- 서버 응답의 `body`가 비어 있지 않으면 시간과 함께 보인다.
- `read` boolean이 있으면 읽음 상태 판단에 사용한다.
- `read`가 없는 구형 응답은 `readAt`으로 읽음 상태를 판단한다.
- 기존 `message`만 있는 응답도 빈 알림으로 보이지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 Flutter 알림 모델과 단일 화면, 단일 테스트에 집중되어 있다.
- 서버 계약 확인과 Flutter 렌더링 수정이 같은 맥락이라 메인 에이전트가 순차로 처리하는 편이 안전하다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 전체 작업을 직접 수행한다.

## 검증 계획

- `flutter test test/features/mypage/models/notification_response_test.dart`
- 가능하면 `flutter test test/features/mypage/services/mypage_repository_test.dart`
- 필요 시 `flutter analyze`는 변경 범위가 작아도 최종 확인용으로 실행한다.
