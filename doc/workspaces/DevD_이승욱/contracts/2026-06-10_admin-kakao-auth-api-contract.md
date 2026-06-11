# 관리자 카카오 인증 API 계약 (합의 완료) — 김지민(admin-web)

- **일자**: 2026-06-10 (합의 완료)
- **작성**: 이승욱(service-user BE)
- **합의자**: 김지민(admin-web FE)
- **상태**: ✅ **합의 완료** — 본 계약대로 service-user에 구현(task 3)
- **기준**: 기존 사용자 로그인(`POST /api/v1/auth/kakao`, `LoginResponse`)과 일관성 유지. 단일 DB·service-user가 유일한 JWT 발급자(RS256).

## 1. 엔드포인트
```
POST /api/v1/admin/auth/kakao        (permitAll — 인증 전 진입)
```
- 흐름: 카카오 access token 검증(`KakaoOAuthClient`) → 회원 조회(`members.kakao_id`) → **관리자 자격 검증**(`admin_users` / VerifyAdminRole, task 1) → ADMIN 스코프 JWT 발급(`JwtProvider`).
- 관리자가 아니면 토큰을 발급하지 않고 인가 오류를 반환.

## 2. 요청 (사용자 로그인과 동일 형태)
```json
{ "kakaoAccessToken": "<카카오 SDK가 발급한 access token>" }
```
- 필드: `kakaoAccessToken` (string, 필수). 기존 `LoginRequest`와 동일 — FE가 카카오 SDK로 받은 토큰을 그대로 전달.

## 3. 성공 응답 (200) — 공통 envelope `ApiResponse<AdminLoginResponse>`
```json
{
  "success": true,
  "data": {
    "accessToken": "<JWT, role=ADMIN>",
    "refreshToken": "<JWT refresh>",
    "admin": {
      "memberId": 12,
      "nickname": "운영자",
      "role": "ADMIN",
      "adminRole": "SUPER_ADMIN",
      "status": "ACTIVE"
    }
  },
  "error": null
}
```
- `accessToken` / `refreshToken`: 사용자 로그인과 동일하게 body로 전달(admin-web가 저장). access token claim의 `role=ADMIN`.
- `admin` 블록(사용자 `member` 요약과 대응, admin 전용 필드 추가):
  - `memberId` (Long) — 회원 PK
  - `nickname` (string)
  - `role` (string) — 항상 `ADMIN`
  - **`adminRole`** (string) — `OPERATOR` / `REVIEWER` / `CONTENT_CREATOR` / `SUPER_ADMIN` (admin-web 메뉴 권한 분기용 ← **FE가 가장 필요로 할 값**)
  - `status` (string) — `ACTIVE` 등

## 4. 오류 응답 (공통 envelope, `success:false`) — ErrorCode 04 표준 확정
| 상황 | HTTP | ErrorCode | code | 의미 |
|------|------|-----------|------|------|
| 카카오 토큰 무효/만료 | 401 | `KAKAO_AUTH_FAILED` | M0009 | 카카오 검증 실패 |
| 관리자 회원 없음/일반 회원 | 403 | `ADMIN_USER_NOT_FOUND` | AD0001 | `members.role!=ADMIN` 또는 `admin_users` 없음 → 관리자 로그인 거부 |
| 관리자 계정 비활성 | 403 | `ADMIN_USER_DISABLED` | AD0002 | `admin_users.status` 비활성 |
| 회원 정지 | 403 | `MEMBER_SUSPENDED` | M0007 | `members.status=SUSPENDED` |
```json
{ "success": false, "data": null,
  "error": { "code": "AD0001", "message": "관리자 계정을 찾을 수 없습니다." } }
```

## 5. 합의 완료 결정 (2026-06-10, 김지민 ↔ 이승욱)
1. **응답 키 이름**: 사용자 로그인은 `member`, 관리자는 **`admin`** 으로 구분 유지(§3 그대로). ✅
2. **adminRole 노출 범위**: **단일 역할 문자열**로 확정(배열 미사용). ✅
3. **refreshToken 전달 방식**: 앱·웹 **모두 body로 전달**(HttpOnly 쿠키 미사용). admin-web도 사용자 앱과 동일하게 body의 `refreshToken`을 받아 저장. ✅
4. **권한 부족(403) 처리**: FE는 **별도 안내 화면 없이 ErrorCode를 그대로 표시**. 따라서 `error.code`/`error.message`가 그대로 노출 가능하도록 명확히 채운다(§4). ✅
5. **토큰 만료**: admin access token 만료 = **사용자와 동일(access 30분, refresh 14일)**. ✅

> 위 5건 확정 → §1~§4가 그대로 구현 계약. task 3는 별도 협의 없이 본 문서대로 진행.

## 6. 비고
- 카카오 검증·JWT 발급은 service-user의 `KakaoOAuthClient`/`AuthService`/`JwtProvider` 재사용(신규 외부 연동 없음).
- 관리자 자격 검증은 task 1(VerifyAdminRole RestClient → admin-server)에 의존. task 1·dev 시드(task 4) 완료 후 task 3에서 본 계약대로 구현.
- 콘텐츠/AI 서비스는 `/api/v1/admin/**` 차단(transition-status §1). admin 인증 엔드포인트는 JWT 발급 권한이 있는 service-user에만 둔다.
