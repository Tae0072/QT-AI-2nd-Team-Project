# Report - 2026-06-11 admin-ad0106-response-contract

## Summary

- Branch: `docs/admin-ad0106-contract`
- Target: `dev`
- Workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-11_admin-ad0106-response-contract.md`
- Scope: 코드리뷰 TODO 2, AD-01/AD-06 응답 계약 확정 문서
- Output: `doc/workspaces/DevC_강상민/contracts/2026-06-11_admin-ad0106-response-contract.md`

## Implementation

- AD-01 `GET /api/v1/admin/dashboard` 응답 필드, 타입, nullable 규칙을 정리했다.
- AD-06 공지 목록/생성/수정/발행/숨김 API의 request/response/status code를 정리했다.
- 공통 `ApiResponse<T>` envelope와 AD-06 목록 페이지 봉투(`AdminNoticeListResponse`)를 명시했다.
- admin-web 반영 포인트를 김지민 전달용으로 정리했다.

## Source Checked

- `doc/workspaces/DevC_강상민/2026-06-10_코드리뷰_TODO_강상민.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-10_admin-dashboard-api_report.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-10_admin-notices-api_report.md`
- `qtai-server/apis/api-v1/openapi.yaml`
- `AdminDashboardResponse`, `AdminNoticeListResponse`, `AdminNoticeDetailResponse`, `AdminNoticePublishResponse`, `AdminNoticeController`

## Verification

- OpenAPI에서 AD-01 schema/path 존재 확인.
- OpenAPI에서 AD-06 notice schema/path/status code 존재 확인.
- DTO 레코드와 문서 필드 대조 완료.

## Follow-up

- 김지민 admin-web 타입/API client 반영 PR에서 실데이터 렌더링을 공동 확인한다.
- 공지 숨김 `204 No Content` 처리와 AD-01 nullable 필드 렌더링을 우선 확인한다.
