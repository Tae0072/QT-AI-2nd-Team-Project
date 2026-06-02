# AI 산출물 승인 시 verse_explanations 노출본 연결 Report

## Summary

- 브랜치: `feature/ai-asset-approval-verse-explanation-publish`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-asset-approval-verse-explanation-publish.md`
- 관리자 AI 산출물 `approve/reject/hide` API를 추가했다.
- `EXPLANATION + BIBLE_VERSE + activateForTarget=true` 승인 시 `verse_explanations` ACTIVE 노출본으로 연결되도록 `study.api` publish 계약을 추가했다.
- 신규 DB schema/Flyway migration은 없다.

## Changes

- `ReviewAiAssetUseCase` command에 `memberRole`, `adminRole`, `activateForTarget`를 추가하고 `AiAssetReviewService`를 구현했다.
- APPROVE는 active checklist와 최신 validation log `PASSED`를 요구한다.
- `PublishApprovedVerseExplanationUseCase`를 추가해 AI 도메인이 study 내부 구현을 직접 import하지 않도록 했다.
- 기존 ACTIVE 해설은 `activeUniqueKey=null`로 내린 뒤 flush하고, 새 `APPROVED + ACTIVE` 해설을 저장한다.
- `AI_ASSET_APPROVE`, `AI_ASSET_REJECT`, `AI_ASSET_HIDE` 감사 로그를 추가하되 payload/prompt/provider/reason 원문은 snapshot에 저장하지 않는다.
- OpenAPI와 `04_API_명세서.md`에 검수 API와 승인 노출본 연결 조건을 반영했다.
- Review fix: APPROVE 초입에서 asset `VALIDATING` 상태를 명시 검증하고, `REJECTED` validation log와 validation log 없음 케이스의 approve/publish 차단 테스트를 추가했다.

## Verification

| Command | Result |
| --- | --- |
| `.\gradlew.bat test --tests "*AiAssetReviewServiceTest"` | PASS |
| `.\gradlew.bat test --tests "*AdminAiAssetControllerTest"` | PASS |
| `.\gradlew.bat test --tests "*AiAssetReview*"` | PASS |
| `.\gradlew.bat test --tests "*VerseExplanation*"` | PASS |
| `.\gradlew.bat test --tests "*AiUseCaseContractTest"` | PASS |
| `.\gradlew.bat build` | PASS |
| `git diff --check` | PASS |
| `rg -n "^import .*domain\.[a-z]+\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | PASS, matches 없음 |
| OpenAPI YAML parse | PASS, approve/reject/hide path 확인 |

## Acceptance

- 관리자 검수 API 전체 구현: 충족.
- APPROVE strict gate 적용: 충족.
- 승인 시 `verse_explanations` 노출본 연결: 충족.
- 도메인 경계 준수: 충족. AI 도메인은 `study.api`만 import한다.
- 신규 schema/migration 없음: 충족.

## Follow-up

- `QT_PASSAGE` 다중 절 asset 게시, SIMULATOR 공개 연결, glossary term 게시 연결은 별도 PR 범위다.
- HIDE 시 이미 게시된 `verse_explanations` 비활성화 정책은 이번 workflow 제외 범위로 유지했다.
