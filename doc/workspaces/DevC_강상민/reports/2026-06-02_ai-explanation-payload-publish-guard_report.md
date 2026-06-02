# AI 해설 산출물 게시 payload 검증 강화 Report

## Summary

- 브랜치: `feature/ai-explanation-payload-publish-guard`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-explanation-payload-publish-guard.md`
- `EXPLANATION + BIBLE_VERSE + activateForTarget=true` 승인 게시 전에 payload 구조와 `targetId` 일치를 검증하도록 보강했다.
- 신규 HTTP API, OpenAPI, DB schema/Flyway migration 변경은 없다.

## Changes

- `AiAssetReviewService.approve(...)`에서 게시 대상 `PublishApprovedVerseExplanationCommand`를 `asset.approve(...)`보다 먼저 생성하도록 순서를 변경했다.
- 게시 대상 payload가 JSON object가 아니거나, `explanations`가 array가 아니거나, `verseId == targetId` 항목이 없거나, matching item의 `summary`/`explanation`이 blank/null이면 `BusinessException(ErrorCode.INVALID_INPUT)`으로 승인 게시를 차단한다.
- 게시 대상 payload 검증 실패 시 asset status는 `VALIDATING`으로 남고 publish/audit write는 호출되지 않는다.
- `activateForTarget=false`와 `QT_PASSAGE` asset은 깨진 payload 또는 다중 절 payload여도 게시 검증 없이 기존 approve 흐름을 유지한다.
- AI 도메인은 기존처럼 `study.api.PublishApprovedVerseExplanationUseCase`만 호출하며 `study.internal` 직접 import는 추가하지 않았다.

## Tests

- `AiAssetReviewServiceTest`
  - 정상 payload 승인 게시 command 검증 유지.
  - invalid JSON, root non-object, `explanations` 누락/비배열, matching `verseId` 없음, matching item의 `summary` blank/null, `explanation` blank/null 차단 검증 추가.
  - `activateForTarget=false`와 `QT_PASSAGE` 비게시 대상 회귀 검증 보강.
- `AiAssetReviewFlowIntegrationTest`
  - invalid publish payload 승인 시 DB의 asset status가 `VALIDATING`으로 남고, 해당 asset 기반 `verse_explanations` 신규 노출본이 생성되지 않으며, audit write가 호출되지 않는지 검증 추가.

## Verification

| Command | Result |
| --- | --- |
| `.\gradlew.bat test --tests "*AiAssetReviewServiceTest"` | RED 확인 후 PASS |
| `.\gradlew.bat test --tests "*AiAssetReviewFlowIntegrationTest"` | PASS |
| `.\gradlew.bat test --tests "*AiAssetReview*"` | PASS |
| `.\gradlew.bat test --tests "*VerseExplanation*"` | PASS |
| `.\gradlew.bat build` | PASS |
| `git diff --check` | PASS, CRLF 변환 warning만 표시 |
| `rg -n "^import .*domain\.[a-z]+\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/domain/study` | PASS, 금지 import match 없음 |

## Acceptance

- 게시 대상 payload 검증 실패 시 `INVALID_INPUT`으로 approve가 차단된다.
- 검증 실패 시 asset status는 `VALIDATING`으로 유지된다.
- 검증 실패 시 `PublishApprovedVerseExplanationUseCase`와 `WriteAuditLogUseCase`는 호출되지 않는다.
- 정상 payload 승인 게시, latest validation log gate, hide/unpublish 흐름은 유지된다.
- 신규 API/OpenAPI/DB 변경 없이 승인 게시 안전장치만 강화했다.

## Follow-up

- `QT_PASSAGE` 다중 절 게시 연결, SIMULATOR 게시, glossary term 게시 연결은 별도 PR 범위로 유지한다.
