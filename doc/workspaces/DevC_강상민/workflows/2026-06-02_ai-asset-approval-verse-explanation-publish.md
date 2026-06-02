# AI 산출물 승인 시 verse_explanations 노출본 연결

## Summary

- 브랜치: `feature/ai-asset-approval-verse-explanation-publish`
- PR 대상: `dev`
- 목표는 관리자 검수 API 전체(`approve/reject/hide`)를 구현하고, `EXPLANATION + BIBLE_VERSE` 산출물 승인 시 `verse_explanations` 사용자 노출본으로 연결하는 것이다.
- 신규 DB schema/migration은 없다. 기존 `verse_explanations.active_unique_key` 유니크 정책을 사용한다.
- 구현 후 report는 `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-asset-approval-verse-explanation-publish_report.md`에 남긴다.

## Key Changes

- `POST /api/v1/admin/ai/assets/{assetId}/approve|reject|hide`를 추가한다.
- 권한은 `ADMIN + REVIEWER/SUPER_ADMIN`으로 제한하고 service layer에서도 재검증한다.
- `ReviewAiAssetUseCase` command에 `memberRole`, `adminRole`, `activateForTarget`를 포함한다.
- APPROVE는 asset `VALIDATING`, active checklist, 최신 validation log `PASSED` 조건에서만 허용한다.
- REJECT는 `VALIDATING -> REJECTED`, HIDE는 `VALIDATING|APPROVED -> HIDDEN`만 허용한다.
- 승인/반려/숨김은 `AI_ASSET_APPROVE`, `AI_ASSET_REJECT`, `AI_ASSET_HIDE` 감사 로그를 남긴다.
- AI 도메인은 `study.internal`을 직접 import하지 않고 `study.api`의 publish UseCase를 통해 공개 해설을 연결한다.
- `EXPLANATION + BIBLE_VERSE + activateForTarget=true` 승인 시에만 payload의 matching verse 항목을 `verse_explanations`에 게시한다.

## Exclusions

- 신규 DB schema, Flyway migration 없음.
- `QT_PASSAGE` asset의 다중 절 게시, SIMULATOR 공개 연결, glossary term 게시 연결은 제외한다.
- raw payload, prompt/provider raw response, reason 원문은 audit snapshot에 저장하지 않는다.

## Test Plan

- `AdminAiAssetControllerTest`: approve/reject/hide 경로, request mapping, 권한/오류 응답 검증.
- `AiAssetReviewServiceTest`: strict approve gate, publish 호출 조건, 상태 전이, 감사 로그 검증.
- `VerseExplanationServiceTest` 및 repository slice test: 기존 ACTIVE 비활성화 후 새 APPROVED ACTIVE 저장 검증.
- 통합 테스트: AI asset approve 후 승인 해설 조회 UseCase에서 새 노출본 반환 검증.

## Verification

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AdminAiAssetControllerTest"
.\gradlew.bat test --tests "*AiAssetReview*"
.\gradlew.bat test --tests "*VerseExplanation*"
.\gradlew.bat test --tests "*AiUseCaseContractTest"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai
```

## Assumptions

- 승인 연결은 최신 절 단위 시딩 정책에 맞춰 `BIBLE_VERSE` 대상만 처리한다.
- 승인 실패 사유는 새 ErrorCode를 늘리지 않고 기존 `CHECKLIST_NOT_FOUND`, `INVALID_INPUT`, `INVALID_STATUS_TRANSITION` 중심으로 매핑한다.
- `verse_explanations` schema는 이미 필요한 컬럼과 unique key가 있으므로 migration은 추가하지 않는다.
