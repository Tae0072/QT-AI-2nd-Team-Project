# BIBLE_VERSE Target 절별 해설 Worker 회귀 테스트 보강 Report

- 일자: 2026-06-02
- 브랜치: `test/ai-bible-verse-explanation-worker-flow`
- 관련 F-ID: F-02, F-08, F-14
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-bible-verse-explanation-worker-flow-test.md`
- 결과: PASS

## Summary

시딩 서비스가 생성하는 `EXPLANATION + BIBLE_VERSE` job이 handler/runner 경로에서 직접 검증되도록 회귀 테스트를 보강했다.

이번 작업은 테스트와 workflow/report 산출물만 변경했다. 신규 HTTP API, OpenAPI, DB schema/Flyway migration, Java UseCase 계약 변경은 없다.

## Changes

### `ExplanationGenerationJobHandlerTest`

- `BIBLE_VERSE` target 성공 케이스를 추가했다.
- `QT_PASSAGE` context 조회 없이 `GetBibleVerseUseCase.getVerses(List.of(targetId))`만 호출하는지 검증했다.
- 생성 asset의 `assetType=EXPLANATION`, `targetType=BIBLE_VERSE`, `targetId=1001`을 검증했다.
- payload의 `sourceMetadata.targetType=BIBLE_VERSE`, `targetId=1001`, `verseIds=[1001]`을 검증했다.
- payload에 `qtPassageId`, `qtDate`, `title` 같은 QT passage metadata가 저장되지 않는지 검증했다.
- LLM prompt가 단일 절 target 정보를 포함하고 QT passage 정보를 포함하지 않는지 검증했다.

### `AiGenerationJobRunnerIntegrationTest`

- `EXPLANATION + BIBLE_VERSE` runner 통합 성공 케이스를 추가했다.
- runner 실행 후 job `SUCCEEDED`, asset `VALIDATING`, auto validation log `PASSED`를 검증했다.
- 저장 payload가 단일 절 `sourceMetadata.verseIds=[targetId]`를 유지하는지 검증했다.
- `QT_PASSAGE` context를 호출하지 않고 `BibleVerse` 조회만 사용하는지 검증했다.

## Verification

| Command | Result | Note |
| --- | --- | --- |
| `.\gradlew.bat test --tests "*ExplanationGenerationJobHandlerTest"` | PASS | handler BIBLE_VERSE 단위 회귀 포함 |
| `.\gradlew.bat test --tests "*AiGenerationJobRunnerIntegrationTest"` | PASS | runner 통합 BIBLE_VERSE 회귀 포함 |
| `.\gradlew.bat test --tests "*AiGenerationJobRunner*"` | PASS | runner 계열 회귀 |
| `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeedServiceTest"` | PASS | 시딩 canonical flow 회귀 |
| `.\gradlew.bat test --tests "*AiGenerationJob*"` | PASS | AI job 계열 회귀 |
| `.\gradlew.bat build` | PASS | 전체 build |
| `git diff --check` | PASS | LF/CRLF 안내 warning만 출력 |
| `rg -n "^import .*domain\.[a-z]+\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/domain/study` | PASS | 금지 domain-boundary import 없음 |

## Acceptance

- `BIBLE_VERSE` target handler가 단일 절 입력만 사용함을 테스트로 고정했다.
- `BIBLE_VERSE` target runner가 `SUCCEEDED` job, `VALIDATING` asset, `PASSED` validation log를 생성함을 테스트로 고정했다.
- 사용자 조회 시점 생성, 승인 게시 정책, `QT_PASSAGE` 다중 절 게시, SIMULATOR/glossary 게시 연결은 변경하지 않았다.

## Findings

- production 코드 수정 없이 요청한 회귀 테스트 보강이 통과했다.
- 추가 결함이나 별도 구현 PR로 분리해야 할 gap은 발견하지 못했다.

## Follow-up

- 없음.
