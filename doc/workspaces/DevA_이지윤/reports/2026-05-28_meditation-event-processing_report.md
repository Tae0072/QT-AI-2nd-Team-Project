# Report - 2026-05-28 meditation-event-processing

## 작업 요약

- `MEDITATION` 노트 생성/수정/삭제 시 `JournalChangedEvent`를 발행하도록 했다.
- `journal_events` 이력 테이블과 JPA Entity/Repository/AFTER_COMMIT Handler를 추가했다.
- `/api/v1/me/meditation-calendar?month=YYYY-MM`를 note 도메인 Controller/UseCase/Service로 구현했다.
- 기존 `MyPageController`의 미구현 묵상 달력 매핑을 제거해 중복 URL 매핑을 방지했다.
- OpenAPI에 묵상 달력 경로와 `month`, `days`, `summary` 응답 스키마를 반영했다.

## 변경 파일 : 
1) 공개 API 계약
2) 이벤트 이력 저장
3) 노트 서비스와 달력 조회
4) 중복 매핑 정리
5) 테스트
6) API 문서

### 1. 공개 API 계약

공통 경로: `qtai-server/src/main/java/com/qtai/domain/note/api/`

- `JournalEventType.java`: 묵상 노트 이벤트 종류를 `JOURNAL_CREATED`, `JOURNAL_UPDATED`, `JOURNAL_DELETED`로 명시한 enum이다.
- `JournalChangedEvent.java`: 노트 이벤트 발행 시 필요한 eventId, memberId, noteId, 상태값을 한 번에 전달하는 공개 event record다.
- `GetMeditationCalendarUseCase.java`: 묵상 달력 조회 기능을 외부에서 호출할 수 있게 만든 UseCase 인터페이스다.

공통 경로: `qtai-server/src/main/java/com/qtai/domain/note/api/dto/`

- `MeditationCalendarResponse.java`: 묵상 달력 응답의 `month`, `days`, `summary` 구조를 정의한 DTO다.

### 2. 이벤트 이력 저장

공통 경로: `qtai-server/src/main/java/com/qtai/domain/note/internal/`

- `JournalEvent.java`: `journal_events` 테이블에 저장될 JPA Entity로, 처리 상태와 실패 메시지, 재처리 횟수를 가진다.
- `JournalEventStatus.java`: 이벤트 처리 상태를 `PENDING`, `PROCESSED`, `FAILED`로 구분하는 enum이다.
- `JournalEventRepository.java`: 이벤트 이력을 저장하고 eventId 중복 여부를 확인하는 Repository다.
- `JournalEventHandler.java`: 커밋 이후 `JournalChangedEvent`를 받아 이벤트 이력을 저장하는 AFTER_COMMIT 핸들러다.

공통 경로: `qtai-server/src/main/resources/db/migration/`

- `V14__create_journal_events.sql`: `journal_events` 테이블과 eventId unique 제약, 상태/시간/오류 컬럼을 추가하는 migration이다.

### 3. 노트 서비스와 달력 조회

공통 경로: `qtai-server/src/main/java/com/qtai/domain/note/internal/`

- `NoteService.java`: `MEDITATION` 노트 생성/수정/삭제 시 journal 이벤트를 발행하도록 기존 노트 저장 흐름을 확장했다.
- `NoteRepository.java`: 묵상 달력 집계를 위해 월 범위의 저장 완료 노트를 조회하는 쿼리를 추가했다.
- `MeditationCalendarService.java`: 저장 완료 노트를 날짜별로 묶고 저장일 수, 저장 노트 수, 연속 묵상일을 계산한다.

공통 경로: `qtai-server/src/main/java/com/qtai/domain/note/web/`

- `MeditationCalendarController.java`: `/api/v1/me/meditation-calendar` 요청을 받아 인증 회원과 월 정보를 UseCase에 전달한다.

### 4. 중복 매핑 정리

공통 경로: `qtai-server/src/main/java/com/qtai/domain/member/web/`

- `MyPageController.java`: 기존 미구현 묵상 달력 매핑을 제거해 새 note 도메인 Controller와 URL이 겹치지 않게 했다.

### 5. 테스트

공통 경로: `qtai-server/src/test/java/com/qtai/domain/note/internal/`

- `NoteServiceTest.java`: 묵상 노트 생성/수정/삭제 이벤트 발행 조건과 자유 노트 미발행을 검증한다.
- `JournalEventHandlerTest.java`: 이벤트 이력 저장, 중복 eventId 무시, 실패 상태 기록을 검증한다.
- `MeditationCalendarServiceTest.java`: 날짜별 집계, 빈 날짜 처리, 연속 묵상일 계산을 검증한다.

공통 경로: `qtai-server/src/test/java/com/qtai/domain/note/web/`

- `MeditationCalendarControllerTest.java`: 인증 회원 위임, 기본 월 처리, 잘못된 월 형식 거부를 검증한다.

공통 경로: `qtai-server/src/test/java/com/qtai/domain/member/web/`

- `MyPageControllerTest.java`: 제거된 미구현 묵상 달력 5xx 테스트를 삭제해 현재 Controller 책임에 맞췄다.

### 6. API 문서

공통 경로: `qtai-server/apis/api-v1/`

- `openapi.yaml`: 묵상 달력 API 경로와 실제 응답 DTO에 맞는 스키마를 추가했다.

## 검증 결과

- `cd qtai-server && .\gradlew.bat test --tests "*NoteServiceTest" --tests "*JournalEventHandlerTest" --tests "*MeditationCalendar*" --tests "*MyPageControllerTest"` 성공
- `cd qtai-server && .\gradlew.bat test --tests "*ArchitectureBoundaryTest"` 성공
- `cd qtai-server && .\gradlew.bat test` 성공
- `cd qtai-server && .\gradlew.bat build` 성공
- `git diff --check` 통과
- `rg -n "com\\.qtai\\.domain\\.(member|bible|qt|sharing|mission|notification)\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/note --glob "*.java"` 결과 없음
- 금지 기술 검색은 기존 `SearchBibleUseCase`의 "RAG/Vector DB 금지" 정책 주석 1건만 탐지했다.
- 금지 데이터/secret 검색은 기존 `application-local.yml`의 "plain private key 커밋 금지" 정책 주석 1건만 탐지했다.

## 미실행 또는 실패 검증

- `jacocoTestReport`, `jacocoTestCoverageVerification`: 현재 Gradle 프로젝트에 task가 없어 실패했다.
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml`: `.spectral.yaml` 등 ruleset 파일이 없어 Spectral이 실행을 중단했다.
- `gitleaks detect --source . --redact --exit-code 1`: 로컬에 `gitleaks` 명령이 설치되어 있지 않아 실행하지 못했다.

## 남은 작업

- 실패한 journal 이벤트 재처리 관리자/배치 API는 후속 workflow 범위다.
- 미션 진행률, 알림, sharing 삭제 스냅샷 연동은 별도 도메인 workflow로 남겼다.
