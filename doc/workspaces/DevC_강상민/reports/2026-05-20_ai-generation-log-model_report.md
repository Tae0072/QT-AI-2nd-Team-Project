# Report — 2026-05-20 ai-generation-log-model

| 항목          | 내용                                                                                                                         |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| 담당자        | 강상민                                                                                                                       |
| 브랜치        | `feature/ai-generation-log-model`                                                                                            |
| PR 링크       | 미생성                                                                                                                       |
| 관련 workflow | [2026-05-20_ai-generation-log-model.md](../workflows/2026-05-20_ai-generation-log-model.md)                                  |
| 관련 F-ID     | F-02, F-14                                                                                                                   |
| 기준 문서     | `07_요구사항_정의서.md` v3.1, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `23_도메인_용어사전.md` |

## 작업 결과 요약

AI 생성·검증 흐름의 공통 기반인 로그 모델 초안을 `domain.ai.internal`에 구현했다. 해설, 시뮬레이터, F-15 Q&A 산출물이 공통으로 사용할 수 있도록 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs`에 대응하는 Entity, 상태 enum, Repository, 내부 Service를 추가했다.

이 작업은 실제 DeepSeek 호출, 관리자 API, 사용자 Q&A API를 열지 않고 로그 모델과 상태 전이 기준만 먼저 고정하는 범위로 진행했다.

## 산출물

| 구분       | 파일                              | 내용                                                                                                   |
| ---------- | --------------------------------- | ------------------------------------------------------------------------------------------------------ |
| Entity     | `AiGenerationJob.java`            | AI 생성 작업 단위. job type, target, prompt version, status, error message, started/finished time 추적 |
| Entity     | `AiGeneratedAsset.java`           | AI 산출물 단위. asset type, target, payload json, source label, status 추적                            |
| Entity     | `AiValidationLog.java`            | AI 검증 결과. layer, result, reviewer type, checklist version, checklist json, error message 추적      |
| Repository | `AiGenerationJobRepository.java`  | 생성 작업 JPA Repository                                                                               |
| Repository | `AiGeneratedAssetRepository.java` | 산출물 JPA Repository                                                                                  |
| Repository | `AiValidationLogRepository.java`  | 검증 로그 JPA Repository                                                                               |
| Service    | `AiLogService.java`               | 생성 작업 queue/running/succeeded/failed, 산출물 등록, 검증 로그 등록                                  |
| Test       | `AiLogServiceTest.java`           | 상태 기록과 검증 실패 처리 단위 테스트                                                                 |

## 상태값 기준

| 대상                               | 상태값                                         |
| ---------------------------------- | ---------------------------------------------- |
| `ai_generation_jobs.status`        | `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`     |
| `ai_generated_assets.status`       | `VALIDATING`, `APPROVED`, `REJECTED`, `HIDDEN` |
| `ai_validation_logs.result`        | `PASSED`, `REJECTED`, `NEEDS_REVIEW`           |
| `ai_validation_logs.reviewer_type` | `AUTO`, `ADMIN`, `ADVISOR`                     |

## 주요 구현 결정

1. Entity는 모두 `domain.ai.internal`에 배치했다.
2. Repository도 같은 도메인의 `internal` 안에 두어 외부 도메인 접근을 막았다.
3. write 성격의 `AiLogService` public 메서드에는 `@Transactional`을 부여했다.
4. 실패 사유는 `errorMessage`로 남기되 길이를 제한했다.
5. 검증 실패(`REJECTED`) 로그 등록 시 연결 산출물은 `APPROVED`가 아니라 `REJECTED`로 전환되도록 했다.
6. 실제 LLM 호출, 사용자 응답, 관리자 검토 API는 후속 workflow 범위로 남겼다.

## 테스트

| 테스트                                                           | 검증 내용                                        |
| ---------------------------------------------------------------- | ------------------------------------------------ |
| `생성작업은_QUEUED_상태와_대상정보를_기록한다`                   | 생성 작업 초기 상태와 target/prompt version 기록 |
| `생성실패는_FAILED_상태와_재처리_대상정보를_유지한다`            | 실패 상태, 실패 사유, 재처리 대상 정보 유지      |
| `산출물등록은_VALIDATING_상태와_출처표기를_기록한다`             | 산출물 초기 상태와 source label 기록             |
| `검증실패는_검증로그를_남기고_산출물을_APPROVED로_만들지_않는다` | 검증 실패 로그와 산출물 `REJECTED` 전환          |

## 검증 결과

| 명령                                                | 결과                                    |
| --------------------------------------------------- | --------------------------------------- |
| `gradle clean test --tests "*Ai*"`                  | 성공                                    |
| `gradle build`                                      | 성공                                    |
| AI 도메인 금지 문자열 스캔                          | 매치 없음                               |
| `gitleaks detect --source . --redact --exit-code 1` | 로컬에 `gitleaks` 명령이 없어 실행 불가 |

실제 실행은 저장소에 Gradle wrapper가 없어 사용자 Gradle 캐시의 `gradle-8.14` 실행 파일로 수행했다.

## 변경 범위

```text
qtai-server/src/main/java/com/qtai/domain/ai/internal/
├─ AiGeneratedAsset.java
├─ AiGeneratedAssetRepository.java
├─ AiGeneratedAssetStatus.java
├─ AiGeneratedAssetType.java
├─ AiGenerationJob.java
├─ AiGenerationJobRepository.java
├─ AiGenerationJobStatus.java
├─ AiGenerationJobType.java
├─ AiLogService.java
├─ AiTargetType.java
├─ AiValidationLog.java
├─ AiValidationLogRepository.java
├─ AiValidationResult.java
└─ AiValidationReviewerType.java

qtai-server/src/test/java/com/qtai/domain/ai/internal/
└─ AiLogServiceTest.java
```

## 미완료 / 후속 작업

1. PR 생성 전 `gitleaks`는 CI 또는 설치된 환경에서 재실행해야 한다.
2. 실제 DB migration 또는 schema.sql이 필요하면 후속 인프라/DB 작업에서 Entity 기준으로 반영해야 한다.
3. `ai-usecase-contracts` workflow에서 공개 UseCase와 command/result DTO를 정리해야 한다.
4. `ai-pre-generation-validation` workflow에서 batch/admin 생성 경로와 자동 검증 흐름을 연결해야 한다.
5. `ai-admin-review-regeneration` workflow에서 승인 시 `verse_explanations` 또는 `simulator_clips` 연결 정책을 구현해야 한다.

## PR 전 체크

- [x] `domain.ai` 밖의 `internal` 타입을 직접 import하지 않았다.
- [x] Controller에서 Repository를 직접 호출하지 않는다.
- [x] `jakarta.*`만 사용했다.
- [x] F-02, F-14 범위로 작업했다.
- [x] `gitleaks`는 로컬 명령 부재로 미실행 상태다. 변경된 코드에 secret Key 없음 확인인
