# Report - 2026-06-10 admin-dashboard-api

## Summary

- Branch: `feature/admin-dashboard-api-clean`
- Target: `dev-msa`
- Workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-10_admin-dashboard-api.md`
- Scope: `admin-server` AD-01 `GET /api/v1/admin/dashboard`
- Excluded: `admin-web` screen/API client changes

## Implementation

- Added `GET /api/v1/admin/dashboard`.
- Response uses `ApiResponse<AdminDashboardResponse>` envelope.
- Authorization:
  - 1st gate: `ROLE_ADMIN` via `@PreAuthorize("hasRole('ADMIN')")`
  - 2nd gate: `VerifyAdminRoleUseCase.verifyAnyRole(memberId, ["OPERATOR", "REVIEWER"])`
  - `SUPER_ADMIN` passes through existing superiority behavior.
  - `CONTENT_CREATOR` is rejected with 403.
- AI count:
  - Reuses existing `GetAdminAiMonitoringUseCase`.
  - `pendingAiValidationCount` maps from `AdminAiMonitoringResponse.validation.waitingAssets`.
  - Null AI validation response is guarded as `0`.
- Report count:
  - `receivedReportCount = ReportStatus.RECEIVED count`
  - `reviewingReportCount = ReportStatus.REVIEWING count`
- Today QT:
  - `todayQt` is always non-null.
  - Missing rule returns `status=MISSING`, `qtDate=KST today`, nullable QT fields as null, `hasExplanation=false`.
  - `TodayQtStatus` enum is used for `READY/MISSING`.
  - `simulatorStatus` remains separate from dashboard QT availability status.
  - `MISS` from QT cache is treated as missing when `qtPassageId` is absent.
- Recent audit logs:
  - Dashboard-only sanitized DTO is used.
  - Included fields: `id`, `adminUserId`, `actorType`, `actionType`, `targetType`, `targetId`, `createdAt`.
  - Excluded fields: `beforeJson`, `afterJson`, AI payload, prompt/provider raw text, reason raw text.
  - Repository projection excludes sensitive columns at query level.
- OpenAPI:
  - Added `/api/v1/admin/dashboard`.
  - Added `AdminDashboardApiResponse` and related schemas.

## Tests

- `AdminDashboardControllerTest`
  - OPERATOR/REVIEWER/SUPER_ADMIN 200
  - CONTENT_CREATOR 403
  - ROLE_USER 403
  - Unauthenticated 401/403
  - `AdminController` class-level `@PreAuthorize("hasRole('ADMIN')")`
  - `resolveMemberId` Number/CharSequence/fallback/invalid principal branches
- `AdminDashboardServiceTest`
  - AI waitingAssets mapping
  - AI validation null and zero boundary
  - RECEIVED/REVIEWING report count mapping
  - todayQt null/EMPTY/MISS/READY behavior
  - invalid memberId unauthorized path
  - sanitized audit log mapping
- `AdminReportDashboardSummaryServiceTest`
  - RECEIVED/REVIEWING count mapping
- `AdminDashboardAuditLogServiceTest`
  - sanitized DTO mapping
  - `Pageable` sort arguments: `createdAt DESC`, `id DESC`
- `AuditQueryRepositoryTest`
  - JPA integration test for dashboard projection
  - verifies sensitive JSON columns are not exposed through projection row

## Verification

| Command | Result |
| --- | --- |
| `./gradlew.bat :admin-server:test --tests "*AdminDashboard*" --tests "*AuditQueryRepositoryTest" --tests "*AdminDashboardAuditLogServiceTest" --tests "*AdminReportDashboardSummaryServiceTest"` | PASS |
| `./gradlew.bat :admin-server:build` | PASS |
| `git diff --check` | PASS, CRLF warnings only |
| Python YAML parse + AD-01 path/schema check | PASS |
| `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | NOT RUN TO COMPLETION: `.spectral.yaml` is absent from repository root and `qtai-server/` |

## Spectral Follow-up

Local Spectral lint is blocked because the ruleset file is not present in this checkout. This should be handled as an infra/ruleset follow-up: restore/provide `.spectral.yaml`, or update CI/PR guidance to the correct ruleset path. The OpenAPI YAML itself was parsed successfully and the AD-01 path/schema existence was verified.

## Notes

- `GetTodayQtUseCase.getToday(null)` is used intentionally for dashboard summary because the current UseCase contract allows `memberId` null when draft note lookup is not needed.
- `STALE_FALLBACK` is returned as READY when a QT passage is actually present. The cache status remains exposed as `cacheStatus` so operators can see the fallback condition.
- `admin-web` remains out of scope.
