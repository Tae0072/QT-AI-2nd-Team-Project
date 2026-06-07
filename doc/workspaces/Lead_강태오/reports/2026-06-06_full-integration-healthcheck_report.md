# 2026-06-06 전체 통합 건강검진 + MSA 분리 준비도 재평가

목적: 지금까지의 모든 수정 브랜치가 (1) 서로 충돌 없이 합쳐지는지, (2) 합친 상태가 빌드·테스트 그린인지, (3) 도메인 경계가 유지되는지, (4) MSA 물리 분리에 들어가도 될 수준인지 검증.

## 0. 검증 방법
- `origin/dev`에서 임시 통합 브랜치(`integration/verify-all`)를 만들어 **모든 수정 브랜치 32개를 순서대로 머지**(P0→P1→P2)하고 충돌·빌드를 관측.
- 빌드/테스트: Windows JDK21 + `gradlew --no-daemon test`(전체 스위트, H2). 각 브랜치는 개발 시점에 이미 개별 그린 확인됨.
- 제외: `chore/infra-quality-gates-add`(2026-05-18 기반 **스테일 CI 브랜치** — dev보다 ~3주 뒤처짐, 머지 전 **리베이스 필요**), docs/pr/master.

## 1. 핵심 결과(요약)
- **32개 중 28개 브랜치가 자동 머지(충돌 0) + 통합 상태 전체 스위트 BUILD SUCCESSFUL(그린).** 대다수 수정이 함께 깨끗이 합쳐짐을 실측 확인.
- **충돌 4건** — 모두 "여러 브랜치가 같은 파일의 같은 영역을 병렬 수정"한 정상적 머지 충돌(버그 아님). 머지 시점에 표준 해소 필요.
- **마이그레이션 V24 중복(하드 블로커) 발견 → 해소함**: `fix/ai-worker-stabilization`의 `V24__seed_ai_prompt_versions.sql`와 `fix/journal-events-outbox`의 `V24__add_journal_events_outbox_columns.sql`가 **둘 다 V24**. 파일명이 달라 git은 충돌로 안 잡지만 **Flyway가 중복 버전24를 거부**한다. → journal 쪽을 **V25로 리넘버링**(해당 브랜치 커밋 amend 완료). 이제 V24(seed)+V25(outbox)로 충돌 없음.

> 부수 발견: "ai_prompt_versions 초기 시드 없음"(리뷰 P2)은 이미 **P1-3(`fix/ai-worker-stabilization`)에서 V24 시드로 처리**돼 있었음.

## 2. 충돌 4건 상세(머지 해소 가이드)

| 충돌 브랜치 | 충돌 파일 | 원인(같이 건드린 브랜치) | 해소 방향 |
|------------|-----------|--------------------------|-----------|
| `feature/notification-wiring` | `sharing/internal/CommentService.java` | `fix/sharing-atomic-counters`(원자 카운터) | 양쪽 변경 결합 — 원자 UPDATE 카운터 + 댓글 알림 호출 둘 다 유지 |
| `fix/journal-events-outbox` | `note/internal/NoteService.java`(+Test) | `feature/sharing-source-note-deleted`(노트 삭제→나눔 통지) | NoteService 생성자에 `markSourceNoteDeletedUseCase`와 아웃박스(`journalOutbox`) **둘 다** 주입, 삭제 경로에 통지+아웃박스 적재 모두 유지 |
| `refactor/qt-bible-sql-boundary` | `qt/internal/QtPassageRepository.java`, `QtTodayPassageImportService.java`(+Test) | qt P0 4종(seed/verse-mapping/availability/publication-gate) | qt P0 머지 후, native SQL 2건 제거 + `BibleBookLookup` 경유로 재적용 |
| `fix/member-nickname-trim` | `member/internal/MemberService.java`(+Test) | `fix/member-temp-nickname-length` 등 | trim 일원화(changeNicknameInternal)와 임시 닉네임 길이 수정 결합 |

**권장 머지 순서(충돌 최소화):** 같은 도메인은 개발 순서대로 — qt(P0-2→3→8→4) → `refactor/qt-bible-sql-boundary`; sharing(P1-2 `atomic-counters` → P1-13 `notification-wiring` → P0-5 `comment-withdrawn` → P0-6 `source-note-deleted`); note(P0-6 → P1-10 `journal-events-outbox`); member(P1-4 `temp-nickname-length` → P2 `nickname-trim`). 나머지(28개)는 순서 자유.

## 3. 빌드·경계 게이트
- 통합(28브랜치) 전체 스위트 **BUILD SUCCESSFUL** — 컴파일·단위·슬라이스·**ArchUnit 도메인 경계(`DomainBoundaryArchTest`)**·Spring 컨텍스트 로드 모두 통과.
- 도메인 경계: 타 도메인 `internal` 직접 import 0건 유지(ArchUnit 가드). qt→bible 호출도 `refactor/qt-bible-sql-boundary` 머지 후 api 포트 경유로 일원화.
- `MysqlMigrationValidationTest`(Docker 필요)는 로컬 자동 skip — V24/V25 정합은 위 리넘버링으로 보장. CI(Docker)에서 최종 가드 권장.

## 4. MSA 분리 준비도 재평가 (리뷰 §5.2 블로커 현황)

| # | 블로커 | 현황 | 비고 |
|---|--------|------|------|
| 1 | **SQL 레벨 경계 침범**(qt→bible_books native JOIN) — "즉시 깨짐" 최우선 | **해소(브랜치 완료, 충돌 해소 후 머지 대기)** | `refactor/qt-bible-sql-boundary` + `BibleBookLookup` |
| 2 | 크로스도메인 단일 트랜잭션 3곳(member purge / ai승인+study publish / ai검증 LLM) | **부분** — purge는 도메인별 `Purge*UseCase` 포트로 위임(경계 개선)하나 여전히 in-process 오케스트레이션. 분리 시 **saga 필요** | 잔존 |
| 3 | 이벤트 인프라 토대 부재(journal_events 비-outbox·재처리기·previous값 없음) | **해소(브랜치 완료, 머지 대기)** | `fix/journal-events-outbox`(아웃박스+재처리기+previousQtPassageId). 단 `MemberWithdrawnEvent`는 여전히 in-memory ApplicationEvent → 브로커/아웃박스화 잔존 |
| 4 | 동기 호출 위험(sharing→member 댓글 N+1 등) | **부분 해소** — N+1은 P0-5에서 벌크 조회로 제거. audit 횡단 동기 호출은 잔존 | |
| 5 | 물리 FK 맵(도메인 간 FK) | 잔존(분리 시 드랍+앱검증 전환 — 설계상 정상) | |
| 6 | 공유 식별자 고착(bible_verses.id 공용, aiAssetId 노출) | **부분 해소** — 사용자 응답 내부 PK(aiAssetId) 노출 제거. bible_verses.id 공용키는 잔존 | |
| 7 | 로컬 파일시스템 의존(AI 검증 인덱스 `./restricted`) | 잔존 | 오브젝트 스토리지 추상화 필요 |
| 8 | 로컬 캐시(Caffeine todayQt/bibleBooks) | 잔존(분리/이중화 시 Redis 전환) | bibleBooks 캐시는 이번에 실사용 연결 |
| 9 | 시간 정책 SSOT(00:00/04:00 게이트 단일 위치) | **부분 해소** — P0-8로 다중 진입점에 게이트 적용 | |
| 10 | Spring Modulith 미도입 | 잔존(ArchUnit만으로 import 경계 가드) | 분리 리허설 도구로 도입 권장 |

### 판정
- **코드 레벨 경계는 분리 친화적**이며, 이번 작업으로 **최우선 SQL 경계 침범 제거 + 이벤트 아웃박스 토대 + N+1·내부PK 노출 제거**까지 더해져 준비도가 **확실히 상승**했다.
- 그러나 **지금 바로 물리 분리는 시기상조**. 분리 진입 전 최소 다음을 단계적으로 처리해야 한다:
  1. member purge **saga화**(이벤트 + 멱등 삭제 + 완료추적)와 ai 승인↔study publish **보상 흐름**.
  2. `MemberWithdrawnEvent` 등 in-memory 이벤트의 **DB outbox + 폴링 릴레이(또는 Redis Stream)** 전환(journal 아웃박스 패턴을 표준화).
  3. 공유 DB/물리 FK 드랍 계획 + 공유 식별자(bible_verses.id) 동결/자연키 병기 결정.
  4. 로컬 파일/캐시의 오브젝트 스토리지·Redis 추상화.
  5. **Spring Modulith 도입**(`ApplicationModules.verify()`)으로 모듈 경계 리허설.
- 요약: **"코드는 분리 준비 OK, 인프라·트랜잭션 경계는 1~2개 마일스톤의 선행 작업 필요."**

## 5. 잔여 위험 / 액션
- **머지 전 필수**: ① 위 충돌 4건 표준 해소(권장 순서 준수), ② `chore/infra-quality-gates-add` 리베이스 후 CI 게이트(gitleaks/spectral) 반영, ③ Docker 환경에서 `MysqlMigrationValidationTest`로 V1~V25 Flyway+validate 최종 확인.
- **마이그레이션 번호**: 현재 dev 최신=V23. 머지 후 V24(ai 프롬프트 시드)·V25(journal 아웃박스). 추가 마이그레이션 브랜치 생길 때 번호 재점검.
- **남은 P2**(미착수): 결정 필요(ai 검증흐름·audit 범위/PII/purge감사·STALE 캐싱·Jackson·스케줄러), 콘텐츠·마이그레이션(created_at 타임존·purge 인덱스·CASCADE 테스트), Flutter 6건, 문서 drift — 별도 리포트 `2026-06-06_p2-backend-round2_report.md` 참조.

## 6. 검증 산출물 정리
- 임시 통합 브랜치 `integration/verify-all`은 **검증 전용(throwaway)** — 실제 머지는 사용자가 개별 PR로 수행. 필요 시 삭제 가능.
- 모든 수정 브랜치는 push·PR 미수행 상태(합의대로 사용자(T)가 직접 수행).
