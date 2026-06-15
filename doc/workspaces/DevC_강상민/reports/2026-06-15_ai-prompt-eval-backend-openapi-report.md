# AI 프롬프트 평가 게이트 Backend/OpenAPI 리포트

| 항목 | 내용 |
| --- | --- |
| 작성일 | 2026-06-15 |
| 브랜치 | `feature/ai-prompt-eval-backend-openapi` |
| PR 대상 | `dev` |
| 범위 | backend, migration, OpenAPI, backend tests |

## 요약

EXPLANATION 프롬프트를 코드 하드코딩에서 DB 운영 설정으로 이전했다. 관리자는 프롬프트를 DRAFT로 생성할 수 있고, AD-11 평가 run이 성공한 프롬프트만 ACTIVE로 전환할 수 있다.

이 리포트는 backend와 OpenAPI PR 전용이다. admin-web 화면 변경과 화면 문서는 별도 PR에서 다룬다.

## 구현 내용

- `ai_prompt_versions`에 실제 프롬프트 본문과 LLM 실행 설정 컬럼 추가.
- 기존 EXPLANATION 기본 프롬프트를 migration에서 backfill.
- `ai_evaluation_runs`, `ai_evaluation_results` 테이블 추가.
- `ExplanationGenerationJobHandler`가 DB 프롬프트를 렌더링해 LLM 요청을 만들도록 변경.
- `admin-server`와 `service-ai`의 중복 EXPLANATION 생성 로직 동기화.
- 프롬프트 관리 API 추가.
- 평가 run 생성/조회 API 추가.
- prompt ACTIVE 전환 시 평가 run 통과 여부를 검증.
- OpenAPI 계약 갱신.

## 보안/데이터 원칙

- 평가 결과에는 raw provider response, prompt 원문, secret/token/password/private key를 저장하지 않는다.
- 평가 결과 저장값은 target metadata, prompt metadata, model name, payload hash, 결과 count 중심으로 제한한다.
- 사용자 노출 산출물(`ai_generated_assets`)과 평가 실행 결과 저장소를 분리한다.

## 주요 파일

- `qtai-server/admin-server/src/main/resources/db/migration/V44__extend_ai_prompt_versions_and_evaluation_runs.sql`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiPromptVersion.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiPromptManagementService.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiEvaluationRunService.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/web/AdminAiPromptVersionController.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/web/AdminAiEvaluationController.java`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java`
- `qtai-server/apis/api-v1/openapi.yaml`

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `./gradlew.bat build jacocoTestCoverageVerification` | 통과 |
| OpenAPI YAML parse | 통과 |
| `git diff --check` | 통과. Windows 줄끝 경고만 존재 |

실행하지 못한 검증:

- `.spectral.yaml`이 없어 Spectral ruleset 검증 불가.
- `gitleaks` CLI가 설치되어 있지 않아 secret scan 불가.

## 리뷰 포인트

- `V44` migration의 MySQL 적용 적합성.
- prompt ACTIVE 전환 조건이 운영 정책과 맞는지.
- 평가 결과 summary에 저장되는 필드가 충분하면서도 원문 노출을 피하는지.
- admin-web PR은 이 API 계약에 의존한다.
