# 2026-06-05 회원 탈퇴 정책 개편 — 2년 보존·재활성화·만료 정리 배치

## 목표
회원 탈퇴 후 같은 카카오 계정으로 재로그인하면 M0009(카카오 인증 실패)로
영구 차단되는 버그를 근본 해결하고, 탈퇴 정책을 "개인정보 2년 보존 고지 +
보존 중 재로그인 시 계정 복구 + 2년 경과 시 자동 삭제"로 확정·구현한다.

## 작업 브랜치
- 서버: `bugfix/member-withdraw-rejoin` (dev 기반)
- Flutter: `bugfix/flutter-withdraw-cleanup` (dev 기반)

## 원인 진단

### 증상
탈퇴 → 재로그인 시 `BusinessException: M0009 — 카카오 인증에 실패했습니다.`

### 분석 (도커 MySQL 로그·데이터 직접 확인)
1. 카카오 API 호출은 성공 (`카카오 사용자 정보 조회 성공: kakaoId=...`)
2. 기존 `Member.withdraw()`가 탈퇴 즉시 kakao_id를 익명화(-id) →
   재로그인 시 `findByKakaoId` 미발견 → 신규 가입 경로 진입
3. `member_auth_providers`의 `uk_auth_provider_user(provider, provider_user_id)`
   row는 익명화되지 않고 남음 → INSERT 시 SQL 1062 중복
4. `AuthService`가 이를 "동시 가입 경합"으로 오판 → kakao_id 재조회 → 없음 → M0009

### Flutter 측 연쇄 증상
- 탈퇴 핸들러가 SecureStorage 토큰·카카오 세션·authStatus를 정리하지 않음 →
  재로그인 성공 시 `setAuthenticated()`가 동일 상태값이라 화면 전환 불발(로그인 화면 루프)
- `logout()`이 로컬 토큰을 먼저 삭제 후 서버 호출 → 무인증 호출로 Redis refresh 폐기 실패

## 의사결정 기록 (2026-06-05, 이승욱)
| 항목 | 결정 |
|------|------|
| 탈퇴 시 개인정보 | 즉시 익명화하지 않고 **2년 보존** (탈퇴 시 고지) |
| 보존 중 재로그인 | 기존 계정 **재활성화** (신규 가입 아님) |
| 2년 경과 처리 | 매일 03:00 KST 배치가 **전부 hard delete** — 회원·노트·나눔 글·타인 대댓글까지 연쇄 삭제 (B안. 하이브리드 아님) |
| 탈퇴 시 카카오 | **unlink(연결끊기)** — 재로그인 시 동의화면부터 |
| 탈퇴 후 첫 로그인 | **Prompt.login으로 카카오 계정 재인증(이메일/비번) 1회 강제** |
| 고지 문구 | "탈퇴 시 계정은 비활성화되며, 개인정보와 작성 기록은 관련 법령에 따라 2년간 보관 후 자동 삭제됩니다. 보관 기간 내 같은 카카오 계정으로 다시 로그인하면 계정과 기록이 복구됩니다." |

## 단계

### 1단계: 서버 — 탈퇴·재활성화 (완료)
- `Member.withdraw()`: 익명화 제거, status=WITHDRAWN + withdrawnAt만 기록
- `Member.reactivate(email, profileImageUrl)` 추가 — 닉네임·7일 잠금은 유지
- `AuthService.login()`: WITHDRAWN 발견 시 reactivate 후 진행 (SUSPENDED 차단 유지,
  refresh 경로는 재활성화 없이 MEMBER_ALREADY_WITHDRAWN 유지)
- `MemberService.withdraw()`: 세션(refresh token) 무효화 — AFTER_COMMIT 이벤트로 분리

### 2단계: 서버 — 보존기간 만료 정리 배치 (완료)
- `PurgeExpiredWithdrawnMembersUseCase` + `MemberRetentionPurgeService`(오케스트레이터)
- 도메인 경계: 각 도메인 데이터 삭제는 해당 도메인 api 포트로 위임 —
  note/sharing/praise/mission/notification/report에 `Purge*UseCase` 추가,
  member는 자기 테이블(members, member_settings)만 직접 삭제
- 호출 순서(FK 역순): sharing(나눔 글의 note FK 선행 해제) → note → praise →
  mission → notification → report → member 본체(auth_providers는 ON DELETE CASCADE)
- 댓글 자기참조 FK: sharing 도메인에서 트리 transitive 수집 후 리프-우선 반복 삭제
  (빈 IN() 가드 + parent_id cycle 감지 시 해당 회원만 건너뜀)
- 관리자 연결 회원(비활성 포함)은 자동 삭제 제외(수동 처리),
  회원 단위 트랜잭션으로 부분 실패 격리, 1회 실행당 LIMIT 500
- `MemberRetentionPurgeBatch`: 매일 03:00 KST (00:05/04:00 기존 배치와 시간 분리)

### 3단계: Flutter — 탈퇴/로그아웃 정리 (완료)
- 탈퇴: 서버 탈퇴 → unlink + 토큰 삭제 + 재인증 플래그 → setUnauthenticated
- logout(): 서버 폐기 먼저 → 카카오 logout → 로컬 토큰 삭제(finally 보장)
- 탈퇴 후 첫 로그인: `loginWithKakaoAccount(prompts: [Prompt.login])` 1회 강제
- 탈퇴 다이얼로그 고지 문구 교체

### 4단계: 검증 (완료)
- 서버: `gradlew build` 전체 통과 (ArchUnit 도메인 경계 테스트 포함)
- Flutter: `flutter analyze` 0건, `flutter test` 100개 통과
- E2E: 도커 MySQL에서 탈퇴 → 재로그인 → `탈퇴 회원 재활성화: memberId=1` →
  `로그인 성공` 로그 확인 (2회 반복 사이클 검증)

## 리뷰 반영 이력
- [BLOCK] 타 도메인 테이블 직접 DELETE(경계 위반) → 도메인별 api 포트 위임으로 전환
- [BLOCK] 빈 `IN ()` SQL 가드 + 댓글 parent_id cycle 감지(회원 단위 skip)
- [BLOCK] `catch (Exception)` → `DataAccessException | TransactionException |
  IllegalStateException` 멀티캐치로 축소
- [WARN] 탈퇴 세션 무효화를 `@TransactionalEventListener(AFTER_COMMIT)` 이벤트로 분리
- [WARN] 대량 만료 페이지네이션(LIMIT 500), 3단계 대댓글·cycle 엣지 테스트,
  배치 cron 회귀 테스트, refresh 경로 정책 주석·회귀 테스트
- 2차: `reactivateIfWithdrawn` 호출 위치 검증(login 메인 + login 동시가입 재조회 —
  refresh 경로 아님) + 회귀 테스트, `VerifyAdminRoleUseCase`의 DISABLED 분기 계약
  확인(`AdminService.findActiveAdminUser`) + 테스트, 문서 인코딩 복구

## 트러블슈팅 이력
- **구이미지 재빌드 사고**: PR용 Flutter 브랜치(서버 수정 미포함)로 작업 트리를
  전환해둔 사이 `docker compose up --build` 실행 → 구버전 서버로 빌드되어 M0009 재발.
  서버+Flutter 수정이 모두 포함된 트리로 복구 후 재빌드로 해결.
  → 교훈: 도커 빌드는 작업 트리 상태에 의존하므로 빌드 전 브랜치 상태 확인 필수
- **문서 인코딩 깨짐**: PowerShell `Get-Content`(기본 ANSI/CP949)로 UTF-8 문서를
  읽어 재저장하면서 한글 전체가 깨짐 → UTF-8 보장 도구로 재작성하여 복구.
  → 교훈: 한글 문서 일괄 치환은 인코딩 명시 필수, 저장 후 바이트 단위 검증
- 에뮬레이터에 카카오 계정 웹 세션이 남아 "내 정보가 자동으로 뜨는" 현상은
  서비스가 제어할 수 없는 카카오 계정 자체 로그인 → Prompt.login 도입 배경

## 후속 과제
- `07_요구사항_정의서.md`에 탈퇴 정책 변경 반영 (문서 저장소)
- `member_auth_providers` ON DELETE CASCADE는 Flyway 정의라 H2 테스트 미검증 —
  실 MySQL 스모크 테스트 고려
- 탈퇴 회원 닉네임의 사용자 노출 표시 정책(나눔 글 작성자명 등)은 별도 검토
