# Report — 2026-06-11 qt-video-lock-log-followup

## 요약

PR #502 승인 후 남은 WARN 수준 후속 개선 항목을 처리하기 위한 작업 문서다.

이번 작업은 QT 영상 자동 클립 준비 기능의 운영 안정성 보강이며, 사용자 API나 DB schema 변경은 없다.

## 처리 대상

1. preparation lock cleanup 경합 개선
   - 같은 `qtPassageId` 준비 작업의 동시 실행을 직렬화하는 lock map cleanup을 더 명확하게 만든다.
   - DB unique 제약은 최종 방어선으로 유지한다.

2. startup 실패 로그 경로 테스트
   - `ApplicationReadyEvent` 경로에서 `prepareToday()` 실패 시 로그 메타데이터가 남는지 검증한다.
   - event type, handler name, retryable flag, error type/message를 확인한다.

## 변경 예정 파일

- `qtai-server/service-bible/src/main/java/com/qtai/domain/qtvideo/internal/QtVideoClipPreparationService.java`
- `qtai-server/service-bible/src/test/java/com/qtai/domain/qtvideo/internal/QtVideoClipPreparationListenerTest.java`
- 필요 시 `QtVideoClipPreparationEventIntegrationTest`

## 검증 예정

- `.\gradlew.bat :service-bible:test --tests "com.qtai.domain.qtvideo.internal.*"`
- `.\gradlew.bat :service-bible:test`
- `git diff --check`

## 남은 리스크

- 단일 JVM 내부 lock만 다룬다.
- 다중 인스턴스 환경의 완전한 분산 멱등성은 후속 운영 정책으로 별도 검토한다.
