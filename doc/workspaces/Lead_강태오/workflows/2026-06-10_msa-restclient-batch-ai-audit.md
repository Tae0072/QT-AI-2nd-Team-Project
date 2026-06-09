# 2026-06-10 MSA 배치 RestClient ③ service-ai → audit(admin-server) 워크플로우

> 작업 브랜치: `feature/msa-restclient-batch` (origin/dev-msa 기준)
> 선행: PR #441/#443/#444(ai→bible/qt/study), #445(retention purge). 시스템 토큰 패턴 재사용.

## 목표

service-ai의 AI 검수·생성 작업이 감사 로그를 기록할 때 쓰는 `WriteAuditLogUseCaseMock`(no-op)을 admin-server(audit 소유)
HTTP 호출 어댑터로 교체한다.

## 범위 한정 결정 (audit만, admin VerifyRole 제외)

상위 ③은 "ai→audit/admin"이지만, 조사 결과:
- **ai의 `WriteAuditLogUseCase`는 실제 사용**(AiService/AiAssetReviewService/ValidationReferenceJobService/AdminAiValidationChecklistService가 감사 기록) → 어댑터 필요.
- **ai의 `VerifyAdminRoleUseCase`는 호출되지 않는 안전 스텁**: ai의 `/api/v1/admin/**`은 SecurityConfig에서 denyAll이고, Mock은 호출 시 `ADMIN_ROLE_INSUFFICIENT`(403)를 던진다. 진짜 관리자 검증은 admin-server 소관이라 ai에 어댑터를 만들면 **죽은 코드 + 안전 약화**. → **Mock 유지**(전환하지 않음).
- (참고) 실제로 의미 있는 VerifyAdminRole 통합은 service-**user**→admin(retention 가드 `hasAdminLink`)이며, 이는 PR②의 deploy guard 활성화 선행조건으로 별도 트랙.

## admin-server의 특수성과 시스템 토큰 수용 (Lead 결정: lib-common 검증 도입)

admin-server는 **lib-common 비의존 standalone**(모놀리식 통째 복사, #433)이라 lib-common의 `SystemTokenValidator`/
`JwtAuthenticationFilter` 시스템 토큰 폴백(PR #440)이 없다. ai가 lib-common 시스템 토큰으로 호출해도 admin-server가 그
HS256 토큰을 검증할 방법이 없었다. **결정: lib-common과 동일한 HS256 시스템 토큰 검증을 admin-server 자체 보안 패키지에 도입**.

## TODO

- [x] admin-server 보안: `qtai/security/SystemTokenValidator`(HS256, `@ConditionalOnProperty security.jwt.system-secret`) 추가 + `JwtAuthenticationFilter`에 RS256 실패 시 시스템 토큰 폴백(`ObjectProvider`, ROLE_SYSTEM_BATCH)
- [x] admin-server 수신: `audit/web/SystemAuditLogController` `POST /api/v1/system/audit-logs`. SecurityConfig가 이미 `/api/v1/system/** → hasRole("SYSTEM_BATCH")`로 보호.
- [x] **`AdminControllerSurfaceTest` 불변식 갱신**: "admin만 노출" → "admin 또는 system 노출"(근거 주석 추가). admin-server가 audit 소유 → 시스템 배치 수신 엔드포인트는 정당(PR#432 교훈: 불변식 갱신을 코드와 함께 명시).
- [x] ai 어댑터: `ai/client/audit/WriteAuditLogRestClientAdapter`(fire-and-forget) + `WriteAuditLogUseCaseMock` 삭제
- [x] 테스트 — admin: `SystemAuditLogControllerTest`(SYSTEM_BATCH 200·사용자 403·미인증 401/403) + `SystemTokenValidatorTest`(검증·거부 4). ai: `WriteAuditLogRestClientAdapterTest`(정상·5xx 무시·토큰미설정 생략)
- [x] 빌드 `:admin-server:build` + `:service-ai:build` GREEN
- [x] 문서
- [ ] dev-msa 정합 → 커밋·푸시·PR → 리뷰

## 설계 결정·근거

- **audit는 fire-and-forget**(WriteAuditLogUseCase javadoc·횡단 관심사): 전송 실패가 AI 작업을 깨지 않도록 어댑터는 `RestClientException`만 잡아 경고만 남기고 진행한다(no-op Mock의 무해성 유지). 토큰·본문 민감정보 미로깅(§7·§9).
- **수신 보호는 경로 규칙으로**: `/api/v1/system/**`이 이미 `hasRole("SYSTEM_BATCH")`라 컨트롤러에 별도 `@PreAuthorize` 불필요(중복 회피). 시스템 토큰은 admin-server 필터 폴백이 검증.
- **발급기는 admin-server에 두지 않음**: admin-server는 시스템 토큰을 **받기만** 한다(SystemTokenValidator만, Provider 없음).

## 검증

`:admin-server:build` + `:service-ai:build` **BUILD SUCCESSFUL**. 함정: admin-server `AdminControllerSurfaceTest`가 "/api/v1/admin만" 불변식이라 system 경로 추가 시 깨짐 → 불변식을 근거와 함께 확장.

## 결과

배치/시스템 RestClient 소비자 트랙(① ai→bible/qt/study, ② retention purge, ③ ai→audit)이 모두 실제 어댑터로 전환됨. admin-server가 lib-common 시스템 토큰을 수용하게 되어, 향후 user→admin VerifyAdminRole 등 batch 호출의 토대도 마련됨.
