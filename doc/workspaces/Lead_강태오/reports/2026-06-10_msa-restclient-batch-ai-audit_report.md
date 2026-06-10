# 리포트 — MSA 배치 RestClient ③ service-ai → audit(admin-server)

작성일: 2026-06-10 / 작성: Claude (Lead 강태오) / 브랜치: `feature/msa-restclient-batch`

## 1. 요약

service-ai의 감사 기록 `WriteAuditLogUseCaseMock`(no-op)을 admin-server HTTP 어댑터로 교체했다. admin-server는
lib-common 비의존이라 lib-common과 동일한 HS256 시스템 토큰 검증을 admin-server 자체 보안 패키지에 도입했고(Lead 결정),
이미 존재하던 `/api/v1/system/** → hasRole("SYSTEM_BATCH")` 규칙 아래 수신 엔드포인트를 신설했다. 배치 RestClient ①②③ 트랙 완료.

## 2. 변경 내역

### 신규 (admin-server)
- `security/SystemTokenValidator` — HS256 공유 시크릿 검증(type=system, role=SYSTEM_BATCH), `@ConditionalOnProperty security.jwt.system-secret`. Provider는 두지 않음(받기만).
- `audit/web/SystemAuditLogController` — `POST /api/v1/system/audit-logs` → `WriteAuditLogUseCase.write`.

### 수정 (admin-server)
- `security/JwtAuthenticationFilter` — RS256 사용자 검증 실패 시 `ObjectProvider<SystemTokenValidator>` 폴백(ROLE_SYSTEM_BATCH, 둘 다 실패만 401). lib-common PR #440과 동일 패턴.
- `AdminControllerSurfaceTest` 불변식 — "admin만 노출" → "admin 또는 system 노출"(근거 주석: admin-server가 audit 소유 → 시스템 배치 수신 정당).

### 신규 (service-ai)
- `ai/client/audit/WriteAuditLogRestClientAdapter` — admin-server `/api/v1/system/audit-logs` POST, 시스템 토큰, **fire-and-forget**(실패·토큰미설정에도 예외 없이 경고만).

### 삭제 (service-ai)
- `WriteAuditLogUseCaseMock`.

### 테스트
- admin: `SystemAuditLogControllerTest` 3(SYSTEM_BATCH 200·사용자 403·미인증 401/403), `SystemTokenValidatorTest` 4(검증·시크릿/타입/role 거부).
- ai: `WriteAuditLogRestClientAdapterTest` 3(정상·5xx 무시·토큰미설정 생략).

## 3. 설계 결정

| 결정 | 근거 |
|---|---|
| audit만, ai VerifyAdminRole Mock 유지 | ai는 VerifyAdminRole 미호출(/admin denyAll). 전환 시 죽은 코드·안전 약화. 의미 있는 통합은 user→admin(별도). |
| admin-server에 lib-common 동일 HS256 검증 도입 | admin-server 비의존이라 시스템 토큰 검증 부재 → 동일 규약 검증기를 자체 패키지에 추가(Lead 결정). 발급기는 미도입. |
| 수신 보호는 `/api/v1/system/**` 경로 규칙 | SecurityConfig가 이미 hasRole(SYSTEM_BATCH) → @PreAuthorize 중복 불필요. |
| audit fire-and-forget | 횡단 관심사. 전송 실패가 AI 작업을 깨지 않게 RestClientException만 잡고 경고만(§9). |
| 불변식 테스트 명시적 갱신 | PR#432 교훈 — surface 불변식을 코드와 함께 근거 주석으로 확장. |

## 4. 검증

`:admin-server:build` + `:service-ai:build` **BUILD SUCCESSFUL**.

## 5. 리스크 & 후속

- 배포 env `SECURITY_JWT_SYSTEM_SECRET`를 admin-server에도 주입해야 시스템 토큰 검증 활성(미설정 시 SystemTokenValidator 빈 미등록 → 폴백 비활성, 시스템 호출 401). 배포 세션 정합.
- 후속(별도): service-user→admin `VerifyAdminRoleUseCase` 실HTTP 통합 → retention deploy guard(`qtai.retention.purge.enabled`) 활성화 가능.
