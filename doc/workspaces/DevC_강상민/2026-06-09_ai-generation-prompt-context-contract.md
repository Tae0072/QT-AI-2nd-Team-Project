# ai-generation-prompt-context-contract

> 작성자: DevC 강상민
> 기준일: 2026-06-09
> 목적: real executor 구현 전에 generation worker 입력, provider context 사용 범위, 저장 금지 필드 기준을 계약으로 고정한다.

## 1. 결정 요약

`AiGenerationWorkerExecutor`는 현재 worker가 claim한 job snapshot을 입력으로 받는다. 이번 계약은 그 입력 위에 prompt metadata, provider context, result payload가 어떤 형태로 이어지는지 고정한다.

real executor는 provider context와 prompt metadata를 실행 중 일시적으로 사용할 수 있다. 그러나 executor result payload, outbox payload, fixture, log에는 prompt 본문, provider raw response, 본문 원문, 인증 값, DB 접속 값을 저장하지 않는다.

## 2. Worker Input 계약

worker input은 기존 `AiGenerationWorkerJob` record를 그대로 사용한다.

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `jobId` | 예 | `ai_generation_jobs` 식별자 |
| `jobType` | 예 | 생성 유형. 현재 `EXPLANATION`, `SUMMARY`, `GLOSSARY`, `SIMULATOR`, `QA`를 허용한다. |
| `targetType` | 예 | 생성 대상 유형. QT passage 생성은 `QT_PASSAGE`를 사용한다. |
| `targetId` | 예 | 대상 식별자. `QT_PASSAGE`이면 `passageId`와 같다. |
| `promptVersionId` | 예 | 사용할 prompt version 식별자 |
| `startedAt` | 예 | worker가 job을 claim한 시각 |

## 3. Prompt Metadata 계약

real executor는 prompt 본문을 저장하지 않고 prompt version metadata만 참조한다.

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `promptVersionId` | 예 | `ai_prompt_versions.id` |
| `promptType` | 예 | job type과 매칭되는 prompt 유형 |
| `version` | 예 | 운영자가 식별할 수 있는 버전 문자열 |
| `contentHash` | 예 | prompt 본문 검증용 hash |

prompt 본문은 executor 내부 실행 입력으로만 취급한다. fixture, generated asset payload, outbox payload, log에는 prompt 본문을 남기지 않는다.

## 4. Provider Context 계약

provider endpoint가 열린 뒤에도 QT/Bible 참조 조회는 기존 HTTP client 계약을 유지한다. Provider AI input event 계약이 승인되어 provider가 허용된 context block을 event로 제공하기 전까지 HTTP 조회를 제거하지 않는다.

### QT Context

QT context는 `QtContextResult` 필드를 따른다.

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `passageId` | 예 | QT passage 식별자 |
| `bibleBook` | 예 | 성경 권 이름 또는 코드 |
| `chapter` | 예 | 장 |
| `startVerse` | 예 | 시작 절 |
| `endVerse` | 예 | 종료 절 |
| `passageReference` | 예 | 사람이 읽을 수 있는 참조 문자열 |
| `title` | 예 | 허용된 제목 metadata |
| `summary` | 예 | 허용된 요약 metadata |
| `passageContext` | 예 | AI 생성/검증에 필요한 허용된 metadata/context block |

`passageContext`는 본문 원문 전체가 아니다. passage metadata, section label, 검증된 context marker 같은 허용된 block만 담는다. QT context 조회에는 `cacheStatus`를 포함하지 않는다.

### Bible Reference

Bible client가 조회한 본문은 executor 내부 입력으로만 사용할 수 있다. result payload와 outbox payload에는 verse id와 reference만 남긴다.

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `verseId` | 예 | Bible verse 식별자 |
| `reference` | 예 | 사람이 읽을 수 있는 참조 문자열 |

## 5. Executor Result Payload 계약

`AiGenerationWorkerResult`는 JSON object payload만 허용한다. payload는 생성 결과 요약 구조와 출처 식별자만 담는다.

허용 예시는 다음 범위다.

| 필드 | 설명 |
| --- | --- |
| `summary` | 생성 결과 요약 |
| `explanation` | 생성 결과 본문. 외부 raw response가 아니라 내부 결과 구조다. |
| `sourceRefs` | `QT_PASSAGE`, `BIBLE_VERSE` 같은 식별자와 reference |
| `generationMetadata` | `schemaVersion`, `promptVersionId`, `contextBlockType` 같은 실행 metadata |

금지 저장 범위는 다음과 같다.

| 금지 범위 | 이유 |
| --- | --- |
| prompt 본문 | prompt version metadata만 저장한다. |
| provider raw response | 외부 응답 원문 저장은 재처리와 보안 검토 범위를 키운다. |
| 본문 원문 | 허용되지 않은 본문 저장을 방지한다. |
| 인증 값 | 내부 서비스 인증과 외부 API 인증 값 노출을 막는다. |
| DB 접속 값 | 운영 접속 정보 노출을 막는다. |

## 6. Fixture 기준

fixture는 `qtai-server/ai-service/src/test/resources/contracts/ai-generation/prompt-context-contract-fixtures.json`에 둔다.

fixture는 다음을 검증한다.

- `job` section이 `AiGenerationWorkerJob` 생성자 검증을 통과한다.
- `promptVersion` section에 metadata만 있고 prompt 본문 필드가 없다.
- `providerContext.qtContext`에 `passageContext`가 있고 `cacheStatus`가 없다.
- `providerContext.bibleVerseRefs`는 id/reference 중심이며 본문 text 필드를 포함하지 않는다.
- `expectedResult.payloadJson`은 `AiGenerationWorkerResult` 검증을 통과한다.
- 금지 payload 예시는 `AiGenerationWorkerResult`에서 거부된다.

## 7. 다음 작업 연결

이 계약이 완료되면 다음 작업은 `ai-generation-deepseek-client-adapter`다. 해당 작업은 DeepSeek 호환 HTTP 호출 계층을 executor와 분리해서 추가하되, prompt/context 저장 금지 기준은 이 문서를 따른다.
