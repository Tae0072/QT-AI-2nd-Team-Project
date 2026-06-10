# 관리자 카카오 인증 API 계약 (제안) — 김지민(admin-web) 합의용

> ⚠️ 이 문서는 원래 `2026-05-19_qtai-server-comments-and-merge.md` 파일에 잘못 덮여 있던 내용을 올바른 파일로 분리한 것이다(2026-06-10 정리). 원본 5/19 주석작성 문서는 복구됨.

- **일자:** 2026-06-10
- **작성:** 이승욱(service-user BE)
- **대상 합의자:** 김지민(admin-web FE)
- **상태:** 엔드포인트 경로 **확정**(신규 `admin/auth/kakao`) + 응답 형태 **5개 합의 완료**(§7). service-user task 3에서 구현 대기.
- **기준:** 기존 사용자 로그인(`POST /api/v1/auth/kakao`, `LoginResponse`)과 일관성 유지. 단일 DB·service-user가 유일한 JWT 발급자(RS256).

> ✅ **엔드포인트 경로 확정(2026-06-10):** 06-09 결정 ③④는 "기존 `/api/v1/auth/kakao` 재사용"이었으나, 2026-06-10 팀 결정으로 **신규 `POST /api/v1/admin/auth/kakao` 채택으로 확정**(서버 `/oauth2` 미사용 — JS SDK 토큰을 서버로 전달하므로 §5의 `/oauth2` 금지와는 충돌 없음). 응답 형태 5개 포인트도 §7 FE 합의 완료.
>
> ⚠️ **SSoT 갱신 필요 — Lead 검토 (`CLAUDE.md §2` 절차):** 신규 `POST /api/v1/admin/auth/kakao`는 **`CLAUDE.md §5`**(현재 *"Kakao 인증도 `POST /api/v1/auth/kakao`를 사용"*)에 명시되지 않은 엔드포인트다. 근거는 **2026-06-10 팀 결정**이며, SSoT 반영을 위해 **CLAUDE.md §5 갱신 PR + Lead 승인**이 후속으로 필요하다. 그 전까지 본 결정은 **"팀 결정 기준 확정 · SSoT 반영 대기"** 상태로 본다(코드는 이 경로로 선구현, 경로 자체는 Lead 승인 후 고정).

---

## 1. 엔드포인트

```
POST /api/v1/admin/auth/kakao    (permitAll — 인증 전 진입)
```

**흐름:**
카카오 access token 검증(`KakaoOAuthClient`) → 회원 조회(`members.kakao_id`) → **관리자 자격 검증**(`admin_users` / `VerifyAdminRole`, task 1) → ADMIN 스코프 JWT 발급(`JwtProvider`)

> 관리자가 아니면 토큰을 발급하지 않고 인가 오류를 반환.

---

## 2. 요청 (사용자 로그인과 동일 형태)

```json
{ "kakaoAccessToken": "<카카오 SDK가 발급한 access token>" }
```

- **필드:** `kakaoAccessToken` (string, 필수)
  기존 `LoginRequest`와 동일 — FE가 카카오 SDK로 받은 토큰을 그대로 전달.

---

## 3. 성공 응답 (200) — 공통 envelope

`ApiResponse<AdminLoginResponse>`

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

- **`accessToken` / `refreshToken`:** 사용자 로그인과 동일하게 body로 전달(admin-web이 저장). access token claim의 `role=ADMIN`.
- **`admin` 블록** (사용자 `member` 요약과 대응, admin 전용 필드 추가):

| 필드 | 타입 | 설명 |
|---|---|---|
| `memberId` | Long | 회원 PK |
| `nickname` | string | 닉네임 |
| `role` | string | 항상 `ADMIN` |
| `adminRole` | string | `OPERATOR` / `REVIEWER` / `CONTENT_CREATOR` / `SUPER_ADMIN` (admin-web 메뉴 권한 분기용 ← **FE가 가장 필요로 할 값**) |
| `status` | string | `ACTIVE` 등 |

---

## 4. 오류 응답 (공통 envelope, `success: false`)

| 상황 | HTTP | ErrorCode(안) | 의미 |
|---|---|---|---|
| 카카오 토큰 무효/만료 | 401 | (기존 카카오 인증 오류 코드 재사용) | 카카오 검증 실패 |
| 회원은 있으나 관리자 아님 | 403 | `ADMIN_USER_NOT_FOUND` | `admin_users` 없음 → 관리자 로그인 거부 |
| 관리자 계정 비활성/정지 | 403 | (admin status 오류 코드) | `admin_users.status` 비활성 |

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ADMIN_USER_NOT_FOUND",
    "message": "관리자 권한이 없습니다."
  }
}
```

---

## 5. 합의 필요 포인트 (FE 확인 요청)

1. **응답 키 이름:** 사용자 로그인은 `member`, 관리자는 `admin`으로 구분 제안 — FE가 `member`로 통일하길 원하면 맞춤 가능.
2. **`adminRole` 노출 범위:** 단일 역할 문자열로 충분한지, 아니면 권한 목록(향후 다중 역할 대비 배열)이 필요한지.
3. **`refreshToken` 전달 방식:** 사용자 앱은 body→SecureStorage. admin-web(브라우저)도 body로 받을지, HttpOnly 쿠키를 원할지(웹 보안상 쿠키 선호 가능).
4. **권한 부족(403) 처리 UX:** FE가 별도 안내 화면을 띄울지 → ErrorCode/message 문구 합의.
5. **토큰 만료:** admin access token 만료를 사용자와 동일(30분)로 둘지.

---

## 6. 비고

- 카카오 검증·JWT 발급은 service-user의 `KakaoOAuthClient` / `AuthService` / `JwtProvider` 재사용 (신규 외부 연동 없음).
- 관리자 자격 검증은 task 1(`VerifyAdminRole` RestClient → admin-server)에 의존. task 1·dev 시드(task 4) 완료 후 task 3에서 본 계약대로 구현.
- 콘텐츠/AI 서비스는 `/api/v1/admin/**` 차단(transition-status §1). admin 인증 엔드포인트는 JWT 발급 권한이 있는 service-user에만 둔다.

---

## 7. FE(김지민) 회신 — 협의 완료 (2026-06-10)

> 응답 형태 5개 합의 포인트는 아래와 같이 **FE 합의 완료**. (엔드포인트 경로도 **신규 `admin/auth/kakao`로 확정** — 위 §상단 참고.)

| # | 합의 포인트 | 제안(이승욱) | FE 회신(김지민) | 상태 |
|---|---|---|---|---|
| 1 | 응답 키 이름 | `admin` 블록 | 사용자 로그인 `member` / 관리자 `admin` **구분 유지** | ✅ 합의 |
| 2 | `adminRole` 노출 | 단일 문자열 | **단일 역할 문자열**(배열 미사용) | ✅ 합의 |
| 3 | refreshToken 전달 | body | 앱·웹 **모두 body**로 전달(HttpOnly 쿠키 미사용) | ✅ 합의 |
| 4 | 403 권한부족 UX | ErrorCode+message | **별도 안내 화면 없이 ErrorCode 그대로 표시**(`error.code`/`message` 명확화) | ✅ 합의 |
| 5 | 토큰 만료 | 30분(사용자 동일) | 사용자와 동일 — **access 30분 / refresh 14일** | ✅ 합의 |
| 0 | 엔드포인트 경로 | 신규 `admin/auth/kakao` | **신규 `admin/auth/kakao` 채택** | ✅ 확정(2026-06-10 팀 결정) |
