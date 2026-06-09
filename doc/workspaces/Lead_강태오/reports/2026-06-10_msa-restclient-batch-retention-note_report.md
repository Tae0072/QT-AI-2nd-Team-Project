# 리포트 — MSA 배치 RestClient ②-a service-user retention → note/sharing/report purge

작성일: 2026-06-10 / 작성: Claude (Lead 강태오) / 브랜치: `feature/msa-restclient-batch`

## 1. 요약

보존기간 만료 회원 정리 배치의 cross-service purge 중 service-note 소관 3종(note/sharing/report)을 Mock에서
RestClient 어댑터로 교체했다. 수신 purge 로직은 이미 존재(NotePurge/SharingPurge/ReportPurgeService)해 HTTP
노출만 신설했고, 회원 데이터 hard delete라 SYSTEM_BATCH 전용으로 보호했다.

## 2. 변경 내역

### 신규 (service-note 수신, `@PreAuthorize("hasRole('SYSTEM_BATCH')")`)
- `note/web/NotePurgeController` — `POST /api/v1/notes/purge?memberId=`
- `sharing/web/SharingPurgeController` — `POST /api/v1/sharing/purge?memberId=`
- `report/web/ReportPurgeController` — `POST /api/v1/reports/purge?memberId=`

### 신규 (service-user 호출)
- `member/client/note|sharing|report/PurgeMember{Note,Sharing,Report}DataRestClientAdapter` — 시스템 토큰 발급, base=noteBaseUrl, 삭제 행 수(int) 반환.

### 삭제 (service-user)
- `PurgeMemberNoteDataUseCaseMock`, `PurgeMemberSharingDataUseCaseMock`, `PurgeMemberReportDataUseCaseMock`.

### 테스트
- service-user: 어댑터 단위(note 3: 정상·5xx·토큰미설정 / sharing 1 / report 1).
- service-note: `MemberDataPurgeApiTest` 3(SYSTEM_BATCH 200·사용자 403·미인증 401/403).

## 3. 설계 결정

| 결정 | 근거 |
|---|---|
| 수신 SYSTEM_BATCH 전용 | 회원 데이터 hard delete → 사용자 노출 금지. |
| 도메인별 컨트롤러 | 자기 도메인 api만 의존 → DomainBoundaryTest 충족. |
| Mock 삭제 + 어댑터 추가만 | 오케스트레이터가 인터페이스 주입 → 로직·순서·가드 불변. |
| 분산 트랜잭션 미사용 수용 | 회의록 §3(Saga 없음). 수신 자체 tx 독립 커밋, 멱등 재실행, 회원 단위 실패 격리. |

## 4. 검증

`:service-user:build` + `:service-note:build` **BUILD SUCCESSFUL**.

## 5. 리스크 & 후속

- **deploy guard 활성화 선행조건**: `qtai.retention.purge.enabled=true`는 service-user→admin `VerifyAdminRoleUseCase` 실제 통합(현재 Mock "관리자 아님" 고정) + `SECURITY_JWT_SYSTEM_SECRET` env 후에 배포 세션에서 켠다. 지금 켜면 관리자 오삭제 위험(가드의 존재 이유).
- 후속: ②-b user→praise(service-bible), PR③ ai→audit/admin.
