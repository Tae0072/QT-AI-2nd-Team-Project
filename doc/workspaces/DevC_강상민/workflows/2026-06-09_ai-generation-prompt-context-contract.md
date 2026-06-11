# Workflow - 2026-06-09 ai-generation-prompt-context-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-generation-prompt-context-contract` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | AI MSA 남은 작업 스케줄의 다음 작업이 `ai-generation-prompt-context-contract`로 지정됨 |
| 기준 문서 | `doc/workspaces/DevC_강상민/2026-06-09_ai-msa-work-schedule.md` |
| 대상 경로 | `doc/workspaces/DevC_강상민/**`, `qtai-server/ai-service/src/test/**` |

## 작업 목표

real executor 구현 전에 generation worker가 받을 입력, provider context 조회 결과를 어떻게 사용할지, executor result와 fixture에 저장하면 안 되는 필드를 계약으로 고정한다.

이번 작업은 문서와 test fixture 기준의 계약 고정이다. 실제 DeepSeek 호출, provider live 조회, real executor 구현, Kafka, gateway 전환은 진행하지 않는다.

## 범위

- prompt/context 계약 문서를 새로 작성한다.
- prompt/context fixture JSON을 `ai-service` 테스트 리소스에 추가한다.
- fixture가 `AiGenerationWorkerJob`과 `AiGenerationWorkerResult` 계약에 맞는지 테스트한다.
- 스케줄 문서에서 이번 작업을 완료로 표기하고 다음 작업을 `ai-generation-deepseek-client-adapter`로 변경한다.
- report 문서에 실행 결과와 후속 작업을 기록한다.

## 제외 범위

- production executor 구현
- DeepSeek HTTP client adapter 구현
- prompt 본문 저장 또는 migration
- provider live endpoint 호출
- Kafka 의존성, topic, relay, consumer 구현
- gateway route enable 또는 cutover
- monolith AI 코드 삭제

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/2026-06-09_ai-generation-prompt-context-contract.md` | prompt/context 계약 본문 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-generation-prompt-context-contract_report.md` | 작업 결과 리포트 |
| Create | `qtai-server/ai-service/src/test/resources/contracts/ai-generation/prompt-context-contract-fixtures.json` | 계약 fixture |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiGenerationPromptContextContractTest.java` | fixture 계약 테스트 |
| Modify | `doc/workspaces/DevC_강상민/2026-06-09_ai-msa-work-schedule.md` | 현재 완료/다음 작업 상태 업데이트 |

## 구현 순서

1. 현재 브랜치와 작업트리 상태를 확인한다.
2. 계약 문서에 worker input, prompt metadata, QT context, Bible reference, result payload, 금지 저장 필드를 고정한다.
3. fixture JSON을 작성한다.
4. fixture 테스트를 작성해서 `AiGenerationWorkerJob` 생성, `AiGenerationWorkerResult` 검증, 금지 필드 차단을 확인한다.
5. 스케줄 문서를 업데이트한다.
6. report 문서를 작성한다.
7. 지정 검증 명령을 실행하고 결과를 report에 반영한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiGenerationPromptContextContractTest` | fixture가 worker input과 prompt/context/result 계약을 만족하는지 검증 |
| `AiGenerationPromptContextContractTest` | prompt 본문, provider raw response, 본문 원문, 인증/접속 값 필드가 result payload에 들어갈 수 없는지 검증 |

## 수용 기준

- [ ] prompt/context 계약 문서가 생성된다.
- [ ] fixture JSON이 생성되고 금지 데이터/민감 값 예시를 포함하지 않는다.
- [ ] fixture 테스트가 `AiGenerationWorkerJob`과 `AiGenerationWorkerResult` 계약을 검증한다.
- [ ] 스케줄 문서에서 이번 작업은 완료, 다음 작업은 `ai-generation-deepseek-client-adapter`로 표시된다.
- [ ] 실제 DeepSeek/provider/Kafka/gateway 구현 변경이 없다.
- [ ] 지정 검증 명령이 통과한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 계약 문서, fixture, 테스트, 스케줄 업데이트가 같은 계약 판단을 공유한다.
- fixture 필드와 테스트 assertion이 어긋나면 바로 실패하므로 한 흐름에서 직접 작성하는 편이 안전하다.
- 변경 범위가 작고 병렬 편집 이점보다 충돌 위험이 크다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성 후 직접 실행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiGenerationPromptContextContractTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerExecutorContractTest
cd ..
git diff --check
```

문서와 fixture는 placeholder 문구와 금지 데이터/민감 값 예시가 없는지 별도 검색으로 확인한다.

## 후속 작업으로 남기는 항목

- `ai-generation-deepseek-client-adapter`
- `ai-generation-real-executor-implementation`
- `ai-generation-real-executor-runtime-smoke`
- `ai-event-outbox-relay-design`
