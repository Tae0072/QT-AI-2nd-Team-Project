# AI 해설 산출물 게시 payload 검증 강화

## Summary

- 브랜치: `feature/ai-explanation-payload-publish-guard`
- PR 대상: `dev`
- 목표: `EXPLANATION + BIBLE_VERSE + activateForTarget=true` 승인 게시 전에 payload 구조와 `targetId` 일치를 검증해 잘못된 해설이 `verse_explanations`로 게시되지 않도록 한다.
- 기준 문서: `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `25_기능_명세서.md`
- 신규 HTTP API, OpenAPI, DB schema/migration 변경은 없다.

## 작업 목표

관리자 승인 흐름에서 게시 대상 AI 해설 산출물의 payload를 승인 상태 전이 전에 검증한다. 검증 실패 시 `ai_generated_assets.status`는 `VALIDATING`으로 남고, `verse_explanations` 게시와 감사 로그 기록은 수행하지 않는다.

## 범위

- `AiAssetReviewService.approve(...)`에서 게시 command 생성을 `asset.approve(...)`보다 앞에 배치한다.
- 게시 대상은 기존과 동일하게 `assetType=EXPLANATION`, `targetType=BIBLE_VERSE`, `activateForTarget=true`로 제한한다.
- 게시 대상 payload는 JSON object, `explanations` array, `verseId == targetId` 항목, matching item의 non-blank `summary`/`explanation`을 요구한다.
- `activateForTarget=false`, `QT_PASSAGE`, `SIMULATOR` 등 비게시 대상은 payload 게시 검증 없이 기존 승인 흐름을 유지한다.

## 제외 범위

- 신규 API, OpenAPI, DB schema/Flyway migration 변경 없음.
- `QT_PASSAGE` 다중 절 게시, SIMULATOR 게시, glossary term 게시 연결은 제외한다.
- `AiAutoValidationService`의 기존 자동 검증 정책은 변경하지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiAssetReviewService.java` | 승인 전 게시 payload 검증과 publish command 선생성 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewServiceTest.java` | payload 오류 시 상태 유지, publish/audit 미호출 단위 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiAssetReviewFlowIntegrationTest.java` | invalid payload 승인 실패 시 DB 상태와 노출본 미생성 통합 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-explanation-payload-publish-guard_report.md` | 구현 결과와 검증 결과 기록 |

## 구현 순서

1. `AiAssetReviewServiceTest`에 invalid publish payload 케이스를 먼저 추가하고 실패를 확인한다.
2. `AiAssetReviewService.approve(...)`에서 체크리스트와 최신 `PASSED` 검증 로그 확인 후, `asset.approve(...)` 전에 `PublishApprovedVerseExplanationCommand`를 생성한다.
3. `explanationItemForTarget(...)`에 root JSON object 검증을 명시한다.
4. 비게시 대상 approve 흐름이 깨진 payload에도 기존처럼 성공하는지 테스트로 고정한다.
5. `AiAssetReviewFlowIntegrationTest`에 invalid payload가 DB 상태와 `verse_explanations` 노출본을 변경하지 않는 케이스를 추가한다.
6. 지정 검증 명령을 실행하고 report에 결과를 기록한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiAssetReviewServiceTest` | invalid JSON, root non-object, `explanations` 누락/비배열, matching `verseId` 없음, matching item의 `summary`/`explanation` blank/null 차단 |
| `AiAssetReviewServiceTest` | `activateForTarget=false` 또는 `QT_PASSAGE`면 payload 게시 검증과 publish 호출 없음 |
| `AiAssetReviewFlowIntegrationTest` | invalid publish payload 승인 시 asset은 `VALIDATING`, 신규 `verse_explanations` 없음, audit 미호출 |

## 수용 기준

- [ ] 게시 대상 payload 검증 실패 시 `BusinessException(ErrorCode.INVALID_INPUT)`이 발생한다.
- [ ] 검증 실패 시 asset status는 `VALIDATING`으로 남는다.
- [ ] 검증 실패 시 `PublishApprovedVerseExplanationUseCase`와 `WriteAuditLogUseCase`는 호출되지 않는다.
- [ ] 정상 payload 승인 게시와 기존 hide/unpublish 흐름은 유지된다.
- [ ] AI 도메인은 `study.api` 계약만 사용하고 `study.internal`을 import하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 서비스의 승인 순서와 인접 테스트에 집중되어 있어 병렬 편집보다 직접 실행이 충돌 가능성을 줄인다.
- 테스트 보강과 구현 순서가 강하게 연결되어 있어 한 작업자가 RED/GREEN 흐름을 순차 확인하는 편이 안전하다.

### 위임 가능 작업

| Worker | 담당 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 테스트, 구현, 문서, 검증을 순서대로 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AiAssetReviewServiceTest"
.\gradlew.bat test --tests "*AiAssetReviewFlowIntegrationTest"
.\gradlew.bat test --tests "*AiAssetReview*"
.\gradlew.bat test --tests "*VerseExplanation*"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/domain/study
```

## 후속 작업으로 남긴 항목

- `QT_PASSAGE` 다중 절 게시 연결은 별도 PR에서 정책 결정 후 처리한다.
- SIMULATOR/glossary term 게시 연결도 별도 범위로 유지한다.
