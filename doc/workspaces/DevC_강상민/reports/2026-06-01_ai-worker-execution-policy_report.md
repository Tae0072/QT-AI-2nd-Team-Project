# AI worker 실행 방식 결정 리포트

- 작업일: 2026-06-01
- 작업 브랜치: `feature/ai-worker-execution-policy`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-worker-execution-policy.md`
- 관련 기능: F-02, F-14

## 1. 실행 방식 결정

MVP AI generation 실행 방식은 하이브리드 모델로 확정했다.

- job 생성: 시스템 API와 관리자 재생성 API가 `QUEUED` generation job을 생성한다.
- job 처리: 애플리케이션 내부 `AiGenerationJobWorker`가 fixed-delay polling으로 `QUEUED` job을 처리한다.
- 04:00 KST는 운영 트리거 시각으로 둔다.
- 서버 내부에서 일일 대상 탐색과 generation job 생성을 수행하는 04:00 cron scheduler는 이번 범위에 추가하지 않았다.

## 2. 반영 내용

### 2.1 worker 처리 로그 보강

`AiGenerationJobWorker`가 `runner.runQueuedBatch(batchSize)` 반환값을 확인하고, 처리 건수가 1 이상일 때만 아래 형식의 운영 로그를 남기도록 했다.

```text
AI generation worker processed jobs. processedCount=<count>
```

처리 건수가 0이면 반복 polling noise를 줄이기 위해 처리 건수 로그를 남기지 않는다.

### 2.2 worker 실패 로그 보강

worker polling 중 예외가 발생해도 scheduler thread 밖으로 전파하지 않는 기존 정책을 유지했다. 실패 로그에는 exception type과 message만 남긴다.

```text
AI generation worker polling failed. errorType=<type>, errorMessage=<message>
```

prompt 원문, provider raw response, validation reference 원문, secret 계열 값은 로그에 추가하지 않았다.

### 2.3 scheduling 설정 중복 정리

`@EnableScheduling`은 `SchedulingConfig`에서 관리하도록 두고, `QtAiApplication`의 중복 annotation을 제거했다.

## 3. 범위 제외

- 04:00 KST cron scheduler가 generation job을 직접 생성하는 구현
- Spring Batch 도입
- retry count, backoff, next retry time, dead letter queue 구현
- 멈춘 `RUNNING` job 회수 구현
- DB migration, 신규 컬럼, 신규 enum
- OpenAPI 계약 변경
- 사용자 API `/api/v1/ai/**` 생성 경로 추가

## 4. 검증 기록

실행 완료:

```powershell
.\gradlew.bat test --tests "*AiGenerationJobWorkerTest"
.\gradlew.bat test --tests "*AiGenerationJob*"
.\gradlew.bat build
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai
rg -n "providerRawResponse|rawResponse|validationReferenceText|promptText|password|private key|token|secret" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/test/java/com/qtai/domain/ai
git diff --check
rg -n "@EnableScheduling" qtai-server/src/main/java
```

결과:

- 최초 실행은 새 로그 검증 테스트 기준으로 실패해 RED를 확인했다.
- worker 처리 로그와 실패 로그를 구현한 뒤 재실행하여 통과했다.
- `AiGenerationJob*` 테스트와 전체 `build`가 통과했다.
- 도메인 경계 금지 import 검색 결과는 없었다.
- 민감 키워드 검색 결과는 기존 저장 금지 guard, 관련 테스트, token usage 필드로 확인했다.
- `git diff --check`는 통과했고, Git line ending 경고만 출력했다.
- `@EnableScheduling`은 `SchedulingConfig` 한 곳에만 남아 있다.

실행 불가:

- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`: 저장소 루트에 `.spectral.yaml` 없음
- `gitleaks detect --source . --redact --exit-code 1`: 로컬 환경에 `gitleaks` 명령 없음

## 5. 수용 기준 점검

- 시스템/관리자 API producer + fixed-delay worker consumer 모델로 문서화했다.
- 04:00 KST cron scheduler, Spring Batch, DB/OpenAPI 변경은 추가하지 않았다.
- worker disabled/enabled, batch size, 예외 흡수, 처리 건수 로그 기준을 테스트로 보강했다.
- scheduling 활성화는 `SchedulingConfig` 한 곳에서 관리하도록 정리했다.

## 6. 후속 작업 후보

- 외부 scheduler 또는 운영 수동 트리거가 04:00 KST에 system API를 호출하는 배포/운영 구성을 정한다.
- 일일 QT 대상 탐색과 generation job 자동 생성 정책을 별도 PR에서 다룬다.
- retry/backoff와 멈춘 `RUNNING` job 회수 정책을 별도 PR에서 구현한다.
