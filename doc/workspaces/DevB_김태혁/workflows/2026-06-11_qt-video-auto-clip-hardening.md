# 2026-06-11 QT 영상 자동 클립 준비 후속 보강 워크플로우

## 목표

PR #498 승인 후 남은 후속 보강 항목을 처리한다.

- 이벤트 핸들러 실패 로그 경로 테스트
- 비공개 QT / 역전 timecode skip 분기 테스트
- startup 보정과 import 이벤트가 같은 QT 본문을 동시에 준비할 때 active unique 충돌 없이 멱등 처리

## 범위

- 새 API, DB schema, OpenAPI 변경 없음
- 기존 QT 영상 조회 계약 변경 없음
- `service-bible`의 QT 영상 자동 준비 로직과 테스트만 보강

## 구현 계획

1. 이벤트 실패 로그 테스트
   - `QtVideoClipPreparationListenerTest` 추가
   - event handler 실패 시 log message에 `eventId`, `eventType`, `handlerName`, `qtPassageId`, `retryable=true`가 포함되는지 검증

2. skip 분기 테스트
   - unpublished QT는 clip 생성 없이 skip
   - 계산된 timecode가 `start >= end`이면 clip 생성 없이 skip

3. 동시 실행 멱등 처리
   - 같은 `qtPassageId`에 대해 JVM 내부 preparation lock 적용
   - lock release는 트랜잭션 completion 이후 수행
   - 쓰기 트랜잭션은 `REQUIRES_NEW + READ_COMMITTED` 유지
   - startup 보정과 event 준비가 동시에 실행되어도 active clip row는 1개만 유지

## 검증 계획

- `.\gradlew.bat :service-bible:test --tests "com.qtai.domain.qtvideo.internal.*"`
- `.\gradlew.bat :service-bible:test`
- `git diff --check`
