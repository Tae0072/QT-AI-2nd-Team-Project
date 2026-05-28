# Workflow - 2026-05-28 meditation-event-processing

| 항목 | 내용 |
| --- | --- |
| 담당자 | 이지윤 |
| 협업/리뷰 | 이승욱(Note 도메인 공동 owner), 김지민(Note/Sharing 연계 owner), 강태오(Lead) |
| 브랜치 | `feature/meditation-events-calendar` |
| PR 대상 | `dev` |
| 관련 F-ID | F-03, F-13 |
| 트리거 | QT 묵상 노트 생성/수정/삭제 상태가 묵상 달력에 반영되어야 하지만, 현재 `JournalEvent`는 스켈레톤이고 `/api/v1/me/meditation-calendar`는 미구현 응답이다. |
| 기준 문서 | `AGENTS.md`, `CODE_CONVENTION.md`, `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `.github/workflows/qt-ai-ci.yml`, `.github/workflows/claude-pr-review.yml`, `.github/pull_request_template.md`, `.github/CODEOWNERS` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/note/**`, `qtai-server/src/test/java/com/qtai/domain/note/**`, `qtai-server/src/main/java/com/qtai/domain/member/web/MyPageController.java`, `qtai-server/src/test/java/com/qtai/domain/member/web/MyPageControllerTest.java`, `qtai-server/src/main/resources/db/migration/**`, `qtai-server/apis/api-v1/openapi.yaml` |

## 작업 목표

QT 묵상 노트(`category=MEDITATION`)가 생성, 저장 확정, 수정, 삭제될 때 서버 내부 이벤트를 발행하고 처리 이력을 남긴다. 이벤트는 Spring `ApplicationEventPublisher`와 `@TransactionalEventListener(phase = AFTER_COMMIT)`만 사용하며, Kafka 같은 외부 메시지 브로커는 도입하지 않는다.

묵상 달력은 이벤트 이력만 믿지 않고 `notes.status=SAVED`, `notes.deleted_at IS NULL`, `notes.saved_at` 기준으로 조회한다. 이벤트 이력은 후속 부수 효과 추적, 실패 로그, 재처리 후보 식별을 위한 서버 내부 자료로 사용하고, 사용자 응답에는 노출하지 않는다.

## 범위

- `domain.note.api`에 외부 도메인이 안전하게 참조할 수 있는 묵상 이벤트 record를 정의한다.
- 이벤트 record는 `eventId`, `memberId`, `noteId`, `qtPassageId`, `eventType`, `previousStatus`, `currentStatus`, `occurredAt`을 포함한다.
- 이벤트 타입은 `JOURNAL_CREATED`, `JOURNAL_UPDATED`, `JOURNAL_DELETED`로 제한한다.
- `NoteService.create()`는 `MEDITATION` 노트 생성 후 이벤트를 발행한다.
- `NoteService.update()`는 `MEDITATION` 노트의 상태, QT 본문, 저장 시각, 삭제 여부가 달력 결과에 영향을 줄 때 이벤트를 발행한다.
- `NoteService.delete()`는 `MEDITATION` 노트 삭제 시 이벤트를 발행하고, 삭제된 노트의 `activeUniqueKey=null` 처리를 유지한다.
- 이벤트 발행은 같은 트랜잭션 안에서 `ApplicationEventPublisher.publishEvent(...)`를 호출하고, 실제 후속 처리는 커밋 이후 리스너에서 수행한다.
- `JournalEvent`를 JPA Entity로 전환하고 `journal_events` 테이블을 신규 migration으로 추가한다.
- `JournalEventRepository`를 추가해 이벤트 이력, 처리 상태, 실패 메시지, 재처리 후보를 저장할 수 있게 한다.
- 이벤트 핸들러 실패 로그에는 `eventId`, event type, handler name, error message를 남긴다.
- `/api/v1/me/meditation-calendar?month=YYYY-MM`를 `domain.note.web`에서 실제 구현한다.
- 기존 `MyPageController`의 미구현 `meditationCalendar` 매핑은 중복 매핑을 피하도록 제거하거나 note 도메인 공개 UseCase 호출 방식으로 정리한다.
- 묵상 달력 조회는 API 명세 기준으로 `month`, `days`, `summary`를 반환한다.
- 달력 day 집계 기준은 `notes.status=SAVED`, `notes.deleted_at IS NULL`, `notes.saved_at` 날짜, 요청 회원 ID다.
- 월별 `savedDays`, `savedNoteCount`, `meditationStreakDays` 계산을 서비스 단위 테스트로 고정한다.
- OpenAPI의 `/api/v1/me/meditation-calendar` 계약이 실제 응답 DTO와 일치하는지 확인한다.
- PR 본문에는 workflow 경로와 report 경로를 남긴다.

## 제외 범위

- 자유 노트(`SERMON`, `PRAYER`, `REPENTANCE`, `GRATITUDE`)의 이벤트 발행은 제외한다.
- 묵상 이벤트로 인앱 알림을 생성하지 않는다. F-05 알림 연계가 필요하면 별도 notification workflow에서 처리한다.
- 미션 진행률 갱신 구현은 제외한다. 이번 작업은 이벤트 발행/이력과 묵상 달력 조회까지로 제한한다.
- 공유글 스냅샷, `sharing_posts.source_note_deleted_at` 반영은 sharing 도메인 작업으로 남긴다.
- 노트 로컬 캐시, 오프라인 큐잉, 충돌 해결 정책은 v1.1 이후 범위다.
- AI가 묵상 내용을 생성, 수정, 평가, 추천하는 기능은 추가하지 않는다.
- 사용자 응답, fixture, seed, OpenAPI 예시에 개역개정, ESV, NIV, 성서유니온, 두란노 본문 텍스트를 넣지 않는다.
- SSE, RAG, Vector DB, Elasticsearch, Kafka, Kubernetes, Helm, `/ai/sessions/**`를 추가하지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/api/JournalEventType.java` | `JOURNAL_CREATED`, `JOURNAL_UPDATED`, `JOURNAL_DELETED` 이벤트 타입 공개 enum |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/api/JournalChangedEvent.java` | 발행 도메인의 공개 이벤트 record. 수신 도메인은 note `internal`을 import하지 않는다. |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/JournalEvent.java` | 이벤트 이력 JPA Entity, 처리 상태, 실패 메시지, 재처리 가능 상태 |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/internal/JournalEventStatus.java` | `PENDING`, `PROCESSED`, `FAILED` 처리 상태 enum |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/internal/JournalEventRepository.java` | 이벤트 이력 저장, eventId 중복 방지, 실패 이벤트 조회 |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/internal/JournalEventHandler.java` | `@TransactionalEventListener(phase = AFTER_COMMIT)` 이벤트 처리와 실패 로그 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteService.java` | create/update/delete에서 묵상 이벤트 발행 조건 계산 |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/api/GetMeditationCalendarUseCase.java` | 묵상 달력 조회 UseCase |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/api/dto/MeditationCalendarResponse.java` | `month`, `days`, `summary` 응답 DTO |
| Modify | `qtai-server/src/main/java/com/qtai/domain/note/internal/NoteRepository.java` | 월별 묵상 달력 집계 쿼리 |
| Create | `qtai-server/src/main/java/com/qtai/domain/note/web/MeditationCalendarController.java` | `GET /api/v1/me/meditation-calendar` 엔드포인트 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/member/web/MyPageController.java` | 기존 미구현 달력 매핑 제거 또는 note UseCase 위임으로 중복 매핑 방지 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/member/web/MyPageControllerTest.java` | 미구현 5xx 기대 테스트 제거 또는 새 책임에 맞게 조정 |
| Create | `qtai-server/src/main/resources/db/migration/V13__create_journal_events.sql` | `journal_events` 테이블, `event_id` unique, `member_id`, `note_id`, `qt_passage_id`, 상태/시각/오류 컬럼 |
| Create | `qtai-server/src/test/java/com/qtai/domain/note/internal/JournalEventHandlerTest.java` | AFTER_COMMIT 핸들러, 이력 저장, 실패 로그/상태 검증 |
| Modify/Create | `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | 묵상 이벤트 발행 조건과 부정 경로 검증 |
| Create | `qtai-server/src/test/java/com/qtai/domain/note/web/MeditationCalendarControllerTest.java` | 인증, month 검증, UseCase 위임, 공통 envelope 검증 |
| Create | `qtai-server/src/test/java/com/qtai/domain/note/internal/MeditationCalendarServiceTest.java` | 월별 집계, 삭제/임시 제외, 연속일 계산 검증 |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | 묵상 달력 응답 스키마와 실패 응답 정합성 확인 |

## 구현 순서

1. `04_API_명세서.md` §4.6.2, `07_요구사항_정의서.md` §6.4/§6.13, `03_아키텍처_정의서.md` §11.4를 다시 확인한다.
2. `JournalChangedEvent`와 `JournalEventType`을 `domain.note.api`에 추가한다.
3. `JournalEvent`를 JPA Entity로 전환하고, `eventId`는 UUID 문자열 또는 `UUID` 타입으로 저장한다.
4. `journal_events` migration을 추가한다. 최소 컬럼은 `id`, `event_id`, `member_id`, `note_id`, `qt_passage_id`, `event_type`, `previous_status`, `current_status`, `status`, `occurred_at`, `processed_at`, `failed_at`, `last_error_message`, `retry_count`, `created_at`, `updated_at`이다.
5. `JournalEventRepository`를 추가하고 `eventId` unique 제약으로 중복 처리를 막는다.
6. `JournalEventHandler`는 이벤트 수신 시 `PENDING` 이력을 만들고 성공 시 `PROCESSED`로 전환한다. 실패 시 `FAILED`, `failedAt`, `lastErrorMessage`, `retryCount`를 기록한다.
7. 핸들러 실패 로그는 `eventId`, event type, handler name, error message를 한 줄에 포함한다.
8. `NoteService.create()`에서 `category=MEDITATION`이면 생성 결과에 따라 `JOURNAL_CREATED` 이벤트를 발행한다. `status=DRAFT` 생성도 이력에는 남기되 달력의 완료 표시에는 반영하지 않는다.
9. `NoteService.update()` 시작 시 이전 `category`, `qtPassageId`, `status`, `savedAt`, `deletedAt`을 보관하고, 수정 후 달력 영향 필드가 바뀌면 `JOURNAL_UPDATED` 이벤트를 발행한다.
10. `NoteService.delete()`는 삭제 전 노트가 `MEDITATION`이면 `JOURNAL_DELETED` 이벤트를 발행한다.
11. 이벤트 발행 대상은 note 도메인 공개 event record이며, 다른 도메인의 `internal`, `web`, `client` 타입을 import하지 않는다.
12. 묵상 달력 UseCase를 추가하고 `NoteService` 또는 분리된 `MeditationCalendarService`가 구현한다.
13. `MeditationCalendarController`를 `domain.note.web`에 추가하고 `/api/v1/me/meditation-calendar?month=YYYY-MM`를 제공한다.
14. `month` 파라미터는 `yyyy-MM` 형식만 허용한다. 누락 시 KST 현재 월을 사용할지, 명시 입력만 허용할지는 API 명세와 현재 Controller 관례를 확인해 결정하고 테스트로 고정한다.
15. 달력 조회는 `memberId`, 월 시작일, 다음 월 시작일을 기준으로 `savedAt` 범위를 계산한다.
16. `status=SAVED`, `deletedAt IS NULL`, `savedAt IS NOT NULL`인 노트만 `savedNoteCount`에 포함한다.
17. `meditationNoteId`는 해당 날짜의 `MEDITATION` 노트 ID를 반환하고, 없으면 `null`로 둔다.
18. `categories`는 해당 날짜에 저장된 노트 카테고리 enum 목록을 중복 없이 반환한다.
19. `savedDays`는 `savedNoteCount > 0`인 날짜 수로 계산한다.
20. `meditationStreakDays`는 요청 월 마지막 날 또는 KST 오늘 중 더 이른 날짜를 기준으로 연속 저장일 수를 역방향으로 계산한다.
21. 기존 `MyPageController`의 미구현 달력 매핑을 제거해 note 도메인 Controller와 충돌하지 않게 한다.
22. OpenAPI가 `qtai-server/apis/api-v1/openapi.yaml`에 존재하면 실제 DTO와 스키마를 맞춘다.
23. `NoteSaveResponse`는 사용하지 않고, 기존 생성/수정 응답은 `NoteCreateResponse`와 `NoteUpdateResponse` 분리 기준을 유지한다.
24. `rg`로 `domain.note`가 다른 도메인의 `internal` 또는 `web`을 import하지 않는지 확인한다.
25. 테스트/fixture/OpenAPI 예시에 금지 번역본과 실제 본문 텍스트가 들어가지 않았는지 검사한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | MEDITATION 생성 시 `JOURNAL_CREATED` 이벤트 발행 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | 자유 노트 생성 시 journal 이벤트 미발행 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | MEDITATION `DRAFT -> SAVED`, `SAVED -> DRAFT`, `qtPassageId` 변경 시 `JOURNAL_UPDATED` 발행 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | 타 사용자 수정, 미존재 노트, 삭제 노트 수정 실패 시 이벤트 미발행 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/NoteServiceTest.java` | MEDITATION 삭제 시 `JOURNAL_DELETED` 발행, 중복 삭제 호출 시 추가 이벤트 미발행 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/JournalEventHandlerTest.java` | AFTER_COMMIT 이후 `journal_events`에 eventId와 event type 저장 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/JournalEventHandlerTest.java` | 같은 eventId 재수신 시 중복 이력 생성 차단 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/JournalEventHandlerTest.java` | 핸들러 실패 시 `FAILED`, `lastErrorMessage`, `retryCount` 기록과 필수 로그 필드 출력 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/MeditationCalendarServiceTest.java` | `SAVED` 노트만 월별 완료일로 집계 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/MeditationCalendarServiceTest.java` | `DRAFT`, `DELETED`, `savedAt=null`, 타 사용자 노트 제외 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/MeditationCalendarServiceTest.java` | 하루에 여러 저장 노트가 있으면 `savedNoteCount`와 `categories`가 정확히 반환 |
| `qtai-server/src/test/java/com/qtai/domain/note/internal/MeditationCalendarServiceTest.java` | 월 경계 시작일/종료일과 `meditationStreakDays` 계산 |
| `qtai-server/src/test/java/com/qtai/domain/note/web/MeditationCalendarControllerTest.java` | 인증 사용자 기준 200 응답과 공통 envelope |
| `qtai-server/src/test/java/com/qtai/domain/note/web/MeditationCalendarControllerTest.java` | 잘못된 `month` 형식은 `400 VALIDATION_ERROR` 또는 프로젝트 공통 검증 오류로 응답 |
| `qtai-server/src/test/java/com/qtai/domain/member/web/MyPageControllerTest.java` | `MyPageController`에서 제거된 미구현 달력 테스트 정리 |
| `qtai-server/src/test/java/com/qtai/common/ArchitectureBoundaryTest.java` | note 이벤트 구현이 타 도메인 `internal`/`web`을 직접 import하지 않음 |

## 수용 기준

- [ ] `MEDITATION` 노트 생성 시 journal 이벤트가 발행된다.
- [ ] `MEDITATION` 노트 수정 중 달력 영향 필드가 바뀌면 journal 이벤트가 발행된다.
- [ ] `MEDITATION` 노트 삭제 시 journal 이벤트가 발행된다.
- [ ] 자유 노트 생성/수정/삭제는 journal 이벤트 대상이 아니다.
- [ ] 이벤트 클래스는 `domain.note.api`에 있고 수신 측은 note `internal` 타입을 직접 참조하지 않는다.
- [ ] 이벤트 후속 처리는 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 실행된다.
- [ ] 핸들러 실패 로그에는 `eventId`, event type, handler name, error message가 포함된다.
- [ ] `journal_events`는 처리 상태와 실패 메시지를 저장해 재처리 후보를 식별할 수 있다.
- [ ] Kafka, SSE, RAG, Vector DB, Elasticsearch를 추가하지 않는다.
- [ ] `/api/v1/me/meditation-calendar?month=YYYY-MM`가 실제 달력 데이터를 반환한다.
- [ ] 달력은 `notes.status=SAVED`, `notes.deleted_at IS NULL`, `notes.saved_at` 기준으로 계산한다.
- [ ] 기록 없는 날짜 조회가 빈 기록을 자동 생성하지 않는다.
- [ ] 삭제된 QT 노트는 해당 날짜 완료 표시에서 제외된다.
- [ ] `MyPageController`와 `MeditationCalendarController` 사이에 중복 URL 매핑이 없다.
- [ ] `NoteSaveResponse` 참조를 새로 만들지 않는다.
- [ ] 테스트/fixture/OpenAPI 예시에 금지 번역본과 실제 성서유니온/두란노 본문 텍스트가 없다.

## Subagent Decision

### 권장 여부

Subagent use is authorized for this workflow when the agent determines that parallel work is beneficial.

### 판단 근거

- 이벤트 발행/이력, 묵상 달력 조회, OpenAPI 정합화, 테스트 보강이 서로 다른 파일 경로로 나뉜다.
- 이벤트 record와 달력 DTO 계약을 먼저 고정하면 구현 worker와 테스트 worker가 병렬로 움직일 수 있다.
- 단, `NoteService` 이벤트 발행 조건과 달력 집계 기준은 같은 도메인 정책을 공유하므로 메인 에이전트가 최종 통합 검증을 직접 수행해야 한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| Worker 1 | Journal 이벤트 record, Entity, Repository, Handler 구현 | `qtai-server/src/main/java/com/qtai/domain/note/api/**`, `qtai-server/src/main/java/com/qtai/domain/note/internal/**`, `qtai-server/src/main/resources/db/migration/**` |
| Worker 2 | 묵상 달력 UseCase, Repository 집계, Controller 구현 | `qtai-server/src/main/java/com/qtai/domain/note/**`, `qtai-server/src/main/java/com/qtai/domain/member/web/MyPageController.java` |
| Worker 3 | 서비스/컨트롤러/핸들러 테스트 보강 | `qtai-server/src/test/java/com/qtai/domain/note/**`, `qtai-server/src/test/java/com/qtai/domain/member/web/**` |
| Worker 4 | OpenAPI와 PR 문서 정합성 확인 | `qtai-server/apis/api-v1/openapi.yaml`, `doc/workspaces/DevA_이지윤/reports/**` |

### 직접 실행 판단

메인 에이전트는 이벤트 발행 조건, 달력 집계 기준, 도메인 경계, 금지 기술/데이터 검사, 최종 Gradle 검증을 직접 확인한다.

## 검증 계획

- `git diff --check`
- `.\qtai-server\gradlew.bat test --tests "*NoteServiceTest"`
- `.\qtai-server\gradlew.bat test --tests "*JournalEventHandlerTest"`
- `.\qtai-server\gradlew.bat test --tests "*MeditationCalendar*"`
- `.\qtai-server\gradlew.bat test --tests "*MyPageControllerTest"`
- `.\qtai-server\gradlew.bat test --tests "*ArchitectureBoundaryTest"`
- `.\qtai-server\gradlew.bat test`
- `.\qtai-server\gradlew.bat build`
- `.\qtai-server\gradlew.bat jacocoTestReport`
- `.\qtai-server\gradlew.bat jacocoTestCoverageVerification`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml`
- `gitleaks detect --source . --redact --exit-code 1`
- `rg -n "com\\.qtai\\.domain\\.(member|bible|qt|sharing|mission|notification)\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/note --glob "*.java"`
- `rg -n "KafkaTemplate|spring-kafka|@KafkaListener|SseEmitter|text/event-stream|/ai/sessions|VectorStore|EmbeddingStore|Elasticsearch|RAG" qtai-server/src/main qtai-server/src/test`
- `rg -n "개역개정|ESV|NIV|성서유니온|두란노|plain secret|private key" qtai-server/src/main qtai-server/src/test qtai-server/apis/api-v1/openapi.yaml`

`jacocoTestReport` 또는 `jacocoTestCoverageVerification` task가 현재 Gradle 프로젝트에 없으면 실행 실패 사유를 report와 최종 응답에 기록한다. `.spectral.yaml`이 없으면 `--ruleset .spectral.yaml` 없이 OpenAPI lint를 실행하고, `spectral` 또는 `gitleaks`가 로컬에 없으면 미설치 사유를 남긴 뒤 CI 검증 대상으로 표시한다.

## 후속 작업으로 남길 항목

- 미션 진행률 갱신 UseCase와 journal 이벤트 연동
- 인앱 알림 생성이 필요한 사용자 이벤트 정책 확정
- sharing 도메인의 원본 노트 삭제 후 `source_note_deleted_at` 반영
- 대시보드 통계 위젯을 note 도메인 집계 UseCase로 연결
- 실패한 journal 이벤트 재처리 관리자 또는 시스템 배치 API
