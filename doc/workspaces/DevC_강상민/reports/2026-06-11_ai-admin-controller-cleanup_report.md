# Report - 2026-06-11 ai-admin-controller-cleanup

## Summary

- Branch: `chore/ai-admin-controller-cleanup`
- Target: `dev`
- Workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-11_ai-admin-controller-cleanup.md`
- Scope: `service-ai`의 봉인된 `/api/v1/admin/ai/**` 관리자 HTTP 진입점 제거
- Excluded: TODO 2/3/4, `admin-server`, `admin-web`

## Implementation

- Removed service-ai admin AI controllers:
  - `AdminAiAssetController`
  - `AdminAiMonitoringController`
  - `AdminAiBatchRunLogController`
  - `AdminAiValidationChecklistController`
- Removed controller-only web helper/request/response types:
  - `AdminAiAuthentication`
  - `AdminAiValidationChecklistRequest`
  - `RegenerateAiAssetRequest`
  - `RegenerateAiAssetResponse`
- Removed unused service-ai admin auth client/mock remnants:
  - `AdminAuthClient`
  - `AdminAuthClientMock`
  - `VerifyAdminRoleUseCaseMock`
- Kept `SecurityConfig` `/api/v1/admin/** denyAll` and clarified that admin AI APIs are served by `admin-server`.
- Updated `AiForbiddenFeatureTest` to fail if any service-ai web controller exposes `/api/v1/admin/**`.
- Removed service-ai OpenAPI admin asset review paths, `AI Admin` tag, admin auth dependency, and review schemas from `qtai-server/apis/ai-service/openapi.yaml`.
- Did not modify `admin-server` or `admin-web`.

## Tests

- `AiForbiddenFeatureTest`
  - Existing forbidden AI path and streaming return type checks remain.
  - Added service-ai admin path exposure guard.
- `SecurityFilterChainTest`
  - Existing `/api/v1/admin/** denyAll` checks remain unchanged.

## Verification

| Command | Result |
| --- | --- |
| `.\gradlew.bat :service-ai:test --tests "*AiForbiddenFeatureTest" --tests "*SecurityFilterChainTest"` | PASS |
| `.\gradlew.bat :service-ai:build` | PASS |
| Python YAML parse + no service-ai admin path/tag/review schema check | PASS |
| `git diff --check` | PASS, CRLF normalization warnings only |
| `npx @stoplight/spectral-cli lint qtai-server/apis/ai-service/openapi.yaml --ruleset .spectral.yaml` | NOT RUN: `.spectral.yaml` is absent from repository root and `qtai-server/` |

## Acceptance Criteria

- service-ai에 `/api/v1/admin/ai/**` 컨트롤러가 남지 않음: 충족.
- service-ai 전용 OpenAPI에 `AI Admin` tag, 관리자 asset review path/schema가 남지 않음: 충족.
- `/api/v1/admin/** denyAll` 정책 유지: 충족.
- `admin-server`와 `admin-web` 미수정: 충족.
- 지정 테스트와 빌드 통과: 충족.

## Notes

- `domain.ai.api.admin/**` UseCase/DTO와 내부 AI 검수/생성 서비스는 유지했다. 이번 작업은 호출 불가능한 service-ai HTTP 관리자 진입점과 그 전용 client/mock 잔재 제거에 한정했다.
