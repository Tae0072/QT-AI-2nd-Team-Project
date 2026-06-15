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
- 기존 서버 DTO와 어긋나 있던 수동 평가 케이스 생성 요청 스키마를 식별자 기반 계약으로 정정.

## 보안/데이터 원칙

- 평가 결과에는 raw provider response, prompt 원문, secret/token/password/private key를 저장하지 않는다.
- 평가 결과 저장값은 target metadata, prompt metadata, model name, payload hash, 결과 count 중심으로 제한한다.
- 사용자 노출 산출물(`ai_generated_assets`)과 평가 실행 결과 저장소를 분리한다.

## 평가 게이트 의미

- 현재 `PASSED`는 EXPLANATION 생성 호출이 완료되고 응답이 필수 JSON 구조 검증을 통과했다는 의미다.
- `expectedPolicyJson` 기반의 의미/정책 자동 채점은 이번 PR 범위가 아니며 후속 고도화 대상이다.
- 따라서 ACTIVE 전환 게이트는 "생성 가능성과 응답 구조 안정성"을 보증하며, 콘텐츠 품질의 의미론적 판정까지 보증하지 않는다.

## 마이그레이션 포함 여부

이 PR에는 Flyway migration `qtai-server/admin-server/src/main/resources/db/migration/V44__extend_ai_prompt_versions_and_evaluation_runs.sql`이 포함되어 있다.

V44 주요 내용:

- `ai_prompt_versions`에 운영 프롬프트 설정 컬럼 추가: `system_prompt`, `user_prompt_template`, `model_name`, `temperature`, `max_tokens`, `description`, `created_by_admin_id`, `activated_at`, `retired_at`.
- 기존 `EXPLANATION` 프롬프트 기본값 backfill: 기본 system/user prompt, `temperature = 0.2`, `max_tokens = 2000`, `content_hash = c08e6b04543f57a2d60fa538485ab3fc54c2f2752e7e024ec124848fd0e1ef65`, `activated_at = created_at`.
- `ai_prompt_versions(prompt_type, status, created_at, id)` 인덱스 추가.
- `ai_evaluation_runs` 테이블 추가: 평가 세트, 프롬프트 버전, 상태, total/passed/failed/needsReview count, 요청 관리자, 시작/종료 시각 저장.
- `ai_evaluation_results` 테이블 추가: run/case별 결과, 안전한 실패 reason, 원문 없는 요약 JSON 저장.
- run/result 조회용 인덱스 추가.

## OpenAPI 정합화 메모

- `AdminAiEvaluationCaseRequest`의 식별자 기반 스키마 변경은 이번 PR의 신규 서버 동작이 아니라, 이미 `dev`에 반영된 `AdminAiEvaluationController.AiEvaluationCaseRequest`와 OpenAPI를 맞추는 정정이다.
- 클라이언트는 `targetType`, `targetId`, `sourceType`, 선택 `expectedPolicyJson`을 보내고, 서버가 저장용 `inputJson` 메타데이터를 조립한다.

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
