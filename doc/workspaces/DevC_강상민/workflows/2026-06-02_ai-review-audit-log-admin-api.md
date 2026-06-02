# AI 검수 감사 로그 관리자 조회 API

## Summary

- 브랜치: `feature/ai-review-audit-log-admin-api`
- PR 대상: `dev`
- 목표는 이미 저장 중인 `AI_ASSET_APPROVE`, `AI_ASSET_REJECT`, `AI_ASSET_HIDE`, `AI_REGENERATE_REQUEST` audit 로그를 관리자 웹에서 조회할 수 있게 하는 것이다.
- 신규 endpoint는 `GET /api/v1/admin/audit-logs`로 구현하되, 이번 PR에서는 AI 관련 action/target 조회만 지원한다.
- 신규 DB schema/Flyway migration은 없다. 기존 `audit_logs`만 조회한다.
- 구현 후 report는 `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-review-audit-log-admin-api_report.md`에 남긴다.

## Key Changes

- `audit.api`의 TODO 계약을 실제 조회 계약으로 정리한다.
- Query params: `actorType`, `actorId`, `actionType`, `targetType`, `targetId`, `from`, `to`, `page`, `size`.
- 기본 sort는 `createdAt,desc,id,desc`로 고정한다.
- `from/to`는 KST 날짜 문자열 `yyyy-MM-dd`로 받고 `createdAt` 기준 기간 필터를 적용한다.
- 응답은 기존 관리자 목록 API와 같은 page envelope를 사용한다.
- 권한은 `ADMIN + OPERATOR/REVIEWER/SUPER_ADMIN`로 두고 service layer에서도 재검증한다.
- 이번 PR의 조회 대상은 AI audit으로 제한한다.
  - actionType: `AI_ASSET_APPROVE`, `AI_ASSET_REJECT`, `AI_ASSET_HIDE`, `AI_REGENERATE_REQUEST`
  - targetType: `AI_GENERATED_ASSET`
- `AuditController`는 문서 기준 경로인 `/api/v1/admin/audit-logs`로 구현한다.
- OpenAPI와 `04_API_명세서.md`에 endpoint, query params, response schema, AI 범위 제한을 반영한다.

## Exclusions

- 전체 도메인 audit 조회, export, 상세 diff viewer는 제외한다.
- audit 조회 자체의 감사 로그 write는 제외한다.
- 신규 DB schema/migration은 없다.
- raw prompt/provider response/reason 원문을 새로 저장하거나 복원하지 않는다.

## Test Plan

- `AdminAuditLogControllerTest`: 권한, query mapping, 401/403 검증.
- `AuditQueryServiceTest`: 권한 재검증, 날짜/page/size 검증, AI action/target 제한 검증.
- `AuditQueryRepositoryTest`: 필터, KST 기간, 정렬, pagination, 저장 snapshot 반환 검증.
- `AiAssetReviewServiceTest`: approve/hide audit action이 조회 대상 action과 일치하는지 회귀 검증 유지.

## Verification

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AdminAuditLog*"
.\gradlew.bat test --tests "*Audit*"
.\gradlew.bat test --tests "*AiAssetReview*"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/audit qtai-server/src/main/java/com/qtai/domain/ai
```

## Assumptions

- 이번 PR은 AI 검수/재생성 audit 조회만 다룬다.
- 다른 도메인 audit 조회는 별도 SUPER_ADMIN 정책 PR로 둔다.
- `OPERATOR`, `REVIEWER`, `SUPER_ADMIN`은 AI 운영 추적 목적으로 조회 가능하다.
