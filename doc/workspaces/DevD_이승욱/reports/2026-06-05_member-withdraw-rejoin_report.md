# 2026-06-05 탈퇴 회원 재가입 차단(M0009) 해소 — 서버 결과 보고

## 요약
탈퇴 시 즉시 익명화가 `member_auth_providers` UNIQUE 충돌을 일으켜 재가입을
영구 차단하던 버그(M0009)를, 탈퇴 정책 개편(개인정보 2년 보존 + 재로그인 시
재활성화, 2026-06-05 결정)으로 근본 해결했다. 보존기간 만료 회원을 정리하는
일일 배치를 도메인 경계를 지키는 포트 위임 구조로 추가했다.

## 완료된 작업

### 1. 탈퇴 — 익명화 제거, 2년 보존 (fix)
- `Member.withdraw()`: status=WITHDRAWN + withdrawnAt 기록만 수행
  (기존: 닉네임/이메일/프로필/kakaoId 즉시 익명화 → M0009 원인)
- 세션(refresh token) 무효화는 `MemberWithdrawnEvent` +
  `@TransactionalEventListener(AFTER_COMMIT)` 핸들러로 분리 —
  롤백 시 토큰 유지, Redis 실패가 탈퇴 트랜잭션을 깨지 않음
- `WithdrawUseCase` javadoc 정책 갱신

### 2. 재로그인 — 기존 계정 재활성화 (fix)
- `Member.reactivate(email, profileImageUrl)` 추가 — 이메일·프로필은 카카오
  최신 값으로 갱신, 닉네임·nicknameChangedAt(7일 잠금)은 유지
- `AuthService.login()`: WITHDRAWN 회원 발견 시 재활성화 후 토큰 발급.
  재활성화는 login 메인 경로와 login 동시가입 재조회 경로에서만 수행
- 부정 경로 유지: SUSPENDED 로그인 차단, refresh 경로는 재활성화 없이
  MEMBER_ALREADY_WITHDRAWN 유지 (정책 주석 + 회귀 테스트로 고정)

### 3. 보존기간(2년) 만료 정리 배치 (feat)
- `MemberRetentionPurgeService`(오케스트레이터): 각 도메인 데이터 삭제를
  해당 도메인 api 포트(`Purge*UseCase` — note/sharing/praise/mission/
  notification/report)로 위임, member는 자기 테이블만 직접 삭제
- 호출 순서(FK 역순): sharing → note → praise → mission → notification →
  report → member 본체(member_auth_providers는 ON DELETE CASCADE)
- 댓글 자기참조 FK는 sharing 도메인에서 리프-우선 반복 삭제
  (빈 IN() 가드, parent_id cycle 감지 시 해당 회원만 건너뜀)
- 관리자 연결 회원(비활성 DISABLED 포함)은 자동 삭제 제외 —
  `VerifyAdminRoleUseCase` 계약(`AdminService.findActiveAdminUser`) 기반
- 회원 단위 트랜잭션 격리, 1회 실행당 LIMIT 500 (잔여분 다음 실행 처리)
- `MemberRetentionPurgeBatch`: 매일 03:00 KST(@Scheduled), SYSTEM_BATCH 주체
- MySQL/H2 공통 동작 SQL만 사용 (cutoff는 Clock 기반 파라미터 바인딩)

## 변경 파일 (주요)
| 파일 | 변경 내용 |
|------|----------|
| `domain/member/internal/Member.java` | withdraw 익명화 제거 + reactivate 추가 |
| `domain/member/internal/AuthService.java` | 로그인 시 재활성화 분기 + 정책 주석 |
| `domain/member/internal/MemberService.java` | 탈퇴 이벤트 발행(AFTER_COMMIT 분리) |
| `domain/member/internal/MemberWithdrawnEvent(Handler).java` | 신규 — 세션 무효화 이벤트 |
| `domain/member/internal/MemberRetentionPurgeService.java` | 신규 — 만료 정리 오케스트레이터 |
| `domain/member/api/Purge…·WithdrawUseCase` | 포트 신설·정책 갱신 |
| `domain/{note,sharing,praise,mission,notification,report}/api·internal` | Purge 포트 + 구현 (순수 추가) |
| `batch/MemberRetentionPurgeBatch.java` | 신규 — 03:00 KST 스케줄 |
| 테스트 9파일 | 재활성화·보존·연쇄 삭제·경계·cycle·cron·이벤트 검증 |

## 검증
- [x] `gradlew build` 전체 통과 (test/check/ArchUnit 도메인 경계 포함)
- [x] 통합 테스트(H2): 연쇄 삭제(3단계 대댓글 포함)·정확히 2년 경계·
  관리자 연결(활성/비활성) 제외·댓글 cycle 회원 단위 skip·대상 없음 0건
- [x] 단위 테스트: 재활성화(메인/재조회 경로)·refresh 차단 회귀·SUSPENDED 차단·
  탈퇴 이벤트 발행·핸들러 실패 무전파·배치 cron 고정
- [x] 도커 MySQL E2E: 탈퇴 → 카카오 재로그인 → `탈퇴 회원 재활성화: memberId=1`
  → `로그인 성공` (탈퇴/재로그인 2회 사이클 확인)

## 참고
- mission/notification/report는 담당 범위 밖 도메인이나 Purge 포트 **순수 추가**만
  수행했고 기존 코드는 수정하지 않았다 (PR 본문 "담당 범위 밖 변경" 사유 기재)
- `member_auth_providers`의 ON DELETE CASCADE는 Flyway 정의라 H2 테스트에서
  미검증 — 실 MySQL 스모크 테스트는 후속 과제
- `07_요구사항_정의서.md` 탈퇴 정책 반영 필요 (2026-06-05 결정)
