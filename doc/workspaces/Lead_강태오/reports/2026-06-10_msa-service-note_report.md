# MSA Day2 — service-note(note·sharing·report 제출) 추출 리포트 (2026-06-10)

> 작업 폴더(worktree): `D:\workspace\QT-AI-note` · 브랜치 `feature/msa-note-service` · base `dev-msa`(=2bac476)
> 연계: 워크플로우 `workflows/2026-06-10_msa-service-note.md`, 스터디노트 `study-notes/2026-06-10_msa-strangler-domain-extraction.md`

## 1. 목표

읽기전용 콘텐츠 서비스(service-bible) 파일럿에 이어, 노트/나눔/신고(제출) 도메인을 `service-note` 모듈(port 8083)로 Strangler 추출한다. 모놀리식 원본(`qtai-server/src`)은 그대로 두고 도메인을 복사·이전하며, 타 서비스 의존은 api 계약 타입만 가져와 client 어댑터(Mock)로 임시 구현한다(통합 시 RestClient 교체).

## 2. 한 일

### 2.1 도메인 이전 (Strangler, 모놀리식 원본 유지)

| 도메인 | 파일 | 외부(타 서비스) 의존 → 처리 | in-service 의존 |
|--------|------|------------------------------|------------------|
| note | 45 | bible `GetBibleVerseUseCase`(+3 DTO) → `note/client/bible` Mock · qt → 기존 `note/client/qt` 포트(NoteQtClient) | sharing `MarkSourceNoteDeletedUseCase` |
| sharing | 38 | member `GetMemberUseCase`(+2 DTO) → `sharing/client/member` Mock · notification `SendNotificationUseCase`(+1 DTO) → `sharing/client/notification` Mock | note `GetNoteUseCase` 등 |
| report(제출) | 12 | 없음(검수 전용 의존 제외) | sharing `GetSharingPostUseCase` |

- 외부 계약 9파일 복사: `bible.api`(GetBibleVerseUseCase, BibleVerseResponse/RangeResponse/BookResponse), `member.api`(GetMemberUseCase, MemberResponse/MemberPublicResponse), `notification.api`(SendNotificationUseCase, NotificationSendRequest).
- Mock 3종: 통합 전 임시 구현. notification Mock은 no-op이며 제목/본문 등 사용자 콘텐츠를 로그에 남기지 않는다(CLAUDE.md §9). bible Mock은 본문 텍스트를 채우지 않는다(null) — 저작권 리스크 회피(§8).

### 2.2 report "제출분만" — 검수 제외

admin-server 소관인 신고 검수 8파일을 제외했다: `AdminReportController`, `AdminReportService`, `ListAdminReportsUseCase`, `ProcessReportUseCase`, `AdminReportListQuery/Response`, `ProcessReportCommand/Result`. 부수적으로 `ReportRepository.findForAdmin`(검수 목록 쿼리)과 `Report.process()/isClosed()`(검수 상태 전이)도 제거. 처리 결과 컬럼(processedByAdminId/processedAt/status)은 reports 테이블 스키마로 유지(단일 DB에서 admin-server가 기록).

### 2.3 공통 인프라

- `NoteServiceApplication`: `scanBasePackages=com.qtai` + `@EntityScan/@EnableJpaRepositories(com.qtai.domain)` + `@EnableCaching` + `@EnableScheduling`(Reprocessor 폴링).
- `SecurityConfig`: STATELESS + `@EnableMethodSecurity`, `/actuator/health|info` permitAll, **`/api/v1/admin/**` denyAll**(검수는 admin-server), 그 외 authenticated. JWT 필터는 lib-common의 `@ConditionalOnProperty` 빈을 ObjectProvider로 선택 적용.
- `JpaAuditingConfig`: 공통 `Clock`(Asia/Seoul) 기반 `DateTimeProvider`.
- DB: 로컬/테스트 H2(MySQL 모드), 운영 MySQL(env). ddl-auto 기본 validate, 테스트는 create-drop 오버라이드.

### 2.4 JournalEvent 트랜잭션 아웃박스 + 재처리기

note 변경(묵상 노트 생성/수정/삭제)을 호출자 트랜잭션과 원자적으로 `journal_events`(PENDING)에 적재(`JournalEventOutbox`)하고, `JournalEventReprocessor`가 `@Scheduled` 폴링으로 처리한다. 전달 실패 시 **FAILED + 지수 백오프(nextAttemptAt) + retryCount 증가**로 남겨 "재처리 가능 상태"를 보존하고, 실패 로그에 eventId·eventType·handlerName·errorMessage를 남긴다(CLAUDE.md §9, §10).

## 3. 테스트 (총 42개, 실패·스킵 0)

| 클래스 | 수 | 내용 |
|--------|----|------|
| `DomainBoundaryTest` | 1 | ArchUnit 커스텀 ArchCondition — 타 도메인 `.internal` 직접 의존 금지(api/api.dto 허용). sharing→note.api, report→sharing.api 같은 합법 in-svc 의존 통과 |
| `NoteApiSecurityIntegrationTest` | 7 | MockMvc 통합 — 미인증 401/403, 인증 노트목록 200, qtPassageId 쿼리파라미터 200, 노트생성 201, 신고접수 201, /api/v1/admin/** denyAll 403 |
| `NoteServiceTest` | 4 | 카테고리별 입력검증(QT_PASSAGE_REQUIRED/FORBIDDEN, VERSE_REQUIRED, NOT_FOUND) |
| `SharingPostServiceTest` | 10 | 공개 동의 가드, 좋아요(없는 글/중복/성공·동기화), 좋아요취소, 삭제(소유권/멱등/DELETED 전이), 숨김·되돌리기, 내목록 잘못된 status |
| `CommentServiceTest` | 6 | 없는 글/댓글 OFF 차단, 작성 시 카운트 동기화, 삭제 소유권, **탈퇴 회원 닉네임 폴백(회귀)** |
| `MeditationCalendarServiceTest` | 4 | **streak 계산(P1-9 회귀)** — 오늘까지 연속/오늘 미저장 시 어제 anchor/중간 공백 끊김/이력 없음 |
| `PurgeServiceTest` | 3 | report·note·sharing 정리 — 자기 테이블만, 순서·합계 검증(JdbcTemplate mock) |
| `ReportServiceTest` | 4 | 대상타입 검증, 중복 신고, POST 대상 미존재→TARGET_NOT_FOUND, 정상 접수 RECEIVED |
| `JournalEventReprocessorTest` | 2 | 핸들러 실패→FAILED·재처리 가능 상태 보존 / 성공→PROCESSED |
| `NoteServiceApplicationTest` | 1 | 컨텍스트 로드 스모크 |

### 3.1 자동 리뷰(claude-review) 대응 — REQUEST_CHANGES 2건 해소
- **BLOCK1 시간 정책(Clock 우회)**: `PostLike.prePersist()`의 `LocalDateTime.now()` 제거 → 팩토리에서 Clock 기반 createdAt 주입. `SharingPostService.delete()/hide()/like()`도 주입 Clock(`now(clock)`) 사용. service-note 전체 무인자 `now()` 0건.
- **BLOCK2 테스트 누락**: 핵심 미검증 서비스(SharingPostService·CommentService·MeditationCalendarService·Purge 3종) 단위테스트 추가(19→42개).
- WARN: `NoteService`의 sharing UseCase FQN 참조 → import로 정리.

## 4. 검증

- `:service-note:compileJava` / `:service-note:build` BUILD SUCCESSFUL(2회 — Report 검수 메서드 제거 전·후).
- 광범위 `catch (Exception/Throwable)` 0건, raw `Page<T>` 컨트롤러 반환 0건(표준 envelope DTO 사용), 모놀리식 `src` 미변경(`git status -- qtai-server/src` 비어 있음).
- 빌드 산출물(build/, build-note.*)은 스테이징 제외.

## 5. 설계 메모 / 다음 단계

- verseId는 화면이 쿼리파라미터로 전달 — 서비스 간 구절 조회 호출을 만들지 않는다(bible Mock은 로컬 스텁, qt는 NoteQtClient 포트).
- Day3 통합: client Mock 3종(bible/member/notification)을 RestClient 어댑터로 교체. note의 sharing in-svc 의존(MarkSourceNoteDeleted)과 report의 sharing in-svc 의존(GetSharingPost)은 같은 서비스라 유지.
- 신고 검수 CRUD는 admin-server(8090)에서 reports 테이블을 대상으로 구현 예정.
