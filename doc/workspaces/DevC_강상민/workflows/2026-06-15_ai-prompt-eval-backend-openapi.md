# Workflow - 2026-06-15 ai-prompt-eval-backend-openapi

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-prompt-eval-backend-openapi` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-06, F-14 |
| 트리거 | EXPLANATION 프롬프트 운영 설정을 DB화하고 AD-11 평가 실행 결과로 ACTIVE 전환을 통제하기 위함 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `CODE_CONVENTION.md` |
| 해당 경로 | `qtai-server/admin-server/**`, `qtai-server/service-ai/**`, `qtai-server/apis/api-v1/openapi.yaml`, backend 리포트 문서 |

## 작업 목표

EXPLANATION 생성 프롬프트가 코드 하드코딩이 아니라 `ai_prompt_versions`의 운영 설정을 통해 실행되도록 변경한다. 새 프롬프트는 DRAFT로 생성하고, AD-11 평가 세트 실행 결과가 통과해야 ACTIVE로 전환할 수 있게 한다.

이 PR은 서버와 OpenAPI 계약만 다룬다. 관리자 웹 화면 연결은 후속 PR에서 처리한다.

## 범위

- `ai_prompt_versions`에 프롬프트 본문과 LLM 실행 설정 컬럼 추가.
- 기존 EXPLANATION seed 프롬프트를 DB 컬럼으로 backfill.
- `ai_evaluation_runs`, `ai_evaluation_results` 테이블 추가.
- EXPLANATION 생성 핸들러가 DB 프롬프트 템플릿을 사용하도록 변경.
- 프롬프트 관리 API 추가.
- 평가 run 생성/조회 API 추가.
- prompt ACTIVE 전환 게이트 구현.
- OpenAPI 계약 반영.
- backend 전용 workflow/report 작성.

## 제외 범위

- admin-web 화면, 메뉴, API client 변경.
- QA, SIMULATOR 프롬프트 관리.
- 기대 정책 JSON 기반 자동 채점 고도화.
- 실제 배포/운영 DB 적용.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/admin-server/src/main/resources/db/migration/V44__extend_ai_prompt_versions_and_evaluation_runs.sql` | 프롬프트 운영 컬럼과 평가 run/result 테이블 추가 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiPromptVersion.java` | 프롬프트 본문/모델 설정/상태 전환 보유 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java` | DB 프롬프트 렌더링 기반 LLM 호출 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/**` | service-ai 중복 생성 로직 동기화 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/api/admin/prompt/**` | 프롬프트 관리 UseCase/DTO |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiPromptManagementService.java` | 프롬프트 생성/조회/활성화/폐기 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiEvaluationRunService.java` | 평가 실행 및 결과 저장 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/web/AdminAiEvaluationController.java` | 평가 run API 추가 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/web/AdminAiPromptVersionController.java` | 프롬프트 관리 API |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | API 계약 반영 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/**` | 프롬프트 게이트, 평가 run, 생성 핸들러 검증 |
| Test | `qtai-server/service-ai/src/test/java/com/qtai/domain/ai/internal/**` | service-ai 생성 핸들러 동기화 검증 |

## 구현 순서

1. DB migration으로 프롬프트 컬럼과 평가 run/result 테이블을 추가한다.
2. `AiPromptVersion`과 repository를 확장한다.
3. 생성 핸들러를 DB 프롬프트 템플릿 기반으로 변경한다.
4. 프롬프트 관리 API와 service를 구현한다.
5. 평가 run/result entity, repository, service를 추가한다.
6. ACTIVE 전환 게이트를 평가 run 결과와 연결한다.
7. OpenAPI 계약을 갱신한다.
8. 단위 테스트와 서버 build를 실행한다.

## 테스트 보강 목록

| 테스트 파일 | 검증 |
| --- | --- |
| `AiPromptManagementServiceTest.java` | 평가 run 없이 ACTIVE 전환 차단, 통과 run 이후 기존 ACTIVE retire |
| `AiEvaluationRunServiceTest.java` | 평가 실행 결과가 원문 대신 summary/hash만 저장되는지 확인 |
| `ExplanationGenerationJobHandlerTest.java` | DB 프롬프트 system/user/model/temperature/maxTokens 사용 확인 |

## 수용 기준

- [ ] 기존 배치/재생성은 ACTIVE EXPLANATION 프롬프트를 사용한다.
- [ ] DRAFT 프롬프트는 평가 run에는 사용할 수 있으나 생성 job에는 사용할 수 없다.
- [ ] 성공 평가 run이 없으면 ACTIVE 전환이 실패한다.
- [ ] 평가 결과에는 raw output/prompt/secret이 저장되지 않는다.
- [ ] OpenAPI가 신규 API 계약을 포함한다.

## Subagent Decision

### 권장 여부

Subagent use is authorized for this workflow when the agent determines that parallel work is beneficial.

### 판단 근거

- DB/API/service/test/OpenAPI가 서로 연결되지만 파일 경로는 분리 가능하다.
- 단, 최종 계약 정합성과 서버 build는 메인 에이전트가 직접 확인해야 한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| Worker 1 | backend service/API 구현 보조 | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/**` |
| Worker 2 | 생성 핸들러 및 service-ai 동기화 확인 | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/**` |
| Worker 3 | OpenAPI 계약 검토 | `qtai-server/apis/api-v1/openapi.yaml` |

### 직접 실행 판단

메인 에이전트가 migration, API 계약, 테스트 결과를 통합 검증한다.

## 검증 계획

- `./gradlew.bat build jacocoTestCoverageVerification`
- OpenAPI YAML parse
- `git diff --check`
- 가능하면 Spectral과 gitleaks 실행. 도구가 없으면 사유 기록.

## 후속 작업으로 남길 항목

- admin-web 화면 연결은 별도 PR에서 진행.
- 기대 정책 JSON 기반 자동 판정은 후속 고도화.
