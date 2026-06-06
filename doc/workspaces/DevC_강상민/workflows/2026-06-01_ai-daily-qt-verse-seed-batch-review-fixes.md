# Workflow - 2026-06-01 ai-daily-qt-verse-seed-batch-review-fixes

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-daily-verse-seed-batch` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | `ai-daily-qt-verse-seed-batch` 코드 리뷰 반영 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-daily-qt-verse-seed-batch.md` |
| 해당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

일일 QT 절 단위 `EXPLANATION` generation job 시딩 배치의 코드 리뷰 지적사항 중 실제 반영 가치가 있는 항목을 보강한다. 기능 범위는 유지하고, 테스트 가독성, repository query 의도, null/invalid 방어, 트랜잭션 의도, 문서 정합성을 개선한다.

## 범위

- `AiDailyQtVerseExplanationSeedService.seedToday()`에 `@Transactional(propagation = Propagation.NOT_SUPPORTED)`를 명시하고, verse 단위 독립 시딩 의도를 드러낸다.
- `GetTodayQtUseCase.getToday(null)`와 `GetQtPassageContentContextUseCase.getContentContext(...)` 결과에 `Objects.requireNonNull` 가드를 추가한다.
- repository query 이름을 의도 기반으로 바꾼다.
  - asset: `findReadyExplanationBibleVerseTargetIds(Collection<Long> targetIds)`
  - job: `findActiveExplanationBibleVerseTargetIds(Collection<Long> targetIds)`
- `AiGeneratedAssetRepositoryTest.nextGenerationJobId` 필드를 클래스 상단 필드 영역으로 이동한다.
- service 테스트에 빈 verse ids, invalid id, active prompt 미존재 로그 검증을 추가한다.
- 기존 workflow/report에 00:05 정책 설명을 보강한다.
  - 00:05는 내부 사전 생성 시딩이다.
  - 기존 04:00 정책은 사용자 노출/cache refresh 기준이다.
- report 검증 명령은 Windows `.\gradlew.bat`와 Unix/CI `./gradlew` 표기를 함께 남긴다.

## 제외 범위

- scheduler 시간 변경 없음. 00:05 KST 유지.
- scheduler catch 범위 변경 없음. `RuntimeException`만 흡수하고 `Error` 계열은 복구 대상으로 보지 않는다.
- schedlock, retry, 운영 알림, DB unique index 추가 없음.
- SIMULATOR, QT_PASSAGE 단위 generation, worker 처리 흐름 변경 없음.
- 중앙 요구사항 문서 수정 없음. 이번 PR 문서와 report에 정합성 설명만 추가한다.
- `SYSTEM_BATCH`는 기존 프로젝트 관례상 문자열 상수로 유지한다. 별도 Actor enum은 이번 PR에서 만들지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedService.java` | null guard, transaction propagation, renamed query 호출 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGeneratedAssetRepository.java` | ready EXPLANATION BIBLE_VERSE asset target id query |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRepository.java` | active EXPLANATION BIBLE_VERSE job target id query |
| Modify Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedServiceTest.java` | empty/invalid/logging 테스트 보강 |
| Modify Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGeneratedAssetRepositoryTest.java` | renamed query 검증과 필드 위치 정리 |
| Modify Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRepositoryTest.java` | renamed query 검증 |
| Modify | `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-daily-qt-verse-seed-batch.md` | 00:05/04:00 정책 관계 보강 |
| Modify | `doc/workspaces/DevC_강상민/reports/2026-06-01_ai-daily-qt-verse-seed-batch_report.md` | 00:05/04:00 정책 관계와 cross-platform 검증 표기 보강 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-01_ai-daily-qt-verse-seed-batch-review-fixes_report.md` | 리뷰 반영 결과 기록 |

## 구현 순서

1. 이 workflow 문서를 저장한다.
2. `AiDailyQtVerseExplanationSeedServiceTest`를 먼저 보강하고 실패를 확인한다.
3. repository 테스트에서 새 메서드명을 사용하도록 바꾸고 실패를 확인한다.
4. repository 메서드명을 의도 기반 이름으로 변경하고 service 호출부를 맞춘다.
5. service에 `Objects.requireNonNull` 가드와 `NOT_SUPPORTED` 트랜잭션 의도를 추가한다.
6. `AiGeneratedAssetRepositoryTest` 필드 위치를 정리한다.
7. 기존 workflow/report 문서에 00:05 내부 시딩과 04:00 사용자 노출/cache refresh의 관계를 명시한다.
8. 관련 테스트와 build를 실행한다.
9. review-fixes report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가/변경 검증 |
| --- | --- |
| `AiDailyQtVerseExplanationSeedServiceTest` | eligible verse만 생성되는 기존 경로 유지 |
| `AiDailyQtVerseExplanationSeedServiceTest` | 빈 verse ids면 0건 시딩, prompt/job 생성 조회 없음 |
| `AiDailyQtVerseExplanationSeedServiceTest` | `qtPassageId` null/0/음수면 `BusinessException(ErrorCode.INVALID_INPUT)` |
| `AiDailyQtVerseExplanationSeedServiceTest` | verse id null/0/음수면 `BusinessException(ErrorCode.INVALID_INPUT)` |
| `AiDailyQtVerseExplanationSeedServiceTest` | active prompt 미존재 시 warn 로그에 `ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND` 포함 |
| `AiGeneratedAssetRepositoryTest` | renamed query가 `EXPLANATION + BIBLE_VERSE + VALIDATING/APPROVED`만 반환 |
| `AiGenerationJobRepositoryTest` | renamed query가 `EXPLANATION + BIBLE_VERSE + QUEUED/RUNNING`만 반환 |
| `AiDailyQtVerseExplanationSeedSchedulerTest` | enabled/disabled/RuntimeException 흡수 기존 검증 유지 |

## 허용 기준

- [ ] 코드 리뷰의 실제 반영 항목이 모두 처리된다.
- [ ] 00:05/04:00 정책 관계가 PR 문서에서 오해 없이 설명된다.
- [ ] 신규 API/OpenAPI/DB schema 변경이 없다.
- [ ] 관련 테스트와 build가 통과한다.
- [ ] 금지 import 검색에서 신규 도메인 경계 위반이 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `domain.ai.internal` service/repository/test/docs에 집중되어 있다.
- 테스트 보강과 구현 변경이 같은 fixture와 메서드명 변경에 묶여 있어 순차 실행이 안전하다.
- 병렬 작업보다 한 agent가 RED, GREEN, 문서 정합성 확인까지 이어가는 편이 충돌 가능성이 낮다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 workflow 저장, 테스트 보강, 구현 수정, 검증, report 작성을 직접 수행한다.

## 검증 계획

- `.\gradlew.bat test --tests "*AiDailyQtVerseExplanationSeed*"`
- `.\gradlew.bat test --tests "*AiGenerationJobRepositoryTest"`
- `.\gradlew.bat test --tests "*AiGeneratedAssetRepositoryTest"`
- `.\gradlew.bat test --tests "*AiGenerationJob*"`
- `.\gradlew.bat build`
- `git diff --check`
- `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai`

Unix/CI 환경에서는 `qtai-server` 기준 `./gradlew`로 동일한 Gradle task를 실행한다.

## 후속 작업으로 남기는 항목

- schedlock 또는 DB unique constraint 기반 중복 실행 race 보강.
- batch 실패 알림/모니터링 연동.
- SIMULATOR 일일 시딩 정책 결정.
