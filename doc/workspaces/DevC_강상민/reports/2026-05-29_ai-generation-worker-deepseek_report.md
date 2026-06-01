# 2026-05-29 ai-generation-worker-deepseek 작업 보고

## 개요

- 관련 F-ID: F-02, F-14
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-05-29_ai-generation-worker-deepseek.md`
- 브랜치: `feat/ai-generation-worker`
- PR 대상: `dev`
- 실행 경로: workflow-spec-runner 직접 실행

## 작업 결과

기존 시스템 API가 생성한 `QUEUED` AI generation job을 백그라운드 worker가 처리하도록 연결했다. worker는 `QUEUED` job을 batch 단위로 조회하고, pessimistic lock 기반 claim 후 `RUNNING`으로 전이한다. 이후 job type별 handler를 실행해 성공 시 `ai_generated_assets`에 산출물을 저장하고 `SUCCEEDED`로 종료하며, 실패 시 산출물 저장 없이 `FAILED`와 짧은 실패 사유를 기록한다.

PR 3 완료 판정에 필요한 DB 기반 통합 검증도 보강했다. H2/JPA repository에 저장한 실제 `QUEUED` job을 runner가 처리하고, mock `LlmClient` 응답을 통해 `ai_generated_assets.status=VALIDATING` row가 저장되는 흐름을 `AiGenerationJobRunnerIntegrationTest`로 확인한다.

이번 범위에서는 `EXPLANATION` 실제 생성만 구현했다. `SIMULATOR`는 동일 파이프라인에 등록하되, 아직 실제 생성 프로그램 연결 범위가 아니므로 `SIMULATOR_GENERATION_DISABLED` 사유로 실패 종료한다.

## 변경 내용

- Spring scheduling을 활성화했다.
- `ai.generation.worker.*` 설정 기본값을 추가했다.
- `AiGenerationJobRepository`에 `QUEUED` job id 목록 조회와 `PESSIMISTIC_WRITE` 기반 claim 조회를 추가했다.
- `AiGenerationJobWorker`를 추가해 설정된 fixed delay마다 runner를 호출하도록 했다.
- `AiGenerationJobRunner`를 추가해 claim, handler 실행, 성공/실패 상태 전이를 분리했다.
- `AiGenerationJobHandler` 계약과 job type별 handler registry를 추가했다.
- `ExplanationGenerationJobHandler`에서 QT passage context와 Bible verse 응답을 `api` UseCase로 조립해 DeepSeek에 전달하도록 했다.
- DeepSeek 응답 JSON을 파싱하고 `verseId`, 필수 필드, verse scope를 검증한 뒤 안전한 payload만 저장하도록 했다.
- provider raw response, prompt 원문, 검증 참조 원문, secret은 payload에 저장하지 않도록 했다.
- `SimulatorGenerationJobHandler`는 LLM 호출 없이 disabled 실패 사유를 남기도록 했다.
- runner와 handler 단위 테스트를 추가했다.
- repository와 runner 통합 테스트를 추가해 실제 JPA 저장 흐름을 검증했다.

## 설정 계약

| 설정 키 | 기본값 | 설명 |
| --- | --- | --- |
| `ai.generation.worker.enabled` | `${AI_GENERATION_WORKER_ENABLED:true}` | worker 실행 여부 |
| `ai.generation.worker.fixed-delay-ms` | `${AI_GENERATION_WORKER_FIXED_DELAY_MS:10000}` | polling fixed delay |
| `ai.generation.worker.batch-size` | `${AI_GENERATION_WORKER_BATCH_SIZE:5}` | 한 번에 처리할 최대 QUEUED job 수 |

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/QtAiApplication.java` | scheduling 활성화 |
| `qtai-server/src/main/resources/application.yml` | AI generation worker 기본 설정 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRepository.java` | QUEUED 조회, pessimistic claim 조회 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobWorker.java` | scheduled polling 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRunner.java` | claim, handler 실행, 성공/실패 전이 구현 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobHandler.java` | job type별 handler 계약 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java` | EXPLANATION DeepSeek 호출, 응답 검증, payload 생성 구현 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/SimulatorGenerationJobHandler.java` | SIMULATOR disabled 실패 처리 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerTest.java` | runner 상태 전이와 실패 처리 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandlerTest.java` | payload, 금지 필드 차단, invalid JSON/out-of-scope verse 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRepositoryTest.java` | QUEUED 조회, batch size, status 조건 claim 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerIntegrationTest.java` | 실제 JPA repository와 runner 기반 성공/실패 asset 저장 흐름 검증 |
| `doc/workspaces/DevC_강상민/workflows/2026-05-29_ai-generation-worker-pr3-completion.md` | PR 3 완료 판정 보강 workflow |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "com.qtai.domain.ai.internal.AiGenerationJobRunnerTest" --tests "com.qtai.domain.ai.internal.ExplanationGenerationJobHandlerTest"` | 통과 |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "*AiGenerationJob*"` | 통과 |
| `.\qtai-server\gradlew.bat -p qtai-server test --tests "*ExplanationGenerationJobHandlerTest"` | 통과 |
| `.\qtai-server\gradlew.bat -p qtai-server test` | 통과 |
| `.\qtai-server\gradlew.bat -p qtai-server build` | 통과 |
| `.\qtai-server\gradlew.bat -p qtai-server test jacocoTestReport` | 실행 불가. 현재 Gradle 프로젝트에 `jacocoTestReport` 태스크가 없음 |
| `.\qtai-server\gradlew.bat -p qtai-server jacocoTestCoverageVerification` | 실행 불가. 현재 Gradle 프로젝트에 `jacocoTestCoverageVerification` 태스크가 없음 |
| `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml` | 실행 불가. 루트에 `.spectral.yaml` 없음 |
| `gitleaks detect --source . --redact --exit-code 1` | 실행 불가. 로컬에 `gitleaks` 명령 없음 |
| `git diff --check` | 통과. CRLF 변환 경고만 출력 |

## 수용 기준 확인

| 수용 기준 | 상태 |
| --- | --- |
| 시스템 API는 기존처럼 `QUEUED` job을 만들고 202를 반환한다 | 충족. controller 계약은 변경하지 않음 |
| worker가 10초마다 최대 5개 QUEUED job을 처리한다 | 충족. 기본 설정 추가 |
| EXPLANATION job은 DeepSeek 호출 후 `ai_generated_assets` 1건을 저장한다 | 충족. mock `LlmClient`와 실제 JPA repository 기반 통합 테스트로 검증 |
| payload는 QT 전용이 아니라 verseId 기준으로 재사용 가능하다 | 충족. `explanations[]`, `glossaryTerms[]`, source metadata 모두 verseId 기준으로 검증 |
| SIMULATOR job은 disabled 사유로 실패 종료된다 | 충족. 통합 테스트에서 `SIMULATOR_GENERATION_DISABLED`와 asset 미저장을 검증 |
| controller가 LLM client나 repository를 직접 호출하지 않는다 | 충족. controller 변경 없음 |
| 다른 도메인 의존은 `api` UseCase로만 연결된다 | 충족. `qt.api.GetQtPassageContentContextUseCase`, `bible.api.GetBibleVerseUseCase`만 사용 |

## PR 3 완료 판정

| 기준 | 상태 | 근거 |
| --- | --- | --- |
| `ai_generation_jobs.QUEUED` 입력 처리 | 완료 | `AiGenerationJobRepositoryTest`, `AiGenerationJobRunnerIntegrationTest` |
| DeepSeek 호출 연결 | 완료 | 실제 live 호출 대신 mock `LlmClient`로 `complete(...)` 호출 흐름 검증 |
| `ai_generated_assets.VALIDATING` 출력 저장 | 완료 | 통합 테스트에서 asset row 1건과 payload 확인 |
| 실패 시 asset 미저장과 job `FAILED` 기록 | 완료 | invalid JSON, out-of-scope verseId, SIMULATOR disabled 시나리오 검증 |
| 사용자 Q&A, 관리자 UI | 범위 밖 | PR 3 제외 범위 |

## 제외 범위 준수

- 자동 검증 로그 생성과 checklist 실행은 구현하지 않았다.
- 승인 후 `verse_explanations`, `glossary_terms`, `simulator_clips` 반영은 구현하지 않았다.
- Today QT 캐시 갱신은 구현하지 않았다.
- F-15 Q&A 처리는 구현하지 않았다.
- 실제 시뮬레이터 생성 프로그램 통합은 구현하지 않았다.

## 후속 작업

- 자동 검증 로그와 checklist 실행 연결
- 승인 시 `verse_explanations`와 `glossary_terms` 반영
- 시뮬레이터 생성 프로그램을 `SimulatorGenerationJobHandler` 내부에 연결
- generation job 운영 모니터링과 재처리 API 보강
- Jacoco Gradle 플러그인/태스크와 Spectral ruleset, gitleaks 로컬 도구 설치 여부 정리
