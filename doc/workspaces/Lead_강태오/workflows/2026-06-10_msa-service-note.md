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
- [ ] **Day2-4-2 note 이전** — api/internal/web. bible Mock, sharing in-svc(순서상 sharing 먼저/동시). JournalEvent 핸들러 실패 로그·재처리(eventId·type·handler·error).
- [ ] **Day2-4-3 sharing 이전** — member·notification Mock.
- [ ] **Day2-4-4 report(제출) 이전** — notification·audit·admin Mock. 검수 CRUD는 admin-server 소관(여기 미포함).
- [ ] **Day2-4-5 테스트 + PR** — Controller MockMvc(verseId 쿼리·미인증 401), 도메인 단위, ArchUnit(타 도메인 internal import 금지 — PR#2 개선 패턴 차용). base dev-msa.

## 핵심 원칙 (PR#1 교훈)

- 브랜치 `feature/` prefix. commit/push 분리. `gradlew --stop` 금지(타 세션 빌드 보호).
- 첫 푸시부터 APPROVE 품질: MockMvc·단위·ArchUnit·표준 페이징 envelope·권한 헬퍼·광범위 catch 금지·로그 민감정보 금지.
- verseId는 화면이 쿼리파라미터로 전달(서비스 간 조회 호출 없음). 이벤트 실패는 "재처리 가능 상태" 표현.

## 진행 메모
(작업하며 갱신)
