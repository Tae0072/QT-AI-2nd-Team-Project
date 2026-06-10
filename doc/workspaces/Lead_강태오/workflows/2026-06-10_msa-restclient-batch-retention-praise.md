# 2026-06-10 MSA 배치 RestClient ②-b service-user retention → praise purge 워크플로우

> 작업 브랜치: `feature/msa-restclient-batch` (origin/dev-msa 기준)
> 선행: ②-a(note/sharing/report purge, PR #445). 동일 패턴, 수신 서비스만 service-bible.

## 목표

보존기간 만료 회원 정리 배치의 cross-service purge 마지막 1종 **praise(service-bible)**를 Mock에서 RestClient 어댑터로 교체한다.

## TODO

- [x] 수신 컨트롤러(service-bible) — `praise/web/PraisePurgeController` `POST /api/v1/praise-songs/purge`, `@PreAuthorize("hasRole('SYSTEM_BATCH')")`
- [x] 어댑터(service-user) — `member/client/praise/PurgeMemberPraiseDataRestClientAdapter`(시스템 토큰, base=**bibleBaseUrl**)
- [x] `PurgeMemberPraiseDataUseCaseMock` 삭제 (`ListMemberPraiseSongUseCaseMock`은 purge와 무관해 유지)
- [x] 테스트 — 어댑터 단위 3(정상·5xx·토큰미설정) + 수신 MockMvc 3(SYSTEM_BATCH 200·사용자 403·미인증 401)
- [x] 빌드 `:service-user:build` + `:service-bible:build` GREEN
- [x] 문서
- [ ] dev-msa 정합 → 커밋·푸시·PR → 리뷰

## 설계 결정

- ②-a와 동일 패턴. 차이: praise는 service-bible 소속이라 base URL이 `bibleBaseUrl`이고, 수신 컨트롤러가 service-bible에 들어간다(service-bible은 entry point가 있어 미인증이 401로 명확).
- 수신 purge 로직(`PraisePurgeService`, `@Transactional`)은 이미 존재 → HTTP 노출만 신설.
- 이로써 retention 배치의 4개 cross-service purge(note/sharing/report/praise)가 모두 실제 RestClient 어댑터로 전환된다. deploy guard 활성화(`qtai.retention.purge.enabled`)는 여전히 user→admin VerifyAdminRole 통합 + 시스템 시크릿 env 후 배포 세션에서.

## 검증

`:service-user:build` + `:service-bible:build` **BUILD SUCCESSFUL**.

## 다음

PR③ ai→audit/admin(admin-server) — `WriteAuditLogUseCase`·`VerifyAdminRoleUseCase`.
