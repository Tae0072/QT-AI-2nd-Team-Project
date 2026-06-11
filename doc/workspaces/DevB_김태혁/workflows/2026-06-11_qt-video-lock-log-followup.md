# 2026-06-11 QT 영상 자동 클립 lock/log 후속 개선 워크플로우

## 목표

PR #502 승인 후 남은 WARN 수준 후속 개선 항목을 처리한다.

- preparation lock cleanup 경합 가능성 개선
- startup 실패 로그 경로 테스트 추가

## 배경

PR #502에서 같은 `qtPassageId`에 대한 startup 보정과 import 이벤트 동시 실행을 JVM 내부 lock으로 직렬화했다. 다만 lock 해제 후 map cleanup 과정은 `isLocked()` / `hasQueuedThreads()` 관찰값에 의존하므로, 극단적인 경합 상황에서는 cleanup 판단이 보수적으로 동작할 수 있다.

또한 이벤트 실패 로그 경로는 테스트했지만, startup 실패 로그 경로는 별도 테스트가 없었다.

## 범위

- 새 API, DB schema, OpenAPI 변경 없음
- 기존 QT 영상 자동 준비 정책 변경 없음
- 다중 인스턴스 분산 lock은 범위 밖
- `service-bible` qtvideo 내부 lock/log 보강과 테스트만 포함

## 구현 계획

1. lock cleanup 개선
   - 같은 `qtPassageId` lock을 map에서 제거할 때 현재 lock instance와 동일한 경우에만 제거한다.
   - unlock 이후 새 waiter가 들어오는 경합을 고려해 cleanup 조건을 보수적으로 유지한다.
   - 필요 시 cleanup 메서드 단위 테스트 또는 동시 실행 통합 테스트를 보강한다.

2. startup 실패 로그 테스트
   - `QtVideoClipPreparationListener.prepareTodayOnStartup()`에서 `prepareToday()` 실패 시 WARN 로그가 남는지 검증한다.
   - 로그에 `eventType=ApplicationReadyEvent`, `handlerName`, `retryable=true`, error 정보가 포함되는지 확인한다.

## 검증 계획

- `.\gradlew.bat :service-bible:test --tests "com.qtai.domain.qtvideo.internal.*"`
- `.\gradlew.bat :service-bible:test`
- `git diff --check`
