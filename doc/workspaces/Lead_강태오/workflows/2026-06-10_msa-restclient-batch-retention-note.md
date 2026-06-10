# 2026-06-10 MSA 배치 RestClient ②-a service-user retention → note/sharing/report purge 워크플로우

> 작업 브랜치: `feature/msa-restclient-batch` (origin/dev-msa 기준, worktree `D:\workspace\QT-AI-batch-rc`)
> 선행: PR #441/#443/#444(ai→bible/qt/study). 동일 시스템 토큰 패턴.

## 목표

service-user의 보존기간(2년) 만료 회원 정리 배치(`MemberRetentionPurgeService`)가 호출하는 cross-service
purge Mock 중 **service-note 소관 3종(note/sharing/report)**을 RestClient 어댑터로 교체한다. (praise=service-bible은 ②-b.)

## 분할 결정

상위 ②(user retention → note/praise/report/sharing)는 수신 서비스가 두 개다: note/sharing/report=service-note,
praise=service-bible. **수신 서비스 기준으로 ②-a(service-note 3종)·②-b(service-bible praise)로 분할**해 PR당 서비스 2개로 제한(첫 푸시 체크리스트 크기).

## 핵심 사실

- 수신 purge **로직은 이미 존재**: service-note `NotePurgeService`/`SharingPurgeService`/`ReportPurgeService`(각 `@Transactional`, `int purgeByMemberId(Long)`). **HTTP 노출만 신설**.
- 오케스트레이터 `MemberRetentionPurgeService`는 인터페이스로 주입받으므로 **Mock 삭제 + 어댑터 추가만으로 자동 교체**(로직 불변, 호출 순서/배치 가드 그대로).
- **MSA엔 분산 트랜잭션 없음**(회의록 §3): 모놀리식의 "회원 1명=1트랜잭션"이 깨진다. 각 purge는 수신 서비스 자체 트랜잭션에서 독립 커밋. 멱등이라 부분 실패 시 다음 배치 실행에서 잔여분 삭제(오케스트레이터가 회원 단위 실패 기록 후 계속).

## TODO

- [x] 수신 컨트롤러 3종(service-note) — `note/web/NotePurgeController`(`POST /api/v1/notes/purge`), `sharing/web/SharingPurgeController`(`/api/v1/sharing/purge`), `report/web/ReportPurgeController`(`/api/v1/reports/purge`), 전부 `@PreAuthorize("hasRole('SYSTEM_BATCH')")`
- [x] 어댑터 3종(service-user) — `member/client/{note,sharing,report}/Purge...RestClientAdapter`(시스템 토큰, base=noteBaseUrl)
- [x] Mock 3종 삭제
- [x] 테스트 — 어댑터 단위(note 3·sharing 1·report 1) + 수신 MockMvc 3(SYSTEM_BATCH 200·사용자 403·미인증 401/403)
- [x] 빌드 `:service-user:build` + `:service-note:build` GREEN
- [x] 문서
- [ ] dev-msa 정합 → 커밋·푸시·PR → 리뷰

## 설계 결정·근거

- **수신 SYSTEM_BATCH 전용**: 회원 데이터 hard delete라 사용자 경로 노출 금지 → 메서드 보안으로 배치만 허용.
- **도메인별 컨트롤러**: 각 컨트롤러가 자기 도메인 api(Purge*UseCase)만 의존 → DomainBoundaryTest(타 도메인 internal 금지) 충족.
- **시스템 토큰·ObjectProvider·@Autowired·RestClientException-only**: PR #441~#444 동일.
- **deploy guard 활성화는 별도 선행조건**: `qtai.retention.purge.enabled`(기본 false)를 켜려면 `hasAdminLink`가 실제로 동작해야 하는데, 이는 service-user→admin `VerifyAdminRoleUseCase`가 아직 Mock("관리자 아님" 고정)이라 불가. 지금 켜면 관리자 회원 오삭제 위험. **본 PR은 4개(②-b 포함) purge 경로를 실제 어댑터로 만드는 단계**이고, 가드 활성화(env)는 user→admin 검증 통합 + `SECURITY_JWT_SYSTEM_SECRET` 주입 후 배포 세션에서 처리한다.

## 검증 결과

`:service-user:build` + `:service-note:build` **BUILD SUCCESSFUL**. 테스트 함정: 수신 MockMvc는 `ddl-auto=create-drop` 명시 필요(service-note 관례), 미인증은 service-note가 별도 entry point 미설정이라 401/403 모두 가능 → `isIn(401,403)`로 검증(기존 테스트 관례).

## 다음

②-b user→praise(service-bible), 이후 PR③ ai→audit/admin.
