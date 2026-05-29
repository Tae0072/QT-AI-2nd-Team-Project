# Workflow — 2026-05-29 ai-generation-worker-pr3-completion

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feat/ai-generation-worker` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | PR 3 `DeepSeek 호출과 generation job 연결` 완료 판정 보강 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `CODE_CONVENTION.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-28_ai-implementation-order.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-29_ai-generation-worker-deepseek.md` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

현재 구현된 AI generation worker는 runner/handler 단위 테스트 기준으로는 동작하지만, PR 3 완료 기준인 “`QUEUED` generation job 처리 시 DeepSeek 호출 후 `ai_generated_assets`의 `VALIDATING` 산출물 저장까지 이어짐”을 실제 DB 흐름으로 검증하지 못했다.

이번 작업은 기존 구현을 크게 확장하지 않고, JPA/H2 기반 통합 테스트와 필요한 최소 보정을 추가해 PR 3 완료 판정을 가능하게 만든다. 완료 기준은 실제 repository에 저장된 `QUEUED` job이 runner를 통해 처리되고, mock `LlmClient` 응답으로 asset row가 저장되며, 성공/실패 상태가 DB에 남는 것이다.

## 범위

- `AiGenerationJobRepository`의 `QUEUED` 조회와 claim 조건을 JPA 테스트로 검증한다.
- `AiGenerationJobRunner`가 실제 repository와 transaction manager에서 `QUEUED -> RUNNING -> SUCCEEDED/FAILED` 전이를 수행하는지 검증한다.
- `ExplanationGenerationJobHandler`가 실제 runner에 연결되어 mock `LlmClient` 호출 후 `ai_generated_assets` row를 저장하는지 검증한다.
- `SIMULATOR` job은 LLM 호출 없이 `SIMULATOR_GENERATION_DISABLED`로 실패하는지 DB 상태로 검증한다.
- 통합 테스트가 드러내는 runner/handler/repository의 최소 결함만 수정한다.
- 기존 report를 PR 3 기준에 맞게 “부분 완료” 또는 “완료” 상태로 정정한다.

## 제외 범위

- 실제 DeepSeek live API 호출 테스트.
- 자동 검증 로그 생성과 checklist 실행.
- 승인 후 `verse_explanations`, `glossary_terms`, `simulator_clips` 반영.
- Today QT 캐시 갱신.
- F-15 Q&A 처리.
- 멈춘 `RUNNING` job 재처리, retry/backoff, 운영 모니터링 API.
- 실제 시뮬레이터 생성 프로그램 통합.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRepositoryTest.java` | `QUEUED` 조회, batch size, status 조건 claim 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerIntegrationTest.java` | 실제 JPA repository와 runner 기반 성공/실패 asset 저장 흐름 검증 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRunner.java` | 통합 테스트에서 드러난 transaction/state 저장 결함 최소 수정 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java` | 통합 테스트에서 드러난 payload 검증/저장 결함 최소 수정 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRepository.java` | 통합 테스트에서 드러난 query/lock 결함 최소 수정 |
| Modify | `doc/workspaces/DevC_강상민/reports/2026-05-29_ai-generation-worker-deepseek_report.md` | PR 3 기준 완료 여부, 검증 결과, 남은 리스크 정정 |

## 구현 순서

1. `AiGenerationJobRepositoryTest`를 먼저 추가해 `QUEUED` job id 조회가 `createdAt`, `id` 오름차순이며 `PageRequest`의 batch size를 지키는지 검증한다.
2. 같은 repository 테스트에서 `findByIdAndStatus(id, QUEUED)`가 `QUEUED`만 반환하고 `RUNNING`, `SUCCEEDED`, `FAILED`는 반환하지 않는지 검증한다.
3. `AiGenerationJobRunnerIntegrationTest`를 추가하고 H2/JPA 환경에서 `AiPromptVersion`과 `QUEUED` `AiGenerationJob`을 저장하는 fixture를 만든다.
4. 통합 테스트에서 mock `GetQtPassageContentContextUseCase`, mock `GetBibleVerseUseCase`, mock `LlmClient`, 실제 `ExplanationGenerationJobHandler`, 실제 `AiGenerationJobRunner`를 조립한다.
5. 정상 `EXPLANATION` job 실행 시 `ai_generated_assets` row가 1건 저장되고, asset 상태가 `VALIDATING`, job 상태가 `SUCCEEDED`, payload에 `explanations[]`, `glossaryTerms[]`, `promptVersionId`, `modelName`, `sourceMetadata.verseIds`가 들어가는지 검증한다.
6. LLM 응답 JSON이 깨진 경우 job이 `FAILED`로 끝나고 asset row가 0건인지 검증한다.
7. 입력 범위 밖 `verseId`가 응답에 들어온 경우 job이 `FAILED`로 끝나고 asset row가 0건인지 검증한다.
8. `SIMULATOR` job 실행 시 `LlmClient`가 호출되지 않고 job이 `FAILED`, `errorMessage=SIMULATOR_GENERATION_DISABLED`로 저장되는지 검증한다.
9. 테스트 실패가 실제 구현 결함이면 runner/handler/repository만 최소 수정한다.
10. 전체 AI 관련 테스트와 전체 test/build를 실행한다.
11. report를 PR 3 기준으로 정정한다. 통합 테스트가 통과하면 완료로, 미통과 항목이 있으면 부분 완료와 남은 항목으로 기록한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `AiGenerationJobRepositoryTest` | `QUEUED` job id만 오래된 순서로 batch size만큼 조회 |
| `AiGenerationJobRepositoryTest` | claim 조회는 id와 `QUEUED` status가 모두 맞을 때만 반환 |
| `AiGenerationJobRunnerIntegrationTest` | 정상 EXPLANATION job이 DB에서 `SUCCEEDED`로 끝나고 `VALIDATING` asset 1건 저장 |
| `AiGenerationJobRunnerIntegrationTest` | 저장 payload가 verseId 기준이며 prompt/provider raw response, prompt 원문, validation reference 원문을 포함하지 않음 |
| `AiGenerationJobRunnerIntegrationTest` | invalid JSON 응답은 asset 없이 job `FAILED` |
| `AiGenerationJobRunnerIntegrationTest` | out-of-scope verseId 응답은 asset 없이 job `FAILED` |
| `AiGenerationJobRunnerIntegrationTest` | SIMULATOR job은 LLM 호출 없이 disabled 사유로 job `FAILED` |

## 수용 기준

- [ ] 실제 JPA repository에 저장한 `QUEUED` EXPLANATION job이 runner 실행 후 `SUCCEEDED`가 된다.
- [ ] 같은 실행에서 `ai_generated_assets`에 `VALIDATING` asset 1건이 저장된다.
- [ ] 저장 payload는 `explanations[]`, `glossaryTerms[]`, prompt version/hash, model, token usage, source metadata를 포함한다.
- [ ] payload는 prompt 원문, provider raw response, validation reference 원문, secret을 저장하지 않는다.
- [ ] LLM 오류나 응답 검증 실패는 asset 없이 job `FAILED`와 짧은 `errorMessage`로 남는다.
- [ ] SIMULATOR job은 LLM 호출 없이 `SIMULATOR_GENERATION_DISABLED`로 실패한다.
- [ ] controller는 LLM client나 repository를 직접 호출하지 않는다.
- [ ] 다른 도메인 의존은 `api` UseCase로만 연결된다.
- [ ] report가 PR 3 기준의 완료 여부와 미완료 항목을 정확히 기록한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 이번 작업은 구현 확장보다 runner, transaction, repository, handler를 한 흐름으로 검증하는 통합 보강이다.
- 테스트 실패 원인을 같은 맥락에서 즉시 구현 코드에 반영해야 하므로 병렬화보다 순차 TDD가 안전하다.
- 편집 경로가 `domain.ai.internal`에 집중되어 병렬 작업 시 충돌 가능성이 높다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 repository 테스트, runner 통합 테스트, 최소 구현 보정, report 정정을 순서대로 직접 수행한다.

## 검증 계획

- `.\qtai-server\gradlew.bat -p qtai-server test --tests "*AiGenerationJob*"`
- `.\qtai-server\gradlew.bat -p qtai-server test --tests "*ExplanationGenerationJobHandlerTest"`
- `.\qtai-server\gradlew.bat -p qtai-server test`
- `.\qtai-server\gradlew.bat -p qtai-server build`
- `git diff --check`
- `.\qtai-server\gradlew.bat -p qtai-server test jacocoTestReport`는 태스크가 없으면 report에 실행 불가 사유를 남긴다.
- `.\qtai-server\gradlew.bat -p qtai-server jacocoTestCoverageVerification`는 태스크가 없으면 report에 실행 불가 사유를 남긴다.
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`는 ruleset이 없으면 report에 실행 불가 사유를 남긴다.
- `gitleaks detect --source . --redact --exit-code 1`는 로컬 명령이 없으면 report에 실행 불가 사유를 남긴다.

## 후속 작업으로 남길 항목

- PR 4: 자동 검증 최소 구현과 `ai_validation_logs` 기록 연결.
- PR 5: timeout, 429, 5xx, 파싱 실패, 검증 실패에 대한 실패/재시도 정책 정리.
- PR 6: 04:00 KST scheduler, 시스템 API 수동 트리거, Spring Batch 중 운영 실행 방식 결정.
- 승인 후 `verse_explanations`, `glossary_terms`, `simulator_clips` 반영.
- generation job 운영 모니터링과 재처리 API 보강.
