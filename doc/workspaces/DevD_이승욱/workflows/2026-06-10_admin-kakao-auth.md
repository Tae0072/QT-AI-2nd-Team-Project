# 2026-06-10 관리자 카카오 인증 엔드포인트 (task 3)

## 목표
admin-web 관리자 로그인을 위한 `POST /api/v1/admin/auth/kakao` 구현. 카카오 토큰 검증 → 회원 조회 → 관리자 자격 검증(task 1 어댑터) → ADMIN 스코프 토큰 발급. 계약 합의본(task 5) 준수. 진입 전 잠금 3건 동봉.

## 배경
- service-user가 유일한 JWT 발급자(RS256)라 관리자 로그인도 service-user에 둔다(admin-server는 JWT 미발급).
- 기존 사용자 로그인(`AuthService`, `POST /api/v1/auth/kakao`) 재사용: `KakaoOAuthClient`·`JwtProvider`·`RefreshTokenStore`.
- service-user SecurityConfig는 `/api/v1/admin/**`를 **denyAll**(관리자 비즈니스 API는 admin-server). 로그인만 그 앞에 permitAll로 연다.

## 작업 내용
### task 3 — 관리자 인증
- `member/api/AdminLoginUseCase` + `dto/AdminLoginRequest`(kakaoAccessToken)·`AdminLoginResponse`(accessToken/refreshToken/admin{memberId,nickname,role,adminRole,status}) — 계약 합의본.
- `member/internal/AdminAuthService`(AdminLoginUseCase 구현): ① 카카오 검증(실패→KAKAO_AUTH_FAILED) ② 회원 조회 — **자동가입 안 함**(없으면 ADMIN_USER_NOT_FOUND) ③ 상태 검증(SUSPENDED→MEMBER_SUSPENDED, 비ACTIVE→ADMIN_USER_NOT_FOUND) ④ §5 1차 `members.role=ADMIN` ⑤ §5 2차 `VerifyAdminRoleUseCase.getActiveAdmin`(task1 어댑터, admin-server) ⑥ ADMIN 토큰 발급(만료 사용자와 동일 30분/14일). 외부 호출은 트랜잭션 밖.
- `member/web/AdminAuthController`: `POST /api/v1/admin/auth/kakao` → `ApiResponse<AdminLoginResponse>`.
- `user/SecurityConfig`: `POST /api/v1/admin/auth/kakao` permitAll(denyAll보다 먼저 선언해 우선).

### 진입 전 잠금 3건
- (a) `admin-server/application-prod.yml`에 `spring.flyway.locations: classpath:db/migration` 명시 — dev-seed 상속 사고 차단.
- (b) `DEV_ADMIN_ROLE` 기본값 `SUPER_ADMIN`→`OPERATOR`(local·dev) — env 명시 시에만 SUPER_ADMIN.
- (c) 계약 §4 오류표를 04 표준 ErrorCode로 확정(KAKAO_AUTH_FAILED M0009·ADMIN_USER_NOT_FOUND AD0001·ADMIN_USER_DISABLED AD0002·MEMBER_SUSPENDED M0007).

## 범위
- 브랜치: `feature/admin-kakao-auth` (base: `dev-msa`)
- 변경: service-user 5(UseCase/DTO2/Service/Controller) + SecurityConfig + 테스트 2 / admin-server yml 3(prod·local·dev) / 계약·문서.

## 검증
- `:service-user:test` — `AdminAuthServiceTest` 6건 + `AdminAuthControllerSecurityTest` 4건 = **10건 0 실패**.
  - 서비스: 정상(ADMIN 토큰·adminRole)·카카오실패·회원없음·일반회원(role!=ADMIN)·정지·admin검증 실패 전파.
  - 컨트롤러: permitAll 200·관리자아님 403 AD0001·빈토큰 400·다른 admin 경로 차단(401/403).
- **task 1 의존**: 런타임 admin 검증은 task1(`VerifyAdminRoleRestClientAdapter`)이 dev-msa에 머지돼야 동작. 본 브랜치 컨텍스트는 Mock이라 컨트롤러 테스트는 UseCase를 @MockBean으로 격리.

## 미해결 / 후속
- **task 2**(마지막): `MemberRetentionPurgeBatch`에 `qtai.retention.purge.enabled` 게이트 추가 후, task1 검증 통합으로 admin 연결 회원 식별이 정확해지면 활성화.
- task 1 머지 후 service-user에서 admin 로그인 end-to-end(실제 admin-server 호출) 통합 검증(선택).

## 담당
- DevD 이승욱 (service-user 인증 BE)
