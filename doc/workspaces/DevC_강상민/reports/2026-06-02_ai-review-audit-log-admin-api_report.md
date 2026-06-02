# AI 검수 감사 로그 관리자 조회 API 구현 리포트

## Summary

- 브랜치: `feature/ai-review-audit-log-admin-api`
- PR 대상: `dev`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-review-audit-log-admin-api.md`
- 신규 endpoint: `GET /api/v1/admin/audit-logs`
- 신규 DB schema/migration: 없음

## 구현 결과

- `audit.api`의 조회 TODO 계약을 실제 `ListAuditUseCase`와 DTO 계약으로 교체했다.
- 관리자 audit 조회 controller를 `/api/v1/admin/audit-logs`에 추가했다.
- 권한은 controller와 service 양쪽에서 `ADMIN + OPERATOR/REVIEWER/SUPER_ADMIN`으로 검증한다.
- 조회 범위는 AI audit으로 제한했다.
  - actionType: `AI_ASSET_APPROVE`, `AI_ASSET_REJECT`, `AI_ASSET_HIDE`, `AI_REGENERATE_REQUEST`
  - targetType: `AI_GENERATED_ASSET`
- `actionType` 또는 `targetType`이 없으면 위 AI 범위를 기본 필터로 적용한다.
- `from/to`는 KST `yyyy-MM-dd` 날짜로 받아 `createdAt` 기준 `from inclusive`, `to 다음 날 00:00 exclusive`로 조회한다.
- 응답은 page envelope이며 `beforeJson/afterJson`은 저장된 sanitize snapshot 문자열만 그대로 반환한다.
- 기존 placeholder `AuditController`와 `AuditLogResponse`는 실제 controller/DTO로 교체했다.
- OpenAPI와 `04_API_명세서.md`에 신규 endpoint, query params, response schema, AI 범위 제한을 반영했다.

## 테스트

- `AdminAuditLogControllerTest`
  - query param mapping과 page envelope 응답을 검증했다.
  - anonymous, USER, CONTENT_CREATOR 차단과 OPERATOR/REVIEWER/SUPER_ADMIN 허용을 검증했다.
- `AuditQueryServiceTest`
  - service 권한 재검증, AI action/target 범위 제한, 기본 AI 범위 적용, 날짜/page/size validation을 검증했다.
- `AuditQueryRepositoryTest`
  - actor/action/target/date 필터와 `createdAt desc, id desc` 정렬, pagination, total count를 검증했다.
- 기존 `AiAssetReviewServiceTest`, `AiServiceTest`에서 저장되는 audit action이 조회 허용 action과 일치함을 회귀 확인했다.

## 검증 결과

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AdminAuditLog*"
.\gradlew.bat test --tests "*Audit*"
.\gradlew.bat test --tests "*AiAssetReview*"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/audit qtai-server/src/main/java/com/qtai/domain/ai
npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml
npx.cmd js-yaml qtai-server/apis/api-v1/openapi.yaml > $null
```

- `.\gradlew.bat test --tests "*AdminAuditLog*"`: PASS
- `.\gradlew.bat test --tests "*Audit*"`: PASS
- `.\gradlew.bat test --tests "*AiAssetReview*"`: PASS
- `.\gradlew.bat build`: PASS
- `git diff --check`: PASS
- 금지 import 검색: PASS, 매칭 없음
- Spectral lint: ruleset 파일이 없어 실행 불가
- OpenAPI YAML parse: PASS

## 범위 제외

- 전체 도메인 audit 조회는 구현하지 않았다.
- audit export, 상세 diff viewer는 구현하지 않았다.
- audit 조회 자체에 대한 감사 로그 write는 추가하지 않았다.
- raw prompt/provider response/payload/reason 원문 또는 secret/token/password 계열 값은 새로 저장하거나 응답으로 복원하지 않았다.
