# Workflow - 2026-05-27 meditation-event-processing

| 항목 | 내용 |
| --- | --- |
| 담당자 | DevA_이지윤 |
| 작업/리뷰 | Note 도메인 owner 이승욱, Lead 강태오 리뷰 필요 |
| 브랜치 | `feature/note-meditation-events` |
| PR 대상 | `dev` |
| 관련 F-ID | F-03, F-13 |
| 트리거 | `JournalEvent`가 임시 스켈레톤으로 남아 있고, F-03/F-13에서 QT 노트 생성·수정·삭제 이벤트와 묵상 달력 반영을 요구하므로 구현 전에 이벤트 처리 계약을 고정한다. |
| 기준 문서 | `AGENTS.md`, `CODE_CONVENTION.md`, `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/05_시퀀스_다이어그램.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `C:/Users/G/Desktop/깃허브/qt-ai-ci.yml`, `C:/Users/G/Desktop/깃허브/claude-pr-review.yml`, `C:/Users/G/Desktop/깃허브/CODEOWNERS`, `C:/Users/G/Desktop/깃허브/pull_request_template.md` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/note/**`, `qtai-server/src/test/java/com/qtai/domain/note/**`, `qtai-server/src/main/java/com/qtai/domain/member/web/MyPageController.java`, `qtai-server/src/test/java/com/qtai/domain/member/web/**`, `qtai-server/src/main/resources/db/migration/**`, `qtai-server/apis/api-v1/openapi.yaml`, `doc/workspaces/DevA_이지윤/reports/**` |

## 작업 목표

QT 묵상 노트가 `SAVED`로 확정되거나 `DELETED`로 삭제될 때 변경 이벤트를 도메인 내부에서 안정적으로 기록하고, `GET /api/v1/me/meditation-calendar`가 `notes.saved_at` 기준으로 월별 완료 상태를 반환하도록 구현 범위를 정의한다. 이벤트 기록은 감사·추적 목적의 `journal_events` 테이블에 남기되, 달력의 진실 원천은 `notes` 테이블로 유지한다.

이 workflow는 사용자 노트 본문을 AI가 생성·수정·평가하지 않는다는 요구사항, 자동 저장 금지, 기본 비공개, 묵상 1일 1건 멱등 정책, Spring `ApplicationEventPublisher` 사용과 Kafka 금지 규칙을 함께 만족해야 한다.

## 범위

- `JournalEvent` 임시 클래스를 실제 JPA Entity로 전환하고 `journal_events` migration을 추가한다.
- 이벤트 타입은 `JOURNAL_CREATED`, `JOURNAL_UPDATED`, `JOURNAL_DELETED`로 고정한다. 기존 Claude PR 리뷰 규칙의 `JournalCreated/Updated/DeletedEvent` 명명 흐름과 맞춘다.
- 이벤트 payload에는 `eventId`, `eventType`, `memberId`, `noteId`, `qtPassageId`, `category`, `previousStatus`, `nextStatus`, `savedDate`, `occurredAt`을 포함한다.
- 이벤트 발행은 `NoteService.create`, `NoteService.update`, `NoteService.delete`의 트랜잭션 안에서 수행하고, 핸들러는 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 처리한다.
- 이벤트 기록 핸들러는 `journal_events`에 멱등 저장한다. 동일 `eventId`가 재전달되면 중복 저장하지 않고 성공 처리한다.
- 이벤트 핸들러 실패 로그에는 `eventId`, event type, handler name, error message를 남긴다. 민감 정보와 노트 본문은 로그에 남기지 않는다.
- 묵상 달력은 `notes.status=SAVED`, `notes.deleted_at IS NULL`, `notes.saved_at` 날짜 기준으로 월별 집계한다.
- `MyPageController`의 `GET /api/v1/me/meditation-calendar` NOT_IMPLEMENTED 응답을 note 도메인의 calendar UseCase 호출로 교체한다.
- `member` 도메인은 note의 `api` UseCase와 DTO만 import한다. `note.internal` 직접 import는 금지한다.
- OpenAPI `GET /api/v1/me/meditation-calendar` 계약과 실제 응답 DTO를 맞춘다.
- PR 본문에는 이 workflow와 작업 후 report 경로를 포함한다.

## 제외 범위

- 오프라인 큐잉, 로컬 SQLite 캐시, 기기 간 충돌 해결은 v1.1 이후 범위로 남긴다.
- 별도 calendar aggregate 테이블, Redis 캐시, 배치 재계산 작업은 추가하지 않는다.
- Kafka, Spring Kafka, 외부 메시지 브로커, SSE는 사용하지 않는다.
- 알림 생성, 미션 진행률 갱신, 공유글 스냅샷 갱신은 이번 범위에서 제외한다.
- AI가 노트 내용을 분석하거나 추천하는 기능은 구현하지 않는다.
- 실제 성경 본문 텍스트, 개역개정/ESV/NIV seed·fixture·response 데이터는 추가하지 않는다.
- 관리자용 묵상 이벤트 조회 API는 만들지 않는다.

## 이벤트 처리 계약

| 구분 | 기준 |
| --- | --- |
| 생성 이벤트 | `MEDITATION` 노트가 새로 생성되고 `nextStatus=SAVED`인 경우 `JOURNAL_CREATED` 발행 |
| 수정 이벤트 | 기존 `MEDITATION` 노트가 `SAVED` 상태로 저장 확정되거나 저장 시각이 갱신되는 경우 `JOURNAL_UPDATED` 발행 |
| 삭제 이벤트 | 기존 `MEDITATION` 노트가 `DELETED`로 전환되는 경우 `JOURNAL_DELETED` 발행 |
| DRAFT 처리 | `DRAFT` 생성·수정은 달력 완료 상태가 아니므로 이벤트 기록 대상에서 제외 |
| SAVED -> DRAFT | 완료 취소 성격이므로 `JOURNAL_UPDATED`를 발행하고 `nextStatus=DRAFT`, `savedDate=null`로 기록 |
| 중복 방지 | `journal_events.event_id` unique 제약으로 핸들러 재진입을 멱등 처리 |
| 달력 기준 | 이벤트 테이블이 아니라 `notes.saved_at`과 `notes.status` 기준으로 계산 |
| 시간대 | 서버 시간은 `Asia/Seoul` 정책을 따른다. 저장 날짜는 `savedAt`을 KST `LocalDate`로 변환해 사용 |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/api/GetMeditationCalendarUseCase.java` | member/web에서 호출할 note 공개 포트 |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/api/dto/MeditationCalendarResponse.java` | `month`, `days`, `summary` 응답 DTO |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/api/dto/MeditationCalendarDay.java` | 일자별 `date`, `saved`, `savedNoteCount`, `meditationNoteId`, `categories` |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/api/dto/MeditationCalendarSummary.java` | `savedDays`, `savedNoteCount`, `meditationStreakDays` |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/JournalEvent.java` | 임시 스켈레톤을 Entity로 전환 |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/internal/JournalEventType.java` | 이벤트 타입 enum |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/internal/JournalEventRepository.java` | 이벤트 멱등 저장과 조회 |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/internal/JournalEventHandler.java` | AFTER_COMMIT 이벤트 저장 핸들러 |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/internal/event/JournalChangedEvent.java` | ApplicationEventPublisher payload record |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteService.java` | 상태 전이 후 이벤트 발행, 달력 UseCase 구현 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteRepository.java` | 월별 달력 집계 쿼리 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/member/web/MyPageController.java` | 묵상 달력 엔드포인트를 note UseCase에 위임 |
| Create | `qtai-server/src/main/resources/db/migration/V12__create_journal_events.sql` | `journal_events` DDL과 인덱스 추가 |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | 묵상 달력 응답 스키마와 실패 응답 정합화 |
| Test | `qtai-server/src/test/java/com/qtai/domain/note/internal/JournalEventHandlerTest.java` | 이벤트 저장, 중복 eventId, 실패 로그 정책 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | 상태 전이별 이벤트 발행 조건 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteRepositoryIntegrationTest.java` | 월별 달력 집계와 삭제 제외 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/member/web/MyPageControllerTest.java` | calendar API envelope, 인증, UseCase 위임 검증 |
| Test | `qtai-server/src/test/java/com/qtai/common/ArchitectureBoundaryTest.java` | member -> note.api, note -> 타 도메인 internal 금지 검증 |

## 구현 순서

1. `git status --short`로 미추적 파일과 사용자 변경을 확인한다. 기존 `2026-05-27_note-meditation-lifecycle.md`와 report는 덮어쓰지 않는다.
2. `07_요구사항_정의서.md` F-03/F-13, `03_아키텍처_정의서.md` note 책임, `04_API_명세서.md` §4.6.2를 다시 확인한다.
3. `GetMeditationCalendarUseCase`와 calendar DTO를 `domain.note.api`에 먼저 정의한다.
4. `MyPageController`는 `GetMeditationCalendarUseCase`만 주입받도록 수정하고, Controller가 note repository나 internal 타입을 직접 알지 않게 한다.
5. `JournalEvent`를 `BaseEntity` 상속 Entity로 구현하고 `eventId`, `eventType`, `memberId`, `noteId`, `qtPassageId`, `category`, `previousStatus`, `nextStatus`, `savedDate`, `occurredAt` 컬럼을 명시한다.
6. `V12__create_journal_events.sql`에 `event_id` unique, `member_id + saved_date`, `note_id + occurred_at` 인덱스를 추가한다.
7. `JournalChangedEvent` record를 만들고 `eventId`는 UUID 문자열로 생성한다.
8. `NoteService.create/update/delete`에서 상태 전이 전후 값을 비교해 이벤트 발행 여부를 결정한다.
9. 이벤트 발행은 DB 저장 성공 후 같은 트랜잭션 안에서 `ApplicationEventPublisher.publishEvent`를 호출한다.
10. `JournalEventHandler`는 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 이벤트를 받아 `JournalEventRepository`에 저장한다.
11. 핸들러에서 unique 위반 또는 이미 존재하는 `eventId`는 멱등 성공으로 처리한다.
12. 핸들러 실패 시 `log.warn("journal event handler failed: eventId={}, eventType={}, handler={}, error={}", ...)` 형태로 남기고 노트 본문, title, token, 개인정보는 포함하지 않는다.
13. `NoteRepository`에 월 시작일 이상, 다음 달 시작일 미만 범위의 `SAVED` 노트를 조회하는 집계 쿼리를 추가한다.
14. 달력 응답은 월 내 일자 중 저장된 날만 `days`에 담고, `summary.savedDays`, `summary.savedNoteCount`, `summary.meditationStreakDays`를 계산한다.
15. `meditationNoteId`는 해당 일자에 `MEDITATION` `SAVED` 노트가 있으면 그 id를 넣고, 없으면 null로 둔다.
16. `categories`는 해당 날짜에 저장된 노트 카테고리를 중복 없이 반환한다.
17. `SAVED -> DRAFT` 또는 `DELETE` 이후에는 `savedAt=null` 또는 `deletedAt!=null` 기준으로 달력 완료 표시에서 제외되는지 확인한다.
18. OpenAPI와 Controller 응답 DTO가 `04_API_명세서.md`의 envelope와 calendar schema를 따르도록 갱신한다.
19. 구현 완료 후 report에 실행 명령, 통과/실패 결과, 미실행 사유, 남은 Lead 검토 항목을 기록한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `NoteServiceTest` | `MEDITATION` `SAVED` 생성 시 `JOURNAL_CREATED` 발행 |
| `NoteServiceTest` | `DRAFT` 생성·수정 시 이벤트 미발행 |
| `NoteServiceTest` | `DRAFT -> SAVED`, `SAVED -> SAVED`, `SAVED -> DRAFT`, `SAVED -> DELETED`별 이벤트 타입과 payload |
| `NoteServiceTest` | 자유 노트 카테고리는 journal event 발행 대상이 아님 |
| `JournalEventHandlerTest` | AFTER_COMMIT 이벤트 저장 성공 |
| `JournalEventHandlerTest` | 동일 `eventId` 중복 수신 시 멱등 성공 |
| `JournalEventHandlerTest` | 저장 실패 로그에 `eventId`, event type, handler name, error message가 포함되고 본문은 포함되지 않음 |
| `NoteRepositoryIntegrationTest` | 월별 달력 집계가 `SAVED`와 `deleted_at IS NULL`만 포함 |
| `NoteRepositoryIntegrationTest` | `DRAFT`, `DELETED`, `saved_at IS NULL` 노트는 달력에서 제외 |
| `MyPageControllerTest` | `GET /api/v1/me/meditation-calendar?month=2026-05`가 note UseCase에 위임되고 공통 envelope로 응답 |
| `MyPageControllerTest` | 인증 주체가 없으면 `UNAUTHORIZED` |
| `ArchitectureBoundaryTest` | `domain.member.web`은 `domain.note.api`만 import하고 `domain.note.internal`을 import하지 않음 |
| `JpaEntityDdlTest` | `journal_events` Entity와 migration 컬럼 정합성 |

## 수용 기준

- [ ] `JournalEvent` 임시 클래스가 실제 Entity와 Repository, migration으로 대체된다.
- [ ] `MEDITATION` 저장 확정·저장 취소·삭제 이벤트만 기록되고 `DRAFT` 단순 수정은 기록되지 않는다.
- [ ] 이벤트는 `ApplicationEventPublisher`와 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 처리된다.
- [ ] Kafka, SSE, 외부 메시지 브로커가 추가되지 않는다.
- [ ] 이벤트 핸들러 실패 로그에는 `eventId`, event type, handler name, error message가 있고 노트 본문과 민감 정보가 없다.
- [ ] 달력 API는 `notes.status=SAVED`, `notes.deleted_at IS NULL`, `notes.saved_at` 기준으로 월별 완료 상태를 반환한다.
- [ ] QT 노트를 삭제하거나 `SAVED -> DRAFT`로 바꾸면 해당 날짜 완료 표시가 제거된다.
- [ ] 기록 없는 날짜 조회가 새 노트를 자동 생성하지 않는다.
- [ ] Controller는 Repository를 직접 호출하지 않는다.
- [ ] 도메인 간 import는 `api`/DTO 경계를 지킨다.
- [ ] 테스트 fixture와 OpenAPI 예시에 개역개정, ESV, NIV, 성서유니온/두란노 본문 텍스트, plain secret/token/password/private key 예시가 없다.
- [ ] PR 본문에 F-03, F-13과 workflow/report 경로가 포함된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 이벤트 발행 조건, 노트 상태 전이, 달력 집계가 같은 `NoteService` 흐름에 강하게 연결되어 있어 순차 검증이 안전하다.
- member controller 위임, note api 계약, migration, 테스트가 서로 맞물려 있어 메인 에이전트가 계약을 하나로 고정한 뒤 진행하는 편이 충돌을 줄인다.
- 이미 같은 날짜의 note lifecycle workflow/report 미추적 파일이 존재하므로 병렬 편집보다 변경 파일을 좁게 관리해야 한다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 이벤트 계약, 구현, 테스트, OpenAPI 정합성, 최종 report 작성을 직접 수행한다.

## 검증 계획

- `git diff --check`
- `.\gradlew.bat test --tests "*NoteServiceTest"`
- `.\gradlew.bat test --tests "*JournalEventHandlerTest"`
- `.\gradlew.bat test --tests "*NoteRepositoryIntegrationTest"`
- `.\gradlew.bat test --tests "*MyPageControllerTest"`
- `.\gradlew.bat test --tests "*JpaEntityDdlTest"`
- `.\gradlew.bat test --tests "*ArchitectureBoundaryTest"`
- `.\gradlew.bat test --tests "*Note*" --tests "*MyPageControllerTest" --tests "*ArchitectureBoundaryTest"`
- `.\gradlew.bat build`
- `.\gradlew.bat test jacocoTestReport`
- `.\gradlew.bat jacocoTestCoverageVerification`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`
- `rg -n "KafkaTemplate|spring-kafka|@KafkaListener|SseEmitter|text/event-stream|VectorStore|EmbeddingStore|javax\\." qtai-server/src/main qtai-server/src/test`
- `rg -n "개역개정|\\bESV\\b|\\bNIV\\b|plain secret|password|private key" qtai-server/src/main qtai-server/src/test qtai-server/apis/api-v1/openapi.yaml doc/workspaces/DevA_이지윤/workflows/2026-05-27_meditation-event-processing.md`

`jacocoTestReport`, `jacocoTestCoverageVerification`, `spectral`, `gitleaks`가 로컬 도구 또는 Gradle task 부재로 실행되지 않으면 report와 최종 응답에 미실행 사유를 적고 CI에서 재확인한다.

## PR/커밋 기준

- 브랜치는 `dev` 최신에서 `feature/note-meditation-events`로 생성한다.
- 커밋 메시지 예시는 `feat(note): record meditation journal events`로 둔다.
- PR 대상은 `dev`다.
- PR 본문에는 `F-03`, `F-13`, 기준 문서 섹션, workflow 경로, report 경로, 검증 명령 결과를 포함한다.
- CODEOWNERS 기준 note 도메인 owner는 `@LeeSeung-Wook`, Lead는 `@Tae0072`다.
- Claude PR 리뷰 `APPROVE`와 CI success 전에는 merge하지 않는다.

## 후속 작업으로 남길 항목

- 묵상 달력 로컬 캐시와 오프라인 큐잉 정책은 v1.1 workflow에서 별도 정의한다.
- 이벤트 기반 미션 진행률 갱신은 mission 도메인 owner와 별도 API/UseCase 계약을 잡은 뒤 진행한다.
- 공유글 삭제 영향 반영은 sharing workflow에서 처리한다.
- 운영자용 journal event 조회나 재처리 화면은 v1 운영 요구가 확정될 때 추가 검토한다.
