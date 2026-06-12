# [결정 제안] 운영 관리자 계정 발급·비밀번호 정책

- 상태: **제안(검토 대기)** — 팀/강사 합의 필요
- 작성: 강태오(Lead), 2026-06-12
- 선행: 관리자 자체 아이디/비밀번호 로그인(PR #539 머지), 로그인 보안 강화(PR #550), dev-bypass 제거(PR #549)
- 영향 문서: `02_ERD`, `04_API_명세서`, `06_화면_기능_정의서`(admin 화면)

---

## 1. 배경 / 문제
- 현재 관리자 계정은 **dev 시드(admin/admin1234)만 존재**한다. 운영에서 관리자 계정을 만들 방법(발급·비활성·비밀번호 초기화)이 없다.
- 비밀번호 정책(길이·복잡도·초기 비번 강제 변경·실패 잠금)이 코드 차원에서 정의되어 있지 않다(로그인 시도 제한은 PR #550에서 추가됨).
- 자동리뷰도 "운영 계정 발급/비번 정책"을 후속 항목으로 지적.

## 2. 제안 (요지)
**SUPER_ADMIN이 관리자 계정을 발급·관리**하고, **관리자는 자신의 비밀번호를 변경**할 수 있게 한다. 최초 발급 비밀번호는 **첫 로그인 시 강제 변경**한다. 비밀번호는 BCrypt 해시만 저장(기존 정책 유지).

핵심: 로그인·토큰 발급(이미 구현)은 그대로 두고, **계정 라이프사이클(발급/초기화/상태)과 비밀번호 정책**만 추가한다.

## 3. 설계 (변경 시)
### 3.1 API (admin-server)
- `POST   /api/v1/admin/admins` — (SUPER_ADMIN) 관리자 계정 발급: `{ username, adminRole, memberId? }` → 임시 비밀번호 1회 반환(또는 발급자가 지정). `must_change_password=true`.
- `PATCH  /api/v1/admin/admins/{adminUserId}/password-reset` — (SUPER_ADMIN) 비밀번호 초기화(임시 비번 재발급, `must_change_password=true`).
- `PATCH  /api/v1/admin/admins/{adminUserId}/status` — (SUPER_ADMIN) 활성/비활성.
- `GET    /api/v1/admin/admins` — (SUPER_ADMIN) 관리자 목록.
- `POST   /api/v1/admin/me/password` — (본인) 비밀번호 변경: `{ currentPassword, newPassword }`. 성공 시 `must_change_password=false`.
- `must_change_password=true`인 계정은 로그인은 되지만, 비밀번호 변경 외 다른 관리자 API 접근을 제한(서버 가드 또는 프런트 강제 라우팅).

### 3.2 스키마 (ERD / Flyway — admin-server 소유)
- `admin_users`에 컬럼 추가: `must_change_password BOOLEAN DEFAULT TRUE`, `password_updated_at DATETIME NULL`, (선택) `last_login_at DATETIME NULL`.

### 3.3 비밀번호 정책
- 최소 길이(예: 10자 이상) + 3종 이상 문자(영문/숫자/특수) 중 택1 수준 — **팀 결정 필요**.
- 임시 비밀번호는 안전 난수 생성(평문은 발급 응답 1회만 노출, 저장 금지).
- (선택) 비밀번호 재사용 금지/만료(90일) — 후속 여지.

### 3.4 프런트 (admin-web)
- 관리자 계정 관리 화면(예: AD-09, SUPER_ADMIN 전용): 목록·발급·초기화·상태.
- 비밀번호 변경 화면 + `must_change_password=true` 시 강제 라우팅.

## 4. 결정해야 할 선택지 (팀 투표)
1. **UI 범위**: (A) 지금 BE만(임시 운영은 발급 API + 수동) / (B) BE + admin-web 계정관리 화면까지.
2. **비밀번호 정책 수준**: 최소(길이만) / 표준(길이+복잡도+초기 강제변경) / 강화(+만료·재사용금지).
3. **초기 비밀번호 전달**: 발급 응답 1회 노출(발급자가 안전 전달) / 이메일 발송(메일 인프라 필요).
4. **must_change_password 강제**: 도입 / 미도입(초기엔 권고만).

## 5. 보안 고려
- 임시·신규 비밀번호 평문은 로그·저장 금지, 응답 1회만. BCrypt 해시 저장.
- 계정 발급/초기화/상태변경은 `audit_logs`에 기록(주체=SUPER_ADMIN).
- 로그인 시도 제한(PR #550)과 결합 — 초기 비번 무작위 대입 방어.

## 6. 영향 / 순서 / 롤백
- 문서: `02_ERD`(컬럼), `04_API`(엔드포인트), `06_화면정의서`(AD 화면) 갱신(문서 레포 PR).
- 순서: ① 결정 승인 → ② ERD/Flyway → ③ admin-server 엔드포인트+정책 → ④ admin-web 화면 → ⑤ 테스트(발급/초기화/상태/본인변경/강제변경/감사로그).
- 롤백: 컬럼은 NULL 허용·기본값으로 추가 → 기존 로그인 흐름 무영향. 단계적 적용 가능.

## 7. 다음 단계
- 본 제안 팀 리뷰 → §4 선택지 확정 → 구현 브랜치 `feature/admin-account-provisioning`로 진행.
