# AI Asset HIDE 시 verse_explanations 노출본 비활성화

## Summary

- 브랜치: `feature/ai-asset-hide-verse-explanation-unpublish`
- PR 대상: `dev`
- 목표는 `EXPLANATION + BIBLE_VERSE` AI asset을 `HIDDEN` 처리할 때, 해당 asset이 게시한 `verse_explanations` 노출본도 사용자 조회에서 빠지도록 비활성화하는 것이다.
- 신규 HTTP API, OpenAPI schema, DB migration은 없다. 기존 `hide` API의 내부 동작만 보강한다.
- 구현 후 report는 `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-asset-hide-verse-explanation-unpublish_report.md`에 남긴다.

## Key Changes

- `study.api`에 `HidePublishedVerseExplanationUseCase`와 command/result DTO를 추가한다.
- `VerseExplanation` 비활성화 정책은 `status=HIDDEN` + `activeUniqueKey=null`로 고정한다.
- `VerseExplanationService`는 `aiAssetId`로 연결된 `APPROVED + ACTIVE` 해설을 pessimistic lock으로 조회하고 숨김 처리한다.
- 연결 row가 없으면 실패가 아니라 no-op으로 `hiddenCount=0`을 반환한다.
- `AiAssetReviewService`는 `HIDE` 성공 후 `EXPLANATION + BIBLE_VERSE` asset에 한해 study hide use case를 호출한다.
- AI 도메인은 계속 `study.api`만 import하고 `study.internal`을 직접 import하지 않는다.

## Exclusions

- 신규 API/OpenAPI schema/DB schema/Flyway migration 없음.
- 별도 unhide/restore 기능 없음.
- 신규 audit action type 없음. 기존 `AI_ASSET_HIDE` 감사 로그를 유지한다.
- `QT_PASSAGE`, `SIMULATOR`, glossary term 게시 연결은 제외한다.

## Test Plan

- `VerseExplanationServiceTest`: 연결된 `APPROVED + ACTIVE` row가 `HIDDEN + activeUniqueKey=null`로 전환되고 `hiddenCount=1`인지 검증한다.
- `VerseExplanationServiceTest`: 연결 row가 없으면 no-op `hiddenCount=0`인지 검증한다.
- `VerseExplanationReadModelTest`: hide 후 사용자 조회에서 빠지고 같은 verse의 후속 승인본 publish가 가능한지 검증한다.
- `AiAssetReviewServiceTest`: `HIDE`가 `EXPLANATION + BIBLE_VERSE` asset에서 study hide use case를 호출하고 audit을 남기는지 검증한다.
- `AiAssetReviewServiceTest`: 게시 대상이 아닌 asset hide에서는 study hide use case를 호출하지 않는지 검증한다.
- `AiAssetReviewFlowIntegrationTest`: approve로 visible 해설 생성 후 hide하면 visible 목록이 empty가 되고 row 상태가 `HIDDEN + activeUniqueKey=null`인지 검증한다.

## Verification

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AiAssetReviewServiceTest"
.\gradlew.bat test --tests "*AiAssetReviewFlowIntegrationTest"
.\gradlew.bat test --tests "*VerseExplanation*"
.\gradlew.bat test --tests "*AdminAiAssetControllerTest"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai
```

## Assumptions

- `HIDE`된 AI asset의 게시 해설은 운영상 숨김 상태로 봐야 하므로 `VerseExplanationStatus.HIDDEN`을 사용한다.
- `activeUniqueKey`를 반드시 `null`로 내려 unique key를 해제한다.
- 연결된 해설이 없는 HIDE는 실패가 아니라 no-op으로 처리한다.
