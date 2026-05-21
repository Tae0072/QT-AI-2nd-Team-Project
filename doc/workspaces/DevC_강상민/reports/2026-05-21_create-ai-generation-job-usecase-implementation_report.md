# Report — 2026-05-21 create-ai-generation-job-usecase-implementation

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-generation-trigger` |
| PR 대상 | `dev` |
| 실행 경로 | 직접 실행 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-21_create-ai-generation-job-usecase-implementation.md` |
| 관련 F-ID | F-02, F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/reports/2026-05-21_create-ai-generation-job-usecase-implementation_report.md` |

## 작업 결과

`CreateAiGenerationJobUseCase`의 실제 구현을 `AiService`에 추가했다. 정상 command가 들어오면 `ai_generation_jobs`에 `QUEUED` 상태의 새 작업을 등록하고, 생성된 job id와 상태를 `CreateAiGenerationJobResult`로 반환한다.

이번 작업은 내부 Java UseCase 구현까지만 수행했다. `POST /api/v1/system/ai/generation-jobs` HTTP API, 관리자 재생성 API 연동, 실제 DeepSeek 호출, 산출물 생성, 검증 로그 등록, 감사 로그 연결은 workflow의 제외 범위에 따라 구현하지 않았다.

## 변경 요약

1. `AiService`가 `CreateAiGenerationJobUseCase`를 구현하도록 변경했다.
2. `CreateAiGenerationJobCommand`의 null command, blank 문자열, 양수가 아닌 `targetId`, null `requestedAt`을 `INVALID_INPUT`으로 차단했다.
3. `jobType` 문자열은 `AiGenerationJobType`, `targetType` 문자열은 `AiTargetType` enum 이름으로만 매핑했다.
4. `DAILY_QT_EXPLANATION` 같은 HTTP 입력값은 UseCase 레벨에서 허용하지 않고 `INVALID_INPUT`으로 차단했다.
5. 같은 `jobType + targetType + targetId + promptVersion` 기준의 `QUEUED` 또는 `RUNNING` 작업이 있으면 새 job 저장 없이 `INVALID_STATUS_TRANSITION`으로 실패하도록 했다.
6. 중복 작업이 없으면 `AiGenerationJob.queue(...)`로 새 job을 만들고 repository에 저장했다.
7. `requestedBy`는 command 계약 호환을 위해 필수 검증만 수행하고 Entity에는 저장하지 않았다.
8. `AiServiceTest`에 정상 생성, 중복 차단, 입력 검증, enum 매핑 실패 테스트를 추가했다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | `CreateAiGenerationJobUseCase` 구현, command 검증, enum 매핑, 진행 중 job 중복 차단, `QUEUED` job 저장 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` | UseCase bean 구현 가능성, 정상 저장, 중복 차단, null/blank/invalid enum/invalid target 검증 추가 |

## 기존 변경 재사용

| 파일 | 기준 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiGenerationJobRepository.java` | 기존 작업에서 추가된 `existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn(...)`를 재사용했다. |
| `qtai-server/src/main/java/com/qtai/common/exception/ErrorCode.java` | 기존 작업에서 추가된 `INVALID_STATUS_TRANSITION`을 중복 작업 차단 오류에 재사용했다. |
| `qtai-server/src/test/java/com/qtai/domain/ai/api/AiUseCaseContractTest.java` | 기존 계약 테스트가 `CreateAiGenerationJobUseCase`와 command/result record 규칙을 계속 검증한다. |

## 정책 반영

| 정책 | 반영 결과 |
| --- | --- |
| 내부 Java 계약 | HTTP Request/Response DTO 없이 `CreateAiGenerationJobUseCase`와 command/result record만 사용 |
| enum 매핑 | 현재 `AiGenerationJobType`, `AiTargetType` enum 이름만 허용 |
| HTTP 입력값 분리 | `DAILY_QT_EXPLANATION`, `promptVersionId` 매핑은 후속 HTTP API 단계로 분리 |
| 중복 차단 | 같은 job/target/prompt 기준의 `QUEUED`/`RUNNING` job 존재 시 저장하지 않음 |
| 저장 방식 | `AiGenerationJob.queue(...)`로 `QUEUED` job 생성 |
| 트랜잭션 | write UseCase 메서드에 `@Transactional` 적용 |
| requestedBy | 필수 검증만 수행하고 현재 Entity에는 저장하지 않음 |
| 민감 데이터 | prompt 원문, provider raw response, 검증 참조 원문, secret, token을 DTO나 로그에 추가하지 않음 |

## 테스트 보강

| 테스트 파일 | 검증 내용 |
| --- | --- |
| `AiServiceTest` | `AiService`가 `CreateAiGenerationJobUseCase` 구현체로 사용 가능한지 검증 |
| `AiServiceTest` | 정상 command가 `QUEUED` job을 저장하고 id/status를 반환하는지 검증 |
| `AiServiceTest` | 같은 job/target/prompt의 `QUEUED` 또는 `RUNNING` 작업이 있으면 `repository.save`를 호출하지 않는지 검증 |
| `AiServiceTest` | null command를 `INVALID_INPUT`으로 차단하는지 검증 |
| `AiServiceTest` | 잘못된 `jobType`, `targetType`을 `INVALID_INPUT`으로 차단하는지 검증 |
| `AiServiceTest` | blank `promptVersion`, blank `requestedBy`, null `requestedAt`, 양수가 아닌 `targetId`를 차단하는지 검증 |
| `AiUseCaseContractTest` | `CreateAiGenerationJobUseCase`와 command/result record 계약이 유지되는지 확인 |

## 수용 기준 점검

| 수용 기준 | 상태 | 근거 |
| --- | --- | --- |
| `CreateAiGenerationJobUseCase`가 Spring bean 구현체로 등록된다 | 충족 | `AiService`가 `@Service`와 `CreateAiGenerationJobUseCase` 구현을 가진다. |
| 정상 command로 `ai_generation_jobs`에 `QUEUED` 작업이 생성된다 | 충족 | `createAiGenerationJobCreatesQueuedJob` 테스트와 `AiGenerationJob.queue(...)` 저장 로직 |
| 결과 DTO가 생성된 job id와 `QUEUED` 상태를 반환한다 | 충족 | `CreateAiGenerationJobResult(savedJob.getId(), savedJob.getStatus().name())` 반환 |
| `QUEUED` 또는 `RUNNING` 중복 작업이 있으면 새 job을 저장하지 않는다 | 충족 | repository exists 조회 후 `INVALID_STATUS_TRANSITION`, `save` 미호출 테스트 |
| 잘못된 enum 문자열과 필수 입력 누락은 `INVALID_INPUT`으로 차단된다 | 충족 | enum 실패와 입력 검증 테스트 추가 |
| HTTP 입력값 매핑은 이번 UseCase 구현에 섞이지 않는다 | 충족 | `DAILY_QT_EXPLANATION`을 invalid enum으로 차단하는 테스트 추가 |
| `requestedBy`는 필수 검증만 수행하고 저장하지 않는다 | 충족 | command 검증만 수행하고 `AiGenerationJob.queue(...)` 인자로 전달하지 않음 |
| write use case 메서드에 `@Transactional`이 적용된다 | 충족 | `createAiGenerationJob`에 `@Transactional` 적용 |
| HTTP Controller, 관리자 권한, 감사 로그, DeepSeek 호출은 섞이지 않는다 | 충족 | 이번 변경은 `AiService` UseCase 구현과 단위 테스트에 한정 |
| AI 도메인의 금지 import가 추가되지 않는다 | 충족 | 금지 import 검사에서 매치 없음 |
| prompt 원문, provider raw response, 검증 참조 원문, secret, token은 DTO나 로그에 남지 않는다 | 충족 | DTO/로그 필드 추가 없음 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `gradle -p qtai-server test --tests "*AiServiceTest"` | 통과 |
| `gradle -p qtai-server test --tests "*AiUseCaseContractTest"` | 통과 |
| `gradle -p qtai-server test --tests "*AiGenerationJobTest"` | 통과 |
| `gradle -p qtai-server test` | 통과 |
| `gradle -p qtai-server build` | 통과 |
| `rg -n "^import .*domain\\.[a-z]+\\.(internal\|web)" qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |

저장소 루트와 `qtai-server`에 `gradlew`가 없고 시스템 `gradle` 명령도 설치되어 있지 않아, `%TEMP%/codex-gradle/gradle-8.10.2` 임시 Gradle 배포본으로 검증했다.

## 생략한 검증

| 명령 | 사유 |
| --- | --- |
| `./gradlew -p qtai-server test jacocoTestReport` | 저장소에 `gradlew`가 없고, Gradle task 목록에서 Jacoco 관련 task가 확인되지 않음 |
| `./gradlew -p qtai-server jacocoTestCoverageVerification` | 저장소에 `gradlew`가 없고, Gradle task 목록에서 Jacoco 관련 task가 확인되지 않음 |
| `npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml` | OpenAPI 파일을 수정하지 않았고, 저장소 루트에 `apis/`와 `.spectral.yaml`이 없음 |
| `gitleaks detect --source . --redact --exit-code 1` | 현재 환경에 `gitleaks` 실행 파일이 설치되어 있지 않음 |

## 남은 후속 작업

1. `POST /api/v1/system/ai/generation-jobs` HTTP API 구현
2. 관리자 재생성 API에서 `CreateAiGenerationJobUseCase` 호출 연결 여부 정리
3. `promptVersionId`와 `promptVersion` 문자열의 최종 매핑 정책 확정
4. `requestedBy` 저장 위치 또는 감사 로그 연결 정책 확정
5. `inputHash` 저장 위치와 중복 방지 인덱스 설계
6. 실제 DeepSeek 호출, 산출물 생성, 검증 로그 등록 파이프라인 연결
7. 감사 로그 UseCase 계약 확정 후 관리자 트리거 감사 기록 연결
