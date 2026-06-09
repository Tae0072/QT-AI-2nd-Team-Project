# 스터디노트 — cross-service 삭제(purge)와 분산 트랜잭션의 부재

작성일: 2026-06-10 / 작성: Claude (Lead 강태오)

## 1. 모놀리식의 "회원 1명 = 1트랜잭션"이 MSA에서 깨진다

모놀리식 `MemberRetentionPurgeService`는 한 회원의 sharing→note→praise→…→members 삭제를 **하나의
`transactionTemplate.executeWithoutResult`** 안에서 처리했다. 한 도메인 삭제가 실패하면 전체 롤백 → all-or-nothing.

MSA로 쪼개면 sharing/note/report/praise 삭제가 각각 **다른 서비스로의 HTTP 호출**이 된다. 단일 DB를 공유해도
각 서비스는 자기 트랜잭션 매니저로 **독립 커밋**한다. RestClient 호출은 호출자(service-user)의 트랜잭션에 참여할 수
없다. 즉 "회원 1명 = 1트랜잭션" 보장이 사라진다. (계약 javadoc의 "호출자 트랜잭션에 참여"는 모놀리식 시절 표현.)

## 2. 그래서 Saga가 필요한가? — 아니다(회의록 §3)

회의 결정은 "RestClient 동기만, Saga/보상 트랜잭션 미사용"이다. 대신 **멱등 + 재처리**로 일관성을 맞춘다:
- 각 purge는 `DELETE ... WHERE member_id = ?` 형태라 **여러 번 실행해도 결과가 같다(멱등)**.
- 오케스트레이터는 회원 단위로 try/catch해 한 회원 실패가 배치 전체를 멈추지 않게 하고, members 본체 삭제는
  **마지막**에 한다. 중간 실패 시 회원 row가 남아 **다음 배치 실행에서 다시 시도** → 결국 정리된다.
- "유실률 0%" 같은 단정 대신 "실패 로그 + 재처리 가능 상태"로 표현한다(프로젝트 표현 규칙).

## 3. 위험한 배치는 두 겹의 가드로 막는다

회원 데이터 hard delete는 사고 시 복구 불가다. 두 겹으로 막는다:
1. **수신 권한 게이트**: purge 엔드포인트는 `@PreAuthorize("hasRole('SYSTEM_BATCH')")` — 사용자/ADMIN 토큰으로는 못 부른다.
2. **배치 활성 가드**: `qtai.retention.purge.enabled`(기본 false). 이걸 켜는 전제는 `hasAdminLink`가 실제로
   동작하는 것(관리자 회원 오삭제 방지)인데, 그건 service-user→admin `VerifyAdminRoleUseCase`가 Mock이 아니라
   실제 RestClient여야 한다. **어댑터를 만드는 것(이 PR)과 가드를 켜는 것(배포)은 분리**해야 안전하다.

## 핵심 교훈

cross-service 삭제는 분산 트랜잭션 없이 "멱등 + 회원 단위 실패 격리 + 재처리"로 설계하고, 파괴적 배치는
수신 권한 게이트와 활성 플래그(실제 관리자 확인이 가능해질 때까지 off) 두 겹으로 막는다.

관련: [[2026-06-10_msa-batch-system-token-restclient]], [[2026-06-10_msa-batch-write-endpoint-and-multi-contract-adapter]], CLAUDE.md §3·§5·§8.
