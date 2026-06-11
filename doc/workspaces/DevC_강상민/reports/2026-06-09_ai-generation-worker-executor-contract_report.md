# Report - 2026-06-09 ai-generation-worker-executor-contract

## 작업 요약

`AiGenerationWorkerExecutor`의 입력/출력 계약을 코드와 테스트로 고정했다. 실제 DeepSeek/LLM executor, provider live 호출, scheduler 운영 활성화, Kafka/relay 구현은 수행하지 않았다.

## 변경 내용

- `AiGenerationWorkerExecutor`에 job snapshot/result 계약 주석을 추가했다.
- `AiGenerationWorkerResult`에서 `payloadJson` JSON object 여부를 검증한다.
- `AiGenerationWorkerResult`에서 금지 필드 저장을 기존 `AiJsonStorageGuard`로 차단한다.
- `AiGenerationWorkerResult`에서 `sourceLabel` null/blank를 차단한다.
- `AiGenerationWorkerExecutorContractTest`를 추가해 job/result 계약을 검증한다.
- `AiGenerationWorkerSkeletonTest`에 executor input snapshot과 result 계약 위반 실패 흐름을 보강했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 통과 |
| `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiGenerationWorkerExecutorContractTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerSkeletonTest --tests com.qtai.ai.AiGenerationWorkerEnabledContextTest --tests com.qtai.domain.ai.internal.AiServicePersistenceDomainPolicyTest` | 통과 |
| `git diff --check` | 통과 |
| ai-service main source의 LLM/Kafka 관련 구현 검색 | 매칭 없음 |
| 변경 범위 금지 데이터/민감 예시 문구 검색 | 통과 |

## 수용 기준 확인

- job 필수 필드 null/0 이하가 차단된다.
- result `assetType` null이 차단된다.
- result `payloadJson` blank/invalid/non-object JSON이 차단된다.
- result payload 금지 필드가 차단된다.
- result `sourceLabel` null/blank가 차단된다.
- allowed JSON object result는 허용된다.
- result 계약 위반 시 worker는 job을 `FAILED`로 만들고 asset을 저장하지 않는다.

## 제외 범위 확인

- DeepSeek/LLM executor를 구현하지 않았다.
- provider live endpoint를 호출하지 않았다.
- scheduler 운영 활성화를 하지 않았다.
- Kafka, topic, producer, consumer, relay worker를 추가하지 않았다.
- gateway route를 변경하지 않았다.
- 운영 DB migration을 변경하지 않았다.
- monolith AI 코드를 삭제하지 않았다.

## 후속 작업

- production `AiGenerationWorkerExecutor` 구현
- worker runtime smoke readiness
- outbox relay worker 설계 및 skeleton
