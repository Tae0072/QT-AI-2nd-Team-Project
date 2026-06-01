# 리포트 — 관리자 신고 처리 API

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 브랜치: `feature/admin-report-processing` → PR 대상 `dev`
- 관련: 신고 접수(#140) 후속 · API 명세서 §4.7.4 · 연결 화면 AD-04

## 1. 한 줄 요약

접수된 신고를 관리자(OPERATOR/SUPER_ADMIN)가 조회·처리하는 API를 구현했다. 목록 조회(필터·페이징) + resolve/reject(상태 전이·처리자 기록). 전체 회귀 통과.

## 2. API (명세 §4.7.4)

| Method | URL | 권한 |
|--------|-----|------|
| GET | `/api/v1/admin/reports?status=&targetType=&page=&size=` | OPERATOR/SUPER_ADMIN |
| POST | `/api/v1/admin/reports/{reportId}/resolve` | OPERATOR/SUPER_ADMIN |
| POST | `/api/v1/admin/reports/{reportId}/reject` | OPERATOR/SUPER_ADMIN |

- resolve/reject 본문: `{ action, reason, notifyReporter }`
- 처리 = 상태 전이(RECEIVED/REVIEWING → RESOLVED/REJECTED) + `processed_by_admin_id`/`processed_at` 기록
- 권한: ROLE_ADMIN 1차 + `ADMIN_ROLE_OPERATOR|SUPER_ADMIN` 2차 검증(부족 시 `ADMIN_ROLE_INSUFFICIENT` 403)

## 3. 변경 파일

| 구분 | 파일 |
|------|------|
| 수정 | `report/internal/Report.java` (process/isClosed mutator), `ReportRepository.java`(findForAdmin 페이징 쿼리), `common/exception/ErrorCode.java`(REPORT_NOT_FOUND R0003, REPORT_ALREADY_PROCESSED R0004) |
| 신규 | `report/api/ListAdminReportsUseCase`, `ProcessReportUseCase` + dto 4종(Query/ListResponse/Command/Result) |
| 신규 | `report/internal/AdminReportService`(impl), `report/web/AdminReportController` |
| 신규 | 테스트 2종(AdminReportServiceTest 6건, AdminReportControllerTest 5건 — 목록·resolve·reject·403 2건) |

## 4. 검증

- `./gradlew test --no-daemon` (전체) → BUILD SUCCESSFUL. 서비스 단위(필터·처리·예외) + 컨트롤러 슬라이스(권한 403 포함) 통과.

## 5. 범위 / 후속

- 본 구현: **목록 + 상태 처리(처리자/시각 기록)**까지.
- **후속(cross-domain)**: ① `action=HIDE_TARGET` 실제 대상 숨김(sharing/ai 연계) ② 신고자 알림(notification, REPORT_RESULT) ③ 감사 로그(audit_logs) 기록 — 현재는 처리 로그만 남김.
- 회원 제재(§4.7.5 suspend/activate)는 별도 작업.
