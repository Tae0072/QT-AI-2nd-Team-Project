# Workflow - 2026-06-01 ai-worker-execution-policy

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-worker-execution-policy` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 트리거 | AI 구현 순서 10번: 배치 또는 worker 실행 방식 결정 |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/25_기능_명세서.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-28_ai-implementation-order.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/main/java/com/qtai/config/**`, `qtai-server/src/main/java/com/qtai/QtAiApplication.java`, `qtai-server/src/main/resources/application.yml`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

AI 생성 작업의 MVP 실행 방식을 명확히 고정한다. 이번 PR의 결정은 하이브리드 방식이다. 시스템 API와 관리자 재생성 API가 `QUEUED` generation job을 만들고, 애플리케이션 내부 `AiGenerationJobWorker`가 fixed-delay polling으로 queue를 처리한다.

04:00 KST는 운영 트리거 시각으로만 둔다. MVP 서버 안에서 일일 대상 탐색과 job 생성을 수행하는 04:00 cron scheduler나 Spring Batch job은 추가하지 않는다. 이번 작업은 기존 worker 동작을 운영 정책에 맞게 문서화하고, 필요한 최소 로그/테스트/설정 정리만 수행한다.

## 범위

- AI generation 실행 모델을 하이브리드 방식으로 확정한다.
- job 생성 주체를 `/api/v1/system/ai/generation-jobs`와 `/api/v1/admin/ai/assets/{assetId}/regenerate`로 정리한다.
- job 처리 주체를 `AiGenerationJobWorker` fixed-delay polling으로 정리한다.
- 기존 worker 설정 기본값을 유지한다.
  - `AI_GENERATION_WORKER_ENABLED=true`
  - `AI_GENERATION_WORKER_FIXED_DELAY_MS=10000`
  - `AI_GENERATION_WORKER_BATCH_SIZE=5`
- worker가 실제 처리한 job 수를 운영 로그로 확인할 수 있게 최소 보강한다.
- scheduler 활성화 설정이 중복되어 있으면 한 곳으로 정리한다.
- 실행 정책과 검증 결과를 report로 남긴다.

## 제외 범위

- 앱 내부 04:00 KST cron이 generation job을 직접 생성하는 구현.
- Spring Batch 도입.
- retry count, backoff, next retry time, dead letter queue 구현.
- 멈춘 `RUNNING` job 회수 정책과 구현.
- DB migration, 신규 컬럼, 신규 enum 추가.
- OpenAPI 계약 변경.
- Today QT 캐시 교체, `verse_explanations`, `glossary_terms`, `simulator_clips` 반영.
- 사용자 요청 경로 `/api/v1/ai/**` 생성 API 추가.
- provider raw response, prompt 원문, validation reference 원문, secret 저장.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Inspect/Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobWorker.java` | fixed-delay worker 운영 로그와 예외 처리 보강 |
| Inspect | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRunner.java` | queue 처리 흐름이 기존 정책과 맞는지 확인 |
| Modify | `qtai-server/src/main/java/com/qtai/QtAiApplication.java` 또는 `qtai-server/src/main/java/com/qtai/config/SchedulingConfig.java` | `@EnableScheduling` 중복이 있으면 한 곳만 유지 |
| Inspect | `qtai-server/src/main/resources/application.yml` | worker env 기본값이 정책과 일치하는지 확인 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobWorkerTest.java` | enabled/disabled, batch size, 예외 흡수, 처리 로그 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobRunnerIntegrationTest.java` | 기존 queued job 처리 성공/실패 흐름 회귀 확인 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-01_ai-worker-execution-policy_report.md` | 실행 방식 결정, 변경 내용, 검증 결과, 제외 범위 기록 |

## 구현 순서

1. 현재 `AiGenerationJobWorker`가 `ai.generation.worker.enabled`, `fixed-delay-ms`, `batch-size` 설정을 사용하는지 확인한다.
2. `application.yml`의 worker 기본값이 `enabled=true`, `fixed-delay-ms=10000`, `batch-size=5`인지 확인하고, 다르면 정책값으로 맞춘다.
3. `@EnableScheduling`이 `QtAiApplication`과 `SchedulingConfig`에 중복 선언되어 있는지 확인한다.
4. 중복이면 `SchedulingConfig`를 scheduling 활성화 책임 위치로 유지하고 `QtAiApplication`의 annotation을 제거한다.
5. `AiGenerationJobWorkerTest`에 기존 disabled/enabled/exception 테스트가 유지되는지 확인한다.
6. worker가 `runner.runQueuedBatch(batchSize)` 반환값을 받아 처리 건수가 1 이상일 때만 `processedCount` 로그를 남기도록 테스트를 먼저 추가한다.
7. `AiGenerationJobWorker`에 처리 건수 로그를 최소 구현한다.
8. worker polling 실패 로그는 exception class와 message 수준만 남기고, prompt/provider raw response/secret을 포함하지 않도록 유지한다.
9. 실행 방식 결정을 report에 기록한다.
10. 관련 AI generation worker 테스트와 전체 build를 실행한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 또는 확인할 검증 |
| --- | --- |
| `AiGenerationJobWorkerTest` | worker disabled면 runner를 호출하지 않는다 |
| `AiGenerationJobWorkerTest` | worker enabled면 설정된 batch size로 `runQueuedBatch`를 호출한다 |
| `AiGenerationJobWorkerTest` | runner 예외가 scheduler 밖으로 전파되지 않는다 |
| `AiGenerationJobWorkerTest` | `runQueuedBatch` 결과가 1 이상일 때 처리 건수 로그가 남는다 |
| `AiGenerationJobWorkerTest` | `runQueuedBatch` 결과가 0이면 처리 건수 로그를 남기지 않는다 |
| `AiGenerationJobRunnerIntegrationTest` | 기존 queued job 성공/실패 DB 흐름이 유지된다 |

## 수용 기준

- [ ] PR 6의 실행 방식은 시스템/관리자 API producer + fixed-delay worker consumer로 문서화된다.
- [ ] MVP 서버 내부에 일일 generation job 생성용 04:00 cron scheduler를 추가하지 않는다.
- [ ] Spring Batch를 도입하지 않는다.
- [ ] worker 기본 설정은 env 기반으로 유지된다.
- [ ] worker disabled 상태에서는 queue polling을 수행하지 않는다.
- [ ] worker enabled 상태에서는 configured batch size로 runner를 호출한다.
- [ ] worker polling 실패가 scheduler thread 밖으로 전파되지 않는다.
- [ ] worker 처리 건수가 1 이상이면 운영자가 확인 가능한 로그가 남는다.
- [ ] 로그에 prompt 원문, provider raw response, validation reference 원문, secret/token/password 값이 포함되지 않는다.
- [ ] `@EnableScheduling`은 중복 선언 없이 한 위치에서 관리된다.
- [ ] 사용자 API `/api/v1/ai/**` 생성 경로를 추가하지 않는다.
- [ ] DB schema와 OpenAPI 계약을 변경하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 worker, scheduling 설정, worker 테스트에 집중되어 병렬 작업 이점이 작다.
- logging 보강과 테스트 기대값이 같은 클래스에 강하게 연결되어 순차 TDD가 안전하다.
- 코드 변경과 report 작성이 같은 실행 정책 결정을 공유하므로 단일 agent가 끝까지 확인하는 편이 충돌 가능성이 낮다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 agent가 worker 테스트 보강, 최소 구현, scheduling 설정 정리, report 작성, 최종 검증을 직접 순서대로 수행한다.

## 검증 계획

- `.\gradlew.bat test --tests "*AiGenerationJobWorkerTest"` in `qtai-server`
- `.\gradlew.bat test --tests "*AiGenerationJob*"` in `qtai-server`
- `.\gradlew.bat build` in `qtai-server`
- `rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai`
- `rg -n "providerRawResponse|rawResponse|validationReferenceText|promptText|password|private key|token|secret" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai`
- `git diff --check`
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `gitleaks detect --source . --redact --exit-code 1`

`spectral` ruleset이나 `gitleaks` 실행 파일이 로컬에 없으면 실행하지 못한 이유를 report와 최종 응답에 기록한다.

## 후속 작업으로 남길 항목

- 실제 04:00 KST 외부 scheduler 또는 운영 수동 트리거 구성.
- 일일 QT 대상 탐색과 generation job 자동 생성 정책.
- retry count, backoff, next retry time 저장 정책.
- 멈춘 `RUNNING` job 감지와 회수 정책.
- 반복 실패 집계와 관리자 모니터링 API.
- 관리자 UI에서 worker 상태, 실패 job, 재생성 요청 상태 표시.
