# 2026-06-10 MSA Day2 — service-note 추출 워크플로우

> 작업 폴더(worktree): `D:\workspace\QT-AI-note` (브랜치 `feature/msa-note-service`, dev-msa=2bac476 기준)
> 병행: PR#2(qt·study)=`QT-AI-2nd-Team-Project`, service-user(member)=`QT-AI-day2`. 세 폴더 분리 worktree라 충돌 없음(`gradlew --stop` 금지).
> 기준: `2026-06-09_msa-restart-plan.md` §4 Day2.

## 대상 (사전 점검)

| 도메인 | 파일 | cross-domain | 처리 |
|--------|------|--------------|------|
| note(45) | bible(2)·sharing(1) | bible→Mock(다른 서비스), sharing→in-svc | JournalEvent + Reprocessor(이벤트 재처리), BaseEntity |
| sharing(38) | member(4)·notification(4)·note(3) | member·notification→Mock, note→in-svc | SharingPost·Comment |
| report(20) | sharing(2)·notification(2)·audit(2)·admin(1) | sharing→in-svc, notification·audit·admin→Mock | 제출분만(검수는 admin-server) |

→ cross-domain Mock 5종: **bible·member·notification·audit·admin** (`client/{도메인}/...UseCaseMock`, 통합 시 RestClient).

## TODO

- [x] **Day2-4-1 service-note 스켈레톤 + 빌드** — settings include + boot app(web+jpa) + 스모크. `:service-note:build` 통과 (`5e1019b`)
- [x] **Day2-4-2 note 이전** — api/internal/web(45파일). bible→client/bible Mock, qt→기존 client/qt 포트(NoteQtClient) 유지, sharing→in-svc. JournalEvent 아웃박스 + Reprocessor 이전(실패 시 eventId·type·handler·error 로그 + FAILED·백오프·retryCount = 재처리 가능 상태).
- [x] **Day2-4-3 sharing 이전** — api/internal/web(38파일). member→client/member Mock, notification→client/notification Mock(no-op·민감정보 미로깅), note→in-svc.
- [x] **Day2-4-4 report(제출) 이전** — 제출분 12파일. 검수 8파일(AdminReport*·ListAdminReports·ProcessReport·admin DTO) 미포함, ReportRepository.findForAdmin·Report.process()/isClosed() 제거. notification/audit/admin Mock은 검수 전용이라 불필요(제출은 sharing in-svc만 의존).
- [x] **Day2-4-5 테스트 + PR** — DomainBoundaryTest(타 도메인 internal import 금지 ArchCondition, sharing→note.api·report→sharing.api 같은 합법 in-svc 의존 허용), MockMvc 통합(미인증 401/403·인증 200/201·qtPassageId 쿼리·/admin denyAll 403), 도메인 단위(NoteService·ReportService·JournalEventReprocessor). 총 19개 통과. base dev-msa.

## 핵심 원칙 (PR#1 교훈)

- 브랜치 `feature/` prefix. commit/push 분리. `gradlew --stop` 금지(타 세션 빌드 보호).
- 첫 푸시부터 APPROVE 품질: MockMvc·단위·ArchUnit·표준 페이징 envelope·권한 헬퍼·광범위 catch 금지·로그 민감정보 금지.
- verseId는 화면이 쿼리파라미터로 전달(서비스 간 조회 호출 없음). 이벤트 실패는 "재처리 가능 상태" 표현.

## 진행 메모

- **실측 cross-service 의존(문서 추정과 차이)**: note→bible(GetBibleVerseUseCase+3 DTO)뿐(qt는 NoteQtClient 포트로 이미 추상화). sharing→member(GetMemberUseCase)+notification(SendNotificationUseCase) FQN 의존(import 아님 — grep 시 주의). report 제출→sharing.api.GetSharingPostUseCase(in-svc)만. ⇒ 외부 계약 복사 3종(bible·member·notification), Mock 3종(note/client/bible, sharing/client/member, sharing/client/notification).
- **Strangler 유지**: 모놀리식 `qtai-server/src`는 손대지 않고 도메인을 service-note로 복사 후 외부 의존만 Mock으로 치환. `git status -- qtai-server/src` 비어 있음 확인.
- **표준 페이징 envelope**: 기존 도메인 DTO(NoteListResponse 등)가 이미 page/size/totalElements 필드를 포함 — raw Page<T> 노출 없음(추가 변환 불필요).
- **검증**: `:service-note:build` BUILD SUCCESSFUL(2회). 미인증 차단 상태는 401/403 모두 허용으로 단정(엔트리포인트 기본동작 비결정 회피). `@EnableScheduling` 추가(Reprocessor 폴링 활성).
- **DomainBoundaryTest 패턴**: service-bible의 slices.notDependOnEachOther는 in-svc 합법 의존(sharing→note)이 있어 부적합 → 커스텀 ArchCondition으로 "타 도메인 .internal 패키지 직접 의존"만 금지(api/api.dto는 허용). PR#2와 동일 취지.
