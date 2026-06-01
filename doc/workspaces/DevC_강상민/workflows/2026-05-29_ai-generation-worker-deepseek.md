# Workflow — 2026-05-29 ai-generation-worker-deepseek

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feat/ai-generation-worker` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | PR 3: DeepSeek 호출과 generation job 연결 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `CODE_CONVENTION.md` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/main/java/com/qtai/external/llm/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**` |

## 작업 목표

기존 시스템 API가 생성한 `QUEUED` AI generation job을 백그라운드 worker가 처리하도록 연결한다. worker는 DeepSeek를 호출하고, 결과를 `ai_generated_assets`에 저장한 뒤 job 상태를 `SUCCEEDED` 또는 `FAILED`로 마무리한다.

이번 작업은 `EXPLANATION` 실제 생성만 구현한다. `SIMULATOR`는 같은 파이프라인에 붙을 수 있도록 handler 구조만 만들고, 현재는 명확한 disabled 실패 사유를 남긴다.

## 범위

- `POST /api/v1/system/ai/generation-jobs`의 202/QUEUED 응답 계약은 유지한다.
- worker 기본값은 `enabled=true`, `fixed-delay-ms=10000`, `batch-size=5`로 둔다.
- `EXPLANATION` handler는 `qt.api.GetQtPassageContentContextUseCase`와 `bible.api.GetBibleVerseUseCase`로 verseId 기준 입력을 조립한다.
- `ai_generated_assets.payload_json`에는 `explanations[]`, `glossaryTerms[]`, `promptVersionId`, `promptVersion`, `promptContentHash`, `modelName`, token usage, source metadata를 저장한다.
- prompt 원문, provider raw response, 검증 참조 원문, secret은 저장하지 않는다.

## 제외 범위

- 자동 검증 로그 생성과 checklist 실행.
- 승인 시 `verse_explanations`, `glossary_terms`, `simulator_clips`에 노출본 반영.
- Today QT 캐시 갱신.
- F-15 Q&A 처리.
- 실제 시뮬레이터 생성 프로그램 통합.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/QtAiApplication.java` | scheduling 활성화 |
| Modify | `qtai-server/src/main/resources/application.yml` | worker 설정 기본값 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRepository.java` | QUEUED 조회, claim용 lock 조회 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobWorker.java` | scheduled polling |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRunner.java` | claim, handler 호출, 성공/실패 전이 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobHandler.java` | 타입별 handler 계약 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java` | DeepSeek 호출과 verseId 기준 payload 저장 |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/SimulatorGenerationJobHandler.java` | disabled 실패 처리 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerTest.java` | 상태 전이와 실패 처리 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandlerTest.java` | prompt 입력, JSON 검증, asset 저장 |

## 구현 순서

1. worker 설정값과 scheduling 활성화를 추가한다.
2. repository에 `QUEUED` job id 목록 조회와 `PESSIMISTIC_WRITE` 기반 단건 claim 조회를 추가한다.
3. runner를 구현해 `QUEUED -> RUNNING`, handler 실행, `SUCCEEDED/FAILED` 전이를 분리한다.
4. handler registry를 만들고 `EXPLANATION`, `SIMULATOR` 타입을 명시적으로 등록한다.
5. `ExplanationGenerationJobHandler`에서 QT verseIds와 Bible verse 응답을 조립해 DeepSeek에 전달한다.
6. DeepSeek 응답 JSON을 object로 파싱하고, verseId 범위와 필수 필드를 검증한다.
7. 검증된 payload만 `ai_generated_assets`에 저장한다.
8. 실패 경로는 민감정보 없이 `errorMessage`에 짧은 사유를 남긴다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `AiGenerationJobRunnerTest` | 정상 EXPLANATION job이 RUNNING, asset 저장, SUCCEEDED로 전이 |
| `AiGenerationJobRunnerTest` | LLM 오류 시 asset 없이 FAILED 기록 |
| `AiGenerationJobRunnerTest` | SIMULATOR job은 LLM 호출 없이 FAILED와 `SIMULATOR_GENERATION_DISABLED` 기록 |
| `ExplanationGenerationJobHandlerTest` | payload의 `explanations[]`, `glossaryTerms[]`가 verseId 기준 |
| `ExplanationGenerationJobHandlerTest` | provider raw response, prompt 원문, reference 원문 필드 저장 차단 |
| `ExplanationGenerationJobHandlerTest` | 응답 JSON이 깨졌거나 입력 밖 verseId면 FAILED 처리 |

## 수용 기준

- [ ] 시스템 API는 기존처럼 `QUEUED` job을 만들고 202를 반환한다.
- [ ] worker가 10초마다 최대 5개 QUEUED job을 처리한다.
- [ ] EXPLANATION job은 DeepSeek 호출 후 `ai_generated_assets` 1건을 저장한다.
- [ ] payload는 QT 전용이 아니라 verseId 기준으로 재사용 가능하다.
- [ ] SIMULATOR job은 disabled 사유로 실패 종료된다.
- [ ] controller가 LLM client나 repository를 직접 호출하지 않는다.
- [ ] 다른 도메인 의존은 `api` UseCase로만 연결된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- worker claim, 상태 전이, handler, repository 테스트가 강하게 연결되어 순차 TDD가 안전하다.
- 같은 `domain.ai.internal` 경로를 여러 작업자가 동시에 수정하면 충돌 가능성이 높다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 전체 구현과 검증을 직접 수행한다.

## 검증 계획

- `./gradlew -p qtai-server test`
- `./gradlew -p qtai-server build`
- `./gradlew -p qtai-server test jacocoTestReport`
- `./gradlew -p qtai-server jacocoTestCoverageVerification`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`

## 후속 작업으로 남길 항목

- 자동 검증 로그와 checklist 실행 연결.
- 승인 시 `verse_explanations`와 `glossary_terms` 반영.
- 시뮬레이터 생성 프로그램을 `SimulatorGenerationJobHandler` 내부에 연결.
- generation job 운영 모니터링과 재처리 API 보강.
