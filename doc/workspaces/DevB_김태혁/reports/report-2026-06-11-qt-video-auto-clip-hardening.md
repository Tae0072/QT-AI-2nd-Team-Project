# Report — 2026-06-11 qt-video-auto-clip-hardening

## 요약

PR #498 승인 후 리뷰어가 남긴 후속 보강 항목을 처리했다.

이번 변경은 기존 QT 영상 자동 준비 기능의 운영 안정성 강화이며, 새 API나 DB schema 변경은 없다.

## 변경 내용

1. 이벤트 실패 로그 테스트 추가
   - `QtVideoClipPreparationListenerTest`
   - event handler 실패 로그에 `eventId`, `eventType`, `handlerName`, `qtPassageId`, `retryable=true`, error 정보가 포함되는지 검증

2. skip 분기 테스트 추가
   - 비공개 QT는 자동 clip 생성 없이 skip
   - 계산된 timecode가 역전된 경우 자동 clip 생성 없이 skip

3. startup-event 동시 실행 멱등 처리 보강
   - 같은 `qtPassageId`에 대해 preparation lock 적용
   - 트랜잭션 completion 이후 lock을 해제해 두 번째 실행이 첫 번째 commit 이후 active clip을 다시 조회하게 함
   - `READ_COMMITTED` isolation을 명시해 두 번째 실행이 최신 commit 상태를 확인하도록 함
   - 통합 테스트에서 `prepareToday()`와 `prepare(qtPassageId)` 동시 실행 후 active clip row가 1개만 남는지 검증

## 검증

- `.\gradlew.bat :service-bible:test --tests "com.qtai.domain.qtvideo.internal.*"` 통과
- `.\gradlew.bat :service-bible:test` 통과
- `git diff --check` 통과

## 남은 리스크

- 현재 lock은 단일 JVM 내 startup-event 동시 실행을 제어한다.
- 다중 인스턴스 간 완전한 분산 lock은 이번 후속 보강 범위가 아니며, 운영 배포 구성이 다중 인스턴스로 확대되면 DB advisory lock 또는 별도 distributed lock 정책을 검토한다.
