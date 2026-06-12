# [결정 제안] 관리자 웹 인증 — 카카오 → 자체 아이디 로그인 전환 + 로컬 온보딩 단순화

- 상태: **제안(검토 대기)** — 팀/강사 합의 필요
- 작성: 강태오(Lead), 2026-06-11
- 영향 문서: `02_ERD`, `04_API_명세서`, `CLAUDE.md`(OAuth 경로 §1·§5), `doc/.../2026-06-10 admin-kakao-auth-contract`
- 관련 기록: `workflows/2026-06-11_admin-web-login-500-local-dev-fix.md`, `workflows/2026-06-11_admin-web-dev-bypass-restore.md`

---

## 1. 배경 / 문제
1. **카카오 관리자 로그인이 실제로 동작하지 않음.** `admin-web/index.html`은 카카오 JS SDK v2.7.4를 로드하는데, `src/auth/kakao.ts`는 v1 API `Kakao.Auth.login(콜백)`을 호출 → 런타임 `Kakao.Auth.login is not a function`. v2는 콜백형 토큰 발급을 제거하고 `authorize()`(리다이렉트/code)만 제공한다.
2. **카카오 경로는 외부 의존이 크다.** 끝까지 되려면 (a) 프런트 리다이렉트 콜백 처리, (b) 서버 계약을 access token→code 교환으로 변경, (c) 카카오 개발자 콘솔에 리다이렉트 URI/도메인 등록·로그인 활성화, (d) 로그인 계정이 실제 관리자(admin_users)일 것 — 5가지가 모두 필요하다.
3. **관리자 콘솔에 소셜 로그인은 과하다.** 관리자 웹은 내부 운영 도구이므로, 사용자 앱(카카오)과 인증을 분리하는 편이 단순하고 안전하다.
4. **로컬 온보딩이 복잡하다(팀원 부담).** 현재 admin-web 로그인을 로컬에서 띄우려면 JWT 키 생성·`.env` DB/포트 정렬·`SERVER_PORT=8090`·터미널 2개 등 수동 절차가 많다(부록 A). 신규 팀원마다 동일 함정을 반복한다.

## 2. 제안 (요지)
관리자 웹 인증을 **카카오 → 자체 아이디/비밀번호 로그인**으로 전환한다. 관리자는 `username`/`password`로 로그인하고, 서버는 **기존과 동일한 ADMIN RS256 토큰**을 발급한다. 더불어 **로컬 dev 기동을 원커맨드로 단순화**해 팀원이 부록 A 같은 수동 절차를 하지 않게 한다.

핵심 이점: "토큰을 어떻게 받느냐"만 바뀌고, 그 이후(ADMIN JWT → `/admin/me`, `@PreAuthorize`, `admin_users` 권한 검증)는 **전부 그대로 재사용**된다. 외부(카카오) 의존이 사라져 **로컬에서 외부 없이 완전히 동작**한다.

## 3. 설계 (변경 시)
### 3.1 API (service-user)
- 신설: `POST /api/v1/admin/auth/login` — body `{ username, password }`. 기존 `AdminAuthController`(`/api/v1/admin/auth/**`, permitAll)에 메서드 추가.
- 응답: **기존 `AdminLoginResponse` 재사용**(access 30분 / refresh 14일 + `admin{ memberId, nickname, role, adminRole, status }`). 카카오 경로와 토큰 형식·검증 동일.
- 실패: 자격 불일치 401, 비활성/권한 문제 403(기존 ErrorCode 체계).
- 토큰 갱신: 공용 `POST /api/v1/auth/refresh` 그대로 사용(프런트 client.ts 변경 없음).

### 3.2 인증/도메인
- `AdminPasswordLoginUseCase`: `username`으로 `admin_users` 조회 → **BCrypt**로 `password_hash` 검증 → 활성/역할 확인 → `memberId`로 ADMIN RS256 토큰 발급.
- 다운스트림 admin-server `/api/v1/admin/**`는 **무변경**(JWT만 검증).

### 3.3 스키마 (ERD / Flyway — admin-server 소유)
- `admin_users`에 컬럼 추가: `username`(UNIQUE, NOT NULL), `password_hash`(NOT NULL). 선택: `password_updated_at`, `failed_login_count`, `locked_until`.
- 마이그레이션 1개(`Vxx__admin_users_password_login.sql`). 평문 비밀번호·예시 키는 저장 금지(CLAUDE.md §8).

### 3.4 시드 / 계정 발급
- dev-seed(repeatable)에 초기 관리자: `username` + `BCrypt(초기비번)`. (현행 카카오 dev-seed 대체/병행)
- 운영: SUPER_ADMIN이 관리자 계정 발급·비번 초기화(후속 AD 화면 또는 수동 시드).

### 3.5 프런트 (admin-web)
- `LoginPage`: 카카오 버튼 → **아이디/비밀번호 폼** → `/admin/auth/login` 호출 → 토큰 저장.
- `AuthContext` / `getAdminMe` / refresh / `ProtectedRoute` **변경 없음**.
- 선택: dev "개발용 ADMIN 토큰" 입력란은 유지(로컬 편의) 또는 제거(아이디 로그인이 로컬에서도 되므로 불필요).

## 4. 결정해야 할 선택지 (팀 투표)
1. **카카오 처리**: (A) 완전 제거 / (B) 병행(아이디 로그인 기본 + 카카오 후순위, `MemberAuthProvider`에 PASSWORD provider로 얹음).
2. **dev 토큰 우회(`X-Dev-User-Id`)**: 유지 / 제거(아이디 로그인으로 대체).
3. **비밀번호 정책 수준**: 최소(길이만) / 표준(복잡도+실패 잠금+초기 비번 강제변경). 실패 제한은 기존 `RateLimitFilter` 활용 가능.
4. **초기 관리자 발급 방식**: dev-seed 고정 / 운영용 발급 화면(후속 PR).

## 5. 보안 고려
- 비밀번호는 **BCrypt 해시만 저장**(평문·로그 금지, CLAUDE.md §9).
- 로그인 실패 rate limit(기존 필터 재사용), 가급적 초기 비번 강제 변경.
- 토큰 만료/회전 정책은 현행 유지(access 30m / refresh 14d).
- 관리자 권한 2단계 검증(`members.role=ADMIN` + `admin_users.admin_role`)은 그대로.

## 6. 로컬 온보딩 단순화 (목표 ②, 인증과 함께 처리)
부록 A의 수동 절차를 없애기 위해:
1. `scripts/run-admin-dev.ps1` 신설 — 키 로드 + `docker compose up -d mysql redis` + `SERVER_PORT=8090` + `:admin-server:bootRun`을 **한 번에**. 팀원은 이 스크립트 1개 + `npm run dev`만 실행.
2. `.env.example`에 DB/포트 정렬 기본값(3306/6379/qtai) 포함 → 수동 추가 불필요.
3. `admin-web/README`에 "백엔드 dev 기동" 절 추가.
- 결과(목표 흐름): `git pull` → (최초 1회) 키 생성 → 이후 매번 `run-admin-dev.ps1` + `npm run dev` → **아이디/비번으로 로그인**. 터미널 2개·SERVER_PORT 수기 입력·dev 토큰 트릭 불필요.

## 7. 영향 / 마이그레이션 순서 / 롤백
- 문서 갱신: `02_ERD`(admin_users 컬럼), `04_API`(/admin/auth/login), `CLAUDE.md`(OAuth 경로), admin-kakao-auth-contract(상태=대체/병행).
- 순서: ① 결정 승인 → ② ERD/Flyway → ③ service-user 엔드포인트+UseCase → ④ admin-web 폼 → ⑤ dev-seed/스크립트/문서 → ⑥ 테스트(인증 성공/실패/권한/토큰).
- 롤백: 카카오 경로를 (B 병행으로) 남겨두면 즉시 복귀 가능. (A 제거) 선택 시 되돌리려면 프런트 버튼·서버 경로 복원 필요.

## 8. 다음 단계
- 본 제안 팀 리뷰 → §4 선택지 확정.
- 승인 시 구현 브랜치 `feat/admin-id-login`(+ DX는 `chore/admin-dev-run-script`) 분리, 작은 PR로 dev 대상 진행(팀 MSA 중 머지 타이밍은 Lead 판단).

---

## 부록 A — 현행(복잡) 로컬 기동 절차 (없애고자 하는 대상)
> 신규 팀원이 admin-web 로그인까지 가기 위해 현재 거쳐야 하는 수동 절차. 본 제안의 §6으로 대체 목표.

```powershell
# 최초 1회
$OutputEncoding = [Console]::OutputEncoding = [Text.Encoding]::UTF8
cd C:\workspace\QT-AI-2nd-Team-Project
powershell -ExecutionPolicy Bypass -File scripts\generate-keys.ps1
@"
MYSQL_DATABASE=qtai
DB_USERNAME=qtai
DB_PASSWORD=qtai
MYSQL_ROOT_PASSWORD=qtai-root
MYSQL_HOST_PORT=3306
REDIS_HOST_PORT=6379
"@ | Add-Content -Encoding utf8 .env
cd admin-web; npm install; cd ..

# 매번 — 터미널 ① 백엔드
docker compose up -d mysql redis
Get-Content .env | ForEach-Object { if ($_ -match '^\s*([^#=]+)=(.*)$') { Set-Item "env:$($matches[1].Trim())" $matches[2].Trim() } }
$env:SPRING_PROFILES_ACTIVE = "dev"; $env:SERVER_PORT = "8090"
cd qtai-server; .\gradlew.bat :admin-server:bootRun

# 매번 — 터미널 ② 프런트
cd C:\workspace\QT-AI-2nd-Team-Project\admin-web; npm run dev
# → http://localhost:5173 → "개발용 ADMIN 토큰"에 dev 입력 → 로그인
```
