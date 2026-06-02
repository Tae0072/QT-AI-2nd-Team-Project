# AI Asset HIDE 시 verse_explanations 노출본 비활성화 Report

## Summary

- 브랜치: `feature/ai-asset-hide-verse-explanation-unpublish`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-asset-hide-verse-explanation-unpublish.md`
- `EXPLANATION + BIBLE_VERSE` AI asset hide 시 해당 asset이 게시한 `verse_explanations` 노출본을 사용자 조회에서 제외하도록 연결했다.
- 신규 HTTP API, OpenAPI schema, DB schema/Flyway migration은 없다.

## Changes

- `study.api`에 `HidePublishedVerseExplanationUseCase`와 command/result DTO를 추가했다.
- `VerseExplanation` hide 정책을 `status=HIDDEN + activeUniqueKey=null`로 고정했다.
- `VerseExplanationService`는 `aiAssetId` 기준 `APPROVED + ACTIVE` row를 pessimistic lock으로 조회해 숨김 처리하고, 매칭 row가 없으면 `hiddenCount=0`으로 no-op 처리한다.
- `AiAssetReviewService`의 HIDE 성공 후 `EXPLANATION + BIBLE_VERSE` asset에 한해 study hide use case를 호출한다.
- OpenAPI와 `04_API_명세서.md`의 hide 설명을 실제 노출본 비활성화 정책에 맞게 갱신했다.

## Verification

| Command | Result |
| --- | --- |
| `.\gradlew.bat test --tests "*AiAssetReviewServiceTest"` | PASS |
| `.\gradlew.bat test --tests "*AiAssetReviewFlowIntegrationTest"` | PASS |
| `.\gradlew.bat test --tests "*VerseExplanation*"` | PASS |
| `.\gradlew.bat test --tests "*AdminAiAssetControllerTest"` | PASS |
| `.\gradlew.bat build` | PASS |
| `git diff --check` | PASS, CRLF warning only |
| `rg -n "^import .*domain\.[a-z]+\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | PASS, matches 없음 |
| OpenAPI YAML parse | NOT RUN, local YAML parser/spectral config 없음 |

## Acceptance

- AI asset hide 시 연결된 절 해설 노출본 비활성화: 충족.
- `HIDDEN + activeUniqueKey=null` 정책 적용: 충족.
- 연결 row 없음 no-op 처리: 충족.
- AI 도메인 경계 준수: 충족. AI 도메인은 `study.api`만 import한다.
- 신규 API/schema/migration 없음: 충족.

## Follow-up

- 별도 unhide/restore 정책은 이번 PR 범위에서 제외했다.
- `QT_PASSAGE`, `SIMULATOR`, glossary term 게시 연결은 별도 PR 범위다.
