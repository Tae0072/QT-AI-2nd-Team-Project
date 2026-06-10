# 2026-06-10 user→admin 검증 RestClient 통합 (task 1)

## 목표
service-user가 사용자의 관리자 자격을 확인할 때 쓰는 `VerifyAdminRoleUseCase`의 임시 Mock을 **실제 RestClient(admin-server 호출) 어댑터로 교체**한다. admin은 admin-server 소유이므로 service-user는 api 계약만 client 어댑터로 호출한다(CLAUDE.md §4). task 3(관리자 카카오 인증)의 admin 자격 검증·task 2(retention) 선행.

## 배경 (dev-msa)
- 단일 DB·RestClient(동기) MSA. 서비스 간 시스템 호출은 공유 HS256 단명 SYSTEM_BATCH 토큰(`SystemTokenProvider`/admin-server `JwtAuthenticationFilter` 검증).
- 기존: service-user `member/client/admin/VerifyAdminRoleUseCaseMock`(3메서드 모두 `ADMIN_USER_NOT_FOUND`). admin-server엔 `AdminController GET /me`(로그인 관리자 본인)만 있고 **임의 memberId 검증 시스템 엔드포인트 부재** → 양쪽 신설/교체 필요.
- 패턴 기준: #446 service-ai→audit, `PurgeMember*RestClientAdapter`(service-user 인-모듈), `GetBibleVerseRestClientAdapter`(데이터 반환+언랩).

## 작업 내용
### 1) admin-server 수신 — `SystemAdminVerifyController`
- `GET /api/v1/system/admin/verify?memberId={}&requiredRoles={csv}`. `requiredRoles` 없으면 `getActiveAdmin`, 있으면 `verifyAnyRole`(SUPER_ADMIN 우월) 위임 → `ApiResponse<AdminUserInfo>`.
- 경로 `/api/v1/system/**` = `SecurityConfig`의 `hasRole("SYSTEM_BATCH")`로 보호(일반/ADMIN 403, 미인증 401/403). 비즈니스 예외(AD0001/2/3)는 `GlobalExceptionHandler`가 `status(httpStatus)+ApiResponse.error(code)`로 변환.

### 2) service-user 소비 — `VerifyAdminRoleRestClientAdapter` (Mock 교체)
- `RestClient.Builder` + `ServiceEndpointsProperties.getAdminBaseUrl()` + `SystemTokenProvider`(ObjectProvider, 미설정 시 호출 시점 EXTERNAL_API_FAILURE) + `ObjectMapper`.
- 3메서드 모두 `GET /verify`로 위임(getActiveAdmin=역할 없음, verifyRole=[role], verifyAnyRole=roles). `ApiResponse<AdminUserInfo>` 언랩.
- **오류 정밀 역매핑**: admin 실패 3종이 전부 403이라, 응답 envelope `error.code`(AD0001/2/3)를 읽어 `ADMIN_USER_NOT_FOUND`/`ADMIN_USER_DISABLED`/`ADMIN_ROLE_INSUFFICIENT`로 복원(in-process 예외 계약 보존). 그 외·연결 실패는 `EXTERNAL_API_FAILURE`.
- 기존 `VerifyAdminRoleUseCaseMock` 삭제(참조처 0 확인).

## 범위
- 브랜치: `feature/admin-verify-restclient` (base: `dev-msa`)
- 변경: admin-server 컨트롤러 1 + 테스트 1 / service-user 어댑터 1(+Mock 삭제) + 테스트 1.

## 검증
- `:service-user:test --tests VerifyAdminRoleRestClientAdapterTest` — **8건 0 실패**(정상 언랩·requiredRoles 쿼리·AD0001/2/3·미지정코드·5xx·토큰 미설정).
- `:admin-server:test --tests SystemAdminVerifyControllerTest` — **5건 0 실패**(SYSTEM_BATCH 200·역할검증 200·관리자아님 403+AD0001·USER 403·미인증 차단).
- Mock 삭제 후 service-user 컴파일 정상(어댑터가 유일 구현).

## 미해결 / 후속
- **task 3**: `POST /api/v1/admin/auth/kakao` — 본 어댑터로 admin 자격 검증 후 ADMIN 토큰 발급. task3 진입 전 잠금 3건(prod flyway 명시/DEV_ADMIN_ROLE→OPERATOR/ErrorCode 04명세).
- **task 2**: `MemberRetentionPurgeBatch`에 `qtai.retention.purge.enabled` 게이트 추가 후, 본 검증 통합으로 admin 연결 회원을 정확히 식별 가능해지면 활성화.

## 담당
- DevD 이승욱 (service-user 인증 BE)
