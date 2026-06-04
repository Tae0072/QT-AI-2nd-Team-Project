# Workflow - 2026-06-02 ai-bible-verse-explanation-worker-flow-test

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `test/ai-bible-verse-explanation-worker-flow` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-08, F-14 |
| 트리거 | `BIBLE_VERSE` target 절별 해설 job handler/runner 회귀 테스트 보강 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `25_기능_명세서.md` |
| workflow 경로 | `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-bible-verse-explanation-worker-flow-test.md` |
| report 경로 | `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-bible-verse-explanation-worker-flow-test_report.md` |

## 작업 목표

`AiDailyQtVerseExplanationSeedService`가 실제로 만드는 `EXPLANATION + BIBLE_VERSE` generation job이 `ExplanationGenerationJobHandler`와 `AiGenerationJobRunner`에서 직접 회귀 테스트로 고정되도록 보강한다.

기존 production 코드는 `BIBLE_VERSE` target 분기를 지원하지만, handler/runner 테스트는 주로 `QT_PASSAGE` target 중심이다. 이번 작업은 이 테스트 gap을 닫는 것이 목표다.

## 범위

- `ExplanationGenerationJobHandlerTest`에 `BIBLE_VERSE` target 성공 케이스를 추가한다.
- `AiGenerationJobRunnerIntegrationTest`에 `EXPLANATION + BIBLE_VERSE` runner 통합 성공 케이스를 추가한다.
- workflow와 report를 작성한다.
- production 코드는 기본적으로 수정하지 않는다. 테스트가 실제 결함을 드러낼 때만 최소 수정한다.

## 제외 범위

- 신규 HTTP API, OpenAPI, DB schema/Flyway migration, Java UseCase 계약 변경.
- 사용자 조회 시점 AI job 큐잉.
- 승인 게시 정책 변경.
- `QT_PASSAGE` 다중 절 게시 연결.
- SIMULATOR 게시 연결.
- glossary term 게시 연결.
- LLM prompt 정책 변경.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-02_ai-bible-verse-explanation-worker-flow-test.md` | 테스트 보강 범위와 검증 기준 고정 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandlerTest.java` | `BIBLE_VERSE` target handler payload 생성 회귀 테스트 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerIntegrationTest.java` | `EXPLANATION + BIBLE_VERSE` runner 통합 성공 회귀 테스트 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-02_ai-bible-verse-explanation-worker-flow-test_report.md` | 구현 내용, 검증 결과, 후속 항목 기록 |

## 구현 순서

1. workflow 문서를 저장한다.
2. workflow 문서를 다시 읽고 `workflow-spec-runner` 기준 직접 실행 경로를 선택한다.
3. `ExplanationGenerationJobHandlerTest`에 `BIBLE_VERSE` target job helper와 성공 테스트를 추가한다.
4. handler 테스트에서 `GetQtPassageContentContextUseCase`가 호출되지 않고 `GetBibleVerseUseCase.getVerses(List.of(targetId))`만 호출되는지 검증한다.
5. handler 테스트에서 asset target, `sourceMetadata.targetType`, `sourceMetadata.targetId`, `sourceMetadata.verseIds`, LLM prompt의 단일 절 target 정보를 검증한다.
6. `AiGenerationJobRunnerIntegrationTest`에 `BIBLE_VERSE` target job persist helper와 성공 통합 테스트를 추가한다.
7. runner 테스트에서 job `SUCCEEDED`, asset `VALIDATING`, auto validation log `PASSED`, 단일 절 payload metadata를 검증한다.
8. 지정 검증 명령을 실행한다.
9. report 문서를 작성한다.
10. `git status --short`로 workflow/report/test 외 변경이 없는지 확인한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `ExplanationGenerationJobHandlerTest` | `BIBLE_VERSE` target은 QT context 없이 단일 Bible verse 입력으로 payload를 만들고, asset/payload/prompt가 target verse 기준으로 정리된다. |
| `AiGenerationJobRunnerIntegrationTest` | `EXPLANATION + BIBLE_VERSE` job은 runner 실행 후 asset 저장, auto validation `PASSED`, job `SUCCEEDED`로 끝난다. |

## 수용 기준

- [ ] `ExplanationGenerationJobHandlerTest`에 `BIBLE_VERSE` target 성공 케이스가 추가된다.
- [ ] handler 테스트가 QT passage context 미호출과 단일 Bible verse 입력 사용을 검증한다.
- [ ] handler 테스트가 asset target과 `sourceMetadata`를 검증한다.
- [ ] `AiGenerationJobRunnerIntegrationTest`에 `EXPLANATION + BIBLE_VERSE` runner 통합 성공 케이스가 추가된다.
- [ ] runner 테스트가 job `SUCCEEDED`, asset `VALIDATING`, validation log `PASSED`를 검증한다.
- [ ] 신규 API/OpenAPI/DB/UseCase 계약 변경이 없다.
- [ ] workflow와 report가 요청 경로에 저장된다.
- [ ] 도메인 경계 import 위반이 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 두 테스트 파일과 두 문서 파일에 집중되어 있다.
- handler 테스트와 runner 통합 테스트가 같은 `BIBLE_VERSE` target fixture 의미를 공유하므로 한 agent가 일관되게 작성하는 편이 안전하다.
- production 코드 수정이 기본 범위가 아니며, 테스트 실패 시 원인을 즉시 좁혀 확인해야 한다.

### 위임 가능 작업

| Worker | 해당 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow 작성, 테스트 보강, 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat test --tests "*ExplanationGenerationJobHandlerTest"
.\gradlew.bat test --tests "*AiGenerationJobRunnerIntegrationTest"
.\gradlew.bat test --tests "*AiGenerationJobRunner*"
.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeedServiceTest"
.\gradlew.bat test --tests "*AiGenerationJob*"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/domain/study
```

## 후속 작업으로 남길 항목

- `QT_PASSAGE` 다중 절 게시, SIMULATOR 게시, glossary term 게시 연결은 별도 정책 결정 후 진행한다.
- `BIBLE_VERSE` target 테스트 보강 중 production 결함이 발견되면 이번 PR에서 최소 수정하고 report에 원인과 검증 결과를 기록한다.
