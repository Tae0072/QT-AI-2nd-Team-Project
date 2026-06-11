# Workflow - 2026-06-09 ai-generation-worker-executor-contract

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-generation-worker-executor-contract` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `feature/ai-generation-worker-executor-contract` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | generation worker skeleton 이후 실제 LLM executor 구현 전에 입력/출력 계약과 저장 금지 정책을 고정해야 한다. |
| 기준 문서 | `2026-06-09_ai-generation-worker-design.md`, `2026-06-09_ai-event-outbox-decision-record.md` |
| 대상 경로 | `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`AiGenerationWorkerExecutor`의 입력 job snapshot과 출력 result 계약을 코드와 테스트로 고정한다. 실제 DeepSeek/LLM 호출은 만들지 않고, 나중에 production executor를 붙일 때 worker/scheduler 경계를 흔들지 않게 한다.

## 범위

- `AiGenerationWorkerExecutor`에 계약 주석과 result 검증을 추가한다.
- `AiGenerationWorkerJob` 필수 필드 계약을 신규 테스트로 고정한다.
- `AiGenerationWorkerResult`의 JSON object payload, sourceLabel, 금지 필드 차단을 신규 테스트로 고정한다.
- worker skeleton 테스트에 executor 입력 snapshot과 result 계약 위반 실패 흐름을 보강한다.

## 제외 범위

- DeepSeek/LLM executor 구현
- provider live endpoint 호출
- scheduler 운영 활성화
- Kafka, topic, producer, consumer, relay worker 구현
- gateway route 변경
- 운영 DB migration 변경
- monolith AI 코드 삭제

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/AiGenerationWorkerExecutor.java` | executor job/result 계약 검증 |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiGenerationWorkerExecutorContractTest.java` | executor 계약 단위 테스트 |
| Modify | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiGenerationWorkerSkeletonTest.java` | worker failure path 회귀 보강 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-generation-worker-executor-contract.md` | 작업 기준 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-generation-worker-executor-contract_report.md` | 검증 결과 |

## 구현 순서

1. `dev` 최신화 후 `feature/ai-generation-worker-executor-contract` 브랜치를 만든다.
2. workflow 문서를 작성한다.
3. `AiGenerationWorkerExecutor` result 생성자에 payload/sourceLabel 계약 검증을 추가한다.
4. `AiGenerationWorkerExecutorContractTest`를 추가한다.
5. `AiGenerationWorkerSkeletonTest`에서 executor가 받은 job snapshot 전체와 result 계약 위반 실패 흐름을 검증한다.
6. compile/test/guard 검증을 실행한다.
7. report 문서를 작성한다.
8. 지정 파일만 stage 후 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiGenerationWorkerExecutorContractTest` | job 필수 필드, result JSON object, sourceLabel, 금지 필드 차단 |
| `AiGenerationWorkerSkeletonTest` | executor 입력 snapshot, invalid result 실패 처리 |
| `AiGenerationWorkerEnabledContextTest` | worker enabled context 회귀 |
| `AiServicePersistenceDomainPolicyTest` | JSON guard 공유 정책 회귀 |

## 수용 기준

- [ ] job 필수 필드 null/0 이하가 차단된다.
- [ ] result `assetType` null이 차단된다.
- [ ] result `payloadJson` blank/invalid/non-object JSON이 차단된다.
- [ ] result payload 금지 필드가 차단된다.
- [ ] result `sourceLabel` null/blank가 차단된다.
- [ ] allowed JSON object result는 허용된다.
- [ ] result 계약 위반 시 worker는 job을 `FAILED`로 만들고 asset을 저장하지 않는다.
- [ ] 실제 LLM, Kafka, provider live 호출, scheduler 운영 활성화가 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- executor record 계약과 worker failure path가 같은 내부 경계를 공유한다.
- 테스트 fixture와 production 검증을 같은 순서로 맞춰야 해서 직접 실행이 안전하다.
- 변경 범위가 ai-service internal 계약과 문서 2개로 제한된다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 구현, 테스트, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiGenerationWorkerExecutorContractTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerSkeletonTest --tests com.qtai.ai.AiGenerationWorkerEnabledContextTest --tests com.qtai.domain.ai.internal.AiServicePersistenceDomainPolicyTest
cd ..
git diff --check
rg -n "DeepSeek|LlmClient|external\.llm|Kafka|topic" "qtai-server\ai-service\src\main\java"
$forbiddenPattern = ("개역" + "개정") + "|" + ("E" + "SV") + "|" + ("N" + "IV") + "|" + ("성서" + "유니온") + "|" + ("두" + "란노") + "|" + ("plain " + "secret") + "|" + ("private " + "key")
rg -n $forbiddenPattern "qtai-server\ai-service" "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-generation-worker-executor-contract.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-generation-worker-executor-contract_report.md"
```

## 다음 작업으로 넘길 항목

- production `AiGenerationWorkerExecutor` 구현
- worker runtime smoke readiness
- outbox relay worker 설계 및 skeleton
