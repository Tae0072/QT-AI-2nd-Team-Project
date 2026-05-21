# Workflow — 2026-05-21 create-ai-generation-job-usecase-implementation

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-generation-trigger` |
| PR 대상 | `dev` |
| 관련 F-ID | 핵심: F-02, 보조: F-14 |
| 트리거 | "CreateAiGenerationJobUseCase 실제 구현" 작업. 범위 선택: UseCase 단독 구현 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 해당 경로 | 아래 해당 경로 목록 참고 |

## 기준 문서

- `doc/프로젝트 문서/07_요구사항_정의서.md`
- `doc/프로젝트 문서/03_아키텍처_정의서.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `doc/프로젝트 문서/23_도메인_용어사전.md`
- `doc/프로젝트 문서/25_기능_명세서.md`
- `CODE_CONVENTION.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-20_ai-usecase-contracts.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-20_ai-generation-log-model.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-21_ai-failure-retry-policy.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-21_admin-ai-generation-trigger-api.md`

## 해당 경로

- `qtai-server/src/main/java/com/qtai/domain/ai/api/**`
- `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`
- `qtai-server/src/test/java/com/qtai/domain/ai/api/**`
- `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`

## 작업 목표

`CreateAiGenerationJobUseCase`의 실제 구현을 추가해 AI 사전 생성 작업을 `ai_generation_jobs`에 `QUEUED` 상태로 등록한다. 이 UseCase는 배치나 관리자 재생성 흐름에서 호출 가능한 내부 Java 계약이며, HTTP API나 실제 LLM 실행을 직접 담당하지 않는다.

이번 작업의 핵심은 command 검증, enum 매핑, 진행 중 작업 중복 차단, 저장 결과 반환이다. 동일한 `jobType + targetType + targetId + promptVersion` 조합의 `QUEUED` 또는 `RUNNING` 작업이 있으면 새 작업 생성을 막는다.

## 범위

- `CreateAiGenerationJobUseCase` 구현체를 등록한다.
- 구현 위치는 기존 구조를 우선해 `AiService`가 함께 구현하거나, 책임이 과해지면 `CreateAiGenerationJobService`로 분리한다.
- `CreateAiGenerationJobCommand`의 `jobType`, `targetType`, `targetId`, `promptVersion`, `requestedBy`, `requestedAt`을 검증한다.
- `jobType` 문자열은 `AiGenerationJobType`, `targetType` 문자열은 `AiTargetType`으로 매핑한다.
- 이번 UseCase는 내부 Java 계약 기준으로 현재 `AiGenerationJobType`, `AiTargetType` enum 이름만 허용한다. API 명세의 `DAILY_QT_EXPLANATION`, `promptVersionId` 같은 HTTP 입력값 매핑은 시스템/관리자 HTTP API 구현 단계에서 처리한다.
- 잘못된 enum 값, blank/null 입력, 양수가 아닌 `targetId`는 `INVALID_INPUT`으로 차단한다.
- 같은 `jobType + targetType + targetId + promptVersion` 기준의 `QUEUED` 또는 `RUNNING` 작업이 있으면 새 job을 저장하지 않고 프로젝트에서 확정한 중복/상태 전이 예외로 실패한다.
- 성공 시 `AiGenerationJob.queue(...)`로 새 작업을 만들고 repository에 저장한다.
- 결과는 `CreateAiGenerationJobResult(generationJobId, status)`로 반환한다.
- write use case이므로 구현 메서드에 `@Transactional`을 적용한다.
- `requestedBy`는 현재 `CreateAiGenerationJobCommand` 계약 호환을 위해 필수 검증만 수행한다. 현 Entity에 저장 위치가 없으므로 이번 구현에서는 저장하지 않는다.
- AI 도메인은 다른 도메인의 `internal`, `web`, `repository` 타입을 직접 import하지 않는다.

## 제외 범위

- `POST /api/v1/system/ai/generation-jobs` HTTP Controller 구현은 제외한다.
- `POST /api/v1/admin/ai/assets/{assetId}/regenerate` 관리자 API 연동은 제외한다.
- 특정 asset 조회, 관리자 권한 검증, 감사 로그 기록은 제외한다.
- 실제 DeepSeek 호출, 산출물 생성, 검증 로그 등록은 제외한다.
- `ai_prompt_versions` 조회나 `promptVersionId` 매핑 구조 변경은 제외한다.
- `requestedBy`를 `ai_generation_jobs`에 저장하거나 감사 로그와 연결하는 작업은 제외한다.
- `inputHash` 저장 컬럼 또는 유니크 인덱스 추가는 제외한다.
- DB migration은 현재 Entity/Repository로 구현 가능한 범위에서는 추가하지 않는다.

## 구현 순서

1. `CreateAiGenerationJobUseCase`, `CreateAiGenerationJobCommand`, `CreateAiGenerationJobResult`의 현재 계약을 확인한다.
2. `AiGenerationJob`, `AiGenerationJobRepository`, `AiGenerationJobType`, `AiTargetType`, `AiGenerationJobStatus`의 기존 메서드와 enum 값을 확인한다.
3. 구현 클래스를 결정한다. 기존 `AiService`가 여러 AI UseCase를 구현하는 패턴을 유지할지, 별도 service를 둘지 현재 코드 응집도를 기준으로 판단한다.
4. command null, 필수 문자열, 양수 target, `requestedAt` null 검증을 추가한다.
5. `jobType`과 `targetType` 문자열을 enum으로 안전하게 변환하고 실패 시 `BusinessException(ErrorCode.INVALID_INPUT)`을 던진다.
6. `generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn(...)`로 진행 중 작업을 확인한다.
7. 진행 중 작업이 있으면 새 job을 저장하지 않고 프로젝트에서 확정한 중복/상태 전이 예외로 실패시킨다.
8. 진행 중 작업이 없으면 `AiGenerationJob.queue(...)`를 호출해 저장한다.
9. 저장 결과의 id와 status를 `CreateAiGenerationJobResult`로 반환한다.
10. 단위 테스트와 UseCase 계약 테스트를 보강한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` 또는 `CreateAiGenerationJobServiceTest.java` | 정상 command가 `QUEUED` job을 저장하고 id/status를 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` 또는 `CreateAiGenerationJobServiceTest.java` | 같은 job/target/prompt의 `QUEUED` 또는 `RUNNING` 작업이 있으면 `repository.save`를 호출하지 않는다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` 또는 `CreateAiGenerationJobServiceTest.java` | null command를 `INVALID_INPUT`으로 차단한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` 또는 `CreateAiGenerationJobServiceTest.java` | 잘못된 `jobType`, `targetType`은 `INVALID_INPUT`으로 실패한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` 또는 `CreateAiGenerationJobServiceTest.java` | blank `promptVersion`, blank `requestedBy`, null `requestedAt`, 양수가 아닌 `targetId`를 차단한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | `CreateAiGenerationJobUseCase`가 command/result record 규칙을 계속 만족한다 |

## 수용 기준

- [ ] `CreateAiGenerationJobUseCase`가 Spring bean 구현체로 등록된다.
- [ ] 정상 command로 `ai_generation_jobs`에 `QUEUED` 작업이 생성된다.
- [ ] 결과 DTO가 생성된 job id와 `QUEUED` 상태를 반환한다.
- [ ] `QUEUED` 또는 `RUNNING` 중복 작업이 있으면 새 job을 저장하지 않고 프로젝트에서 확정한 중복/상태 전이 예외로 실패한다.
- [ ] 잘못된 enum 문자열과 필수 입력 누락은 `INVALID_INPUT`으로 차단된다.
- [ ] `DAILY_QT_EXPLANATION`, `promptVersionId` 같은 HTTP 입력값 매핑은 이번 UseCase 구현에 섞이지 않는다.
- [ ] `requestedBy`는 command 계약 호환을 위해 필수 검증만 수행하고, 현 Entity에 저장 위치가 없으므로 저장하지 않는다.
- [ ] write use case 메서드에 `@Transactional`이 적용된다.
- [ ] HTTP Controller, 관리자 권한, 감사 로그, DeepSeek 호출은 이번 작업에 섞이지 않는다.
- [ ] AI 도메인의 금지 import가 추가되지 않는다.
- [ ] prompt 원문, provider raw response, 검증 참조 원문, secret, token은 DTO나 로그에 남지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `CreateAiGenerationJobUseCase` 구현과 해당 단위 테스트에 집중되어 병렬화 이점이 작다.
- 구현 클래스 선택, 중복 차단 기준, 테스트 기대값이 같은 맥락에 있어 한 흐름에서 직접 처리하는 편이 안전하다.
- HTTP API, 관리자 재생성, 실제 LLM 실행은 제외되어 독립 위임할 큰 하위 작업이 없다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 구현체 추가, 테스트 보강, 경계 검증을 직접 수행한다.

## 검증 계획

- `./gradlew -p qtai-server test --tests "*AiUseCaseContractTest"`
- `./gradlew -p qtai-server test --tests "*AiServiceTest"`
- `./gradlew -p qtai-server test --tests "*AiGenerationJobTest"`
- `./gradlew -p qtai-server test`
- `rg -n "domain\\.[a-z]+\\.internal|domain\\.[a-z]+\\.web" qtai-server/src/main/java/com/qtai/domain/ai`로 금지 import 여부 확인
- 변경 범위가 OpenAPI 파일을 수정하지 않으므로 Spectral lint는 실행하지 않는다.

## 후속 작업으로 남길 항목

- `POST /api/v1/system/ai/generation-jobs` HTTP API 구현
- 관리자 재생성 API에서 `CreateAiGenerationJobUseCase` 호출 연결
- `promptVersionId`와 `promptVersion` 문자열의 최종 매핑 정책 확정
- `requestedBy` 저장 위치 또는 감사 로그 연결 정책 확정
- `inputHash` 저장 위치와 중복 방지 인덱스 설계
- 실제 DeepSeek 호출, 산출물 생성, 검증 로그 등록 파이프라인 연결
- 감사 로그 UseCase 계약 확정 후 관리자 트리거 감사 기록 연결
