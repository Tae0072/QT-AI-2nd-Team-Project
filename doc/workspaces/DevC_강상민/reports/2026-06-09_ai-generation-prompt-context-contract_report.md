# ai-generation-prompt-context-contract report

## 작업 요약

- `ai-generation-prompt-context-contract` workflow 문서를 작성했다.
- real executor 구현 전 필요한 prompt/context 입력 계약 문서를 작성했다.
- prompt metadata, QT context, Bible reference, executor result payload의 저장 허용 범위를 fixture로 고정했다.
- `AiGenerationPromptContextContractTest`를 추가해 fixture가 `AiGenerationWorkerJob`과 `AiGenerationWorkerResult` 계약을 만족하는지 검증했다.
- MSA 작업 스케줄 문서를 현재 상태 기준으로 업데이트했다.

## 변경 범위

| 구분 | 경로 | 내용 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-generation-prompt-context-contract.md` | workflow 명세 |
| Create | `doc/workspaces/DevC_강상민/2026-06-09_ai-generation-prompt-context-contract.md` | prompt/context 계약 문서 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-generation-prompt-context-contract_report.md` | 작업 리포트 |
| Create | `qtai-server/ai-service/src/test/resources/contracts/ai-generation/prompt-context-contract-fixtures.json` | 계약 fixture |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiGenerationPromptContextContractTest.java` | fixture 계약 테스트 |
| Modify | `doc/workspaces/DevC_강상민/2026-06-09_ai-msa-work-schedule.md` | 현재 완료/다음 작업 상태 업데이트 |

## 계약 결정

- worker input은 기존 `AiGenerationWorkerJob` 필드를 그대로 따른다.
- prompt는 `promptVersionId`, `promptType`, `version`, `contentHash` metadata만 계약으로 고정한다.
- `passageContext`는 허용된 metadata/context block으로 정의하며 QT context에는 `cacheStatus`를 포함하지 않는다.
- Bible 참조는 result payload와 outbox payload에 verse id/reference만 남기는 방향으로 고정한다.
- executor result payload에는 prompt 본문, provider raw response, 본문 원문, 인증 값, DB 접속 값을 저장하지 않는다.

## 제외 확인

- production executor 구현 없음
- DeepSeek HTTP client adapter 구현 없음
- provider live endpoint 호출 없음
- Kafka 의존성 또는 relay 구현 없음
- gateway route 변경 없음
- monolith AI 코드 삭제 없음

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 통과 |
| `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiGenerationPromptContextContractTest --tests com.qtai.domain.ai.internal.AiGenerationWorkerExecutorContractTest` | 통과 |
| `git diff --check` | 통과 |
| placeholder 문구 검색 | 매칭 없음 |
| 금지 데이터/민감 예시 검색 | 매칭 없음 |

## 후속 작업

다음 작업은 `ai-generation-deepseek-client-adapter`다.
