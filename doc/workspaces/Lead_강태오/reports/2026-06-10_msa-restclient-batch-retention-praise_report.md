# 리포트 — MSA 배치 RestClient ②-b service-user retention → praise purge

작성일: 2026-06-10 / 작성: Claude (Lead 강태오) / 브랜치: `feature/msa-restclient-batch`

## 1. 요약

보존기간 만료 회원 정리 배치의 cross-service purge 마지막 1종 praise(service-bible)를 Mock에서 RestClient 어댑터로
교체했다. ②-a(note/sharing/report)와 동일 패턴이며 수신만 service-bible. 이로써 retention 4개 purge가 모두 실제 어댑터로 전환됐다.

## 2. 변경 내역

- 신규 (service-bible): `praise/web/PraisePurgeController` `POST /api/v1/praise-songs/purge` `@PreAuthorize("hasRole('SYSTEM_BATCH')")`.
- 신규 (service-user): `member/client/praise/PurgeMemberPraiseDataRestClientAdapter` (시스템 토큰, base=bibleBaseUrl).
- 삭제 (service-user): `PurgeMemberPraiseDataUseCaseMock`.
- 테스트: service-user 어댑터 3(정상·5xx·토큰미설정), service-bible `PraisePurgeApiTest` 3(SYSTEM_BATCH 200·사용자 403·미인증 401).

## 3. 설계 결정

| 결정 | 근거 |
|---|---|
| 수신 SYSTEM_BATCH 전용 | 회원 찬양 저장 hard delete → 사용자 노출 금지. |
| base=bibleBaseUrl | praise는 service-bible(8082) 소속. |
| Mock 삭제 + 어댑터 추가만 | 오케스트레이터 인터페이스 주입 → 로직·순서·가드 불변. |

## 4. 검증

`:service-user:build` + `:service-bible:build` **BUILD SUCCESSFUL**.

## 5. 리스크 & 후속

- deploy guard 활성화는 ②-a와 동일(user→admin VerifyAdminRole 통합 + SYSTEM_SECRET env 후 배포 세션).
- `ListMemberPraiseSongUseCaseMock`(praise 읽기, purge 무관)은 범위 밖 — 별도 사용자맥락/배치 결정 시 처리.
- 후속: PR③ ai→audit/admin.
