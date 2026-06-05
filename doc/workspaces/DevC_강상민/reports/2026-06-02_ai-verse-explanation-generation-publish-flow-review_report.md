# Report - 2026-06-02 ai-verse-explanation-generation-publish-flow-review

## Summary

- 브랜치: `feature/ai-verse-explanation-generation-publish-flow-review`
- PR 대상: `dev`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-verse-explanation-generation-publish-flow-review.md`
- 관련 F-ID: F-02, F-08, F-14
- 점검명: AI 절별 해설 생성-게시 플로우 점검
- 실행 경로: workflow-spec-runner 기준 직접 실행

이번 작업은 production/test 코드 수정 없이 현재 `dev` 기준 AI 절별 해설 생성-게시 흐름을 코드와 테스트로 점검했다. 신규 HTTP API, OpenAPI, DB schema/Flyway migration, Java UseCase 계약 변경은 없다.

## Flow Review

| 단계 | 점검 결과 |
| --- | --- |
| 내부 시딩 | `AiDailyQtVerseExplanationSeedService.seedToday()`는 오늘 QT verseIds 중 승인 ACTIVE 해설, `VALIDATING/APPROVED` asset, `QUEUED/RUNNING` job이 없는 절만 `EXPLANATION + BIBLE_VERSE` job으로 큐잉한다. `qtPassageId == null`, 빈 verseIds는 no-op이고 active prompt 없음은 failureReason을 남긴다. |
| job 생성 | `AiService.createAiGenerationJob(...)`는 command 필수값, enum, active prompt version, prompt type 매칭을 검증하고 active job 중복을 차단한다. unique race도 `INVALID_STATUS_TRANSITION`으로 매핑한다. |
| worker 실행 | `AiGenerationJobRunner`는 `QUEUED` job을 claim해 `RUNNING`으로 전이하고, handler 성공 시 asset 저장 후 자동 검증을 실행한다. handler/LLM 실패는 job `FAILED`로 기록하고 asset/log를 남기지 않는 회귀가 있다. |
| payload 생성 | `ExplanationGenerationJobHandler`는 LLM 응답을 JSON object로 파싱하고 `explanations[]`, `glossaryTerms[]`, verse scope, non-blank text를 sanitization한다. provider raw response, prompt text, validation reference text는 payload에 저장하지 않는다. |
| 자동 검증 | `AiAutoValidationService`는 JSON object, explanation schema, `sourceMetadata.verseIds` 일치, forbidden field를 검증하고 `PASSED/REJECTED` validation log를 남긴다. `REJECTED` 결과는 asset을 `REJECTED`로 전이한다. |
| 승인 gate | `AiAssetReviewService.approve(...)`는 asset `VALIDATING`, active checklist, latest validation log `PASSED`를 요구한다. 게시 대상이면 `asset.approve(...)` 전에 payload 구조와 `targetId` 일치를 검증한다. |
| 게시 | `EXPLANATION + BIBLE_VERSE + activateForTarget=true`만 `PublishApprovedVerseExplanationUseCase`로 연결된다. invalid publish payload는 `INVALID_INPUT`으로 차단되고 asset은 `VALIDATING`, publish/audit write는 미호출로 유지된다. |
| 사용자 노출 | `VerseExplanationService`는 기존 ACTIVE 해설을 비활성화하고 새 `APPROVED + ACTIVE` 해설을 저장한다. `QtStudyContentService`는 승인 ACTIVE 해설만 조회하며 AI 큐잉 부작용을 만들지 않는다. |

## Test Mapping

| 시나리오 | 고정 테스트 |
| --- | --- |
| 누락 절만 job 생성 | `AiDailyQtVerseExplanationSeedServiceTest.seedTodayCreatesJobsOnlyForEligibleUniqueVerseIds` |
| today QT 없음/빈 verseIds no-op | `seedTodayReturnsZeroWhenTodayQtHasNoPassage`, `seedTodayReturnsZeroWhenVerseIdsAreEmpty` |
| active prompt 없음 failureReason | `seedTodayDoesNotCreateJobsWhenActiveExplanationPromptVersionIsMissing` |
| duplicate active job race skip | `seedTodayTreatsDuplicateActiveJobRaceAsSkipped`, `AiServiceTest.createAiGenerationJobMapsUniqueConstraintRaceToStatusTransitionError` |
| job 생성 command/prompt 검증 | `AiServiceTest.createAiGenerationJobCreatesQueuedJobWithPromptVersionId`, `missingPromptVersionIsBlocked`, `retiredPromptVersionIsBlocked`, `promptTypeMismatchIsBlocked` |
| worker 성공/실패 전이 | `AiGenerationJobRunnerTest.explanationJobRunsAndStoresAssetThenSucceeds`, `llmTimeoutFailsJobWithSafeFailureCodeWithoutStoringAsset`, `AiGenerationJobRunnerIntegrationTest.invalidJsonFailsJobWithoutAsset` |
| LLM payload scope 검증 | `ExplanationGenerationJobHandlerTest.invalidProviderJsonIsRejected`, `outOfScopeVerseIdIsRejected` |
| 자동 검증 `PASSED/REJECTED` | `AiAutoValidationServiceTest.validExplanationPayloadCreatesPassedAutoValidationLog`, `missingExplanationsCreatesRejectedLogAndRejectsAsset`, `sourceVerseIdsMismatchCreatesRejectedLogAndRejectsAsset`, `forbiddenPayloadFieldCreatesRejectedLogAndRejectsAsset` |
| 승인 latest validation log gate | `AiAssetReviewServiceTest.approveRequiresPassedLatestValidationLog`, `approveRejectsRejectedLatestValidationLogWithoutPublishing`, `approveRequiresValidationLogWithoutPublishing` |
| 게시 전 payload/targetId guard | `AiAssetReviewServiceTest.approvePublishTargetRejectsInvalidPayloadBeforeStatusTransition`, `AiAssetReviewFlowIntegrationTest.approveInvalidPublishPayloadKeepsAssetValidatingAndDoesNotCreateVisibleVerseExplanation` |
| 승인 ACTIVE 해설 게시/교체 | `AiAssetReviewFlowIntegrationTest.approveExplanationVerseAssetPublishesVisibleVerseExplanation`, `VerseExplanationServiceTest.publishApprovedVerseExplanation_deactivatesExistingActiveAndSavesNewActive`, `VerseExplanationReadModelTest.publishApprovedVerseExplanation_replacesActiveExplanation` |
| 사용자 조회 read-only | `QtStudyContentServiceTest.getStudyContent_whenSomeVerseExplanationsAreMissing_returnsOnlyApprovedExplanations` |

## Findings

- Blocker: 없음.
- 현재 생성-게시 흐름은 "사용자 조회 시점 생성 금지", "검증 통과 산출물만 사용자 노출", "게시 전 targetId payload guard" 기준을 만족한다.
- Test gap: `ExplanationGenerationJobHandlerTest`와 `AiGenerationJobRunnerIntegrationTest`는 현재 `QT_PASSAGE` explanation job 중심이다. 시딩 flow가 만드는 `BIBLE_VERSE` target에 대한 handler/runner 직접 회귀 테스트는 후속 보강 후보로 남긴다. production 코드는 `AiTargetType.BIBLE_VERSE` 분기를 지원하고 승인 게시 통합 테스트는 `BIBLE_VERSE` asset publish를 검증한다.

## Verification

| Command | Result |
| --- | --- |
| `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeedServiceTest"` | PASS |
| `.\gradlew.bat test --tests "*AiServiceTest"` | PASS |
| `.\gradlew.bat test --tests "*AiGenerationJobRunner*"` | PASS |
| `.\gradlew.bat test --tests "*ExplanationGenerationJobHandlerTest"` | PASS |
| `.\gradlew.bat test --tests "*AiAutoValidationServiceTest"` | PASS |
| `.\gradlew.bat test --tests "*AiAssetReview*"` | PASS |
| `.\gradlew.bat test --tests "*VerseExplanation*"` | PASS |
| `.\gradlew.bat build` | PASS |
| `git diff --check` | PASS |
| `rg -n "^import .*domain\.[a-z]+\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/domain/study` | PASS - forbidden import 없음 |

## Acceptance

- [x] workflow 문서가 요청 경로에 저장됐다.
- [x] report 문서가 요청 경로에 저장됐다.
- [x] 생성 큐잉, job 실행, payload 생성, 자동 검증, 승인 게시, 사용자 노출 흐름의 현재 구현 상태가 정리됐다.
- [x] 각 점검 시나리오의 테스트 커버리지와 gap이 기록됐다.
- [x] 지정 검증 명령 결과가 PASS/FAIL로 기록됐다.
- [x] production/test 코드, API/OpenAPI/DB/UseCase 계약 변경이 없다.
- [x] AI와 study 도메인 경계 import 위반이 없다.

## Follow-up

- `BIBLE_VERSE` target explanation job에 대한 handler/runner 직접 회귀 테스트를 별도 테스트 보강 PR 후보로 분리한다.
- `QT_PASSAGE` 다중 절 게시, SIMULATOR 게시, glossary term 게시 연결은 별도 정책 결정 후 진행한다.
