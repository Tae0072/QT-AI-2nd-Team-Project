# 2026-06-10 user→admin 검증 RestClient 통합 (task 1) — 결과 보고

## 요약
service-user의 `VerifyAdminRoleUseCase` 임시 Mock을 실제 RestClient 어댑터로 교체하고, admin-server에 시스템 수신 엔드포인트를 신설했다. 양쪽 동봉(RestClient PR 관례). 단일 DB·SYSTEM_BATCH 토큰·정밀 오류 역매핑. 동작은 Mock(전부 거부) → 실제 admin-server 검증으로 전환.

## 산출물
| 파일 | 설명 |
|------|------|
| `admin-server/.../admin/web/SystemAdminVerifyController.java` | (신규) `GET /api/v1/system/admin/verify` — SYSTEM_BATCH 수신, VerifyAdminRoleUseCase 위임 |
| `admin-server/.../admin/web/SystemAdminVerifyControllerTest.java` | (신규) 5건 — 200/역할/403 AD0001/USER 403/미인증 |
| `service-user/.../member/client/admin/VerifyAdminRoleRestClientAdapter.java` | (신규) Mock 대체 — 시스템 토큰·언랩·AD0001/2/3 역매핑 |
| `service-user/.../member/client/admin/VerifyAdminRoleUseCaseMock.java` | (삭제) |
| `service-user/.../client/admin/VerifyAdminRoleRestClientAdapterTest.java` | (신규) 8건 |

## 변경 성격
- **Strangler 통합**: 임시 Mock → 실제 HTTP. admin은 admin-server 소유, service-user는 api 계약만 호출(도메인 경계 준수).
- **시스템 인증**: 사용자 JWT 없는 경로(로그인 중·배치)라 SYSTEM_BATCH 토큰 Bearer. 수신은 `/api/v1/system/**` SYSTEM_BATCH 전용.
- **오류 충실성**: 3종(403) 구분 위해 응답 코드 기반 역매핑 — 코스 매핑(EXTERNAL_API_FAILURE 일괄)보다 계약 충실.

## 검증
- 어댑터 8건 + 컨트롤러 5건 = **13건 0 실패**. 부정 경로(미지정코드·5xx·토큰미설정·USER 403·미인증) 포함.
- 2층 셀프 점검: CI 게이트(브랜치/금지패턴/시크릿) + 리뷰 9기준(도메인 경계·시스템토큰·오류 역매핑 부정경로·로그 민감정보 없음) PASS.

## 미해결 / 후속
- task 3(관리자 카카오 인증)에서 본 어댑터 소비. task 2(retention 게이트) 선행 충족.
- (선택) admin status 비활성 케이스 통합 시드 검증 — admin-server 실DB 경로(현재는 UseCase mock으로 컨트롤러만).
