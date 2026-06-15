# Report - 2026-06-15 기본 EXPLANATION 프롬프트 활성 보장

## 요약

관리자 AI 프롬프트 관리 화면에서 활성 EXPLANATION 프롬프트 행은 존재하지만 `system_prompt`, `user_prompt_template` 본문이 비어 있는 상태를 확인했다. 생성 흐름은 DB의 활성 프롬프트를 읽어 LLM 요청을 만들기 때문에, 기존 코드 기본 프롬프트를 DB seed/backfill로 보장하도록 migration을 추가했다.

이번 변경은 DB 보정에 한정했다. 관리자 UI, API DTO, OpenAPI, 생성/검증 로직은 변경하지 않았다.

## 작업 정보

| 항목 | 내용 |
| --- | --- |
| 작업일 | 2026-06-15 |
| 브랜치 | `bugfix/ai-prompt-active-seed` |
| PR 대상 | `dev` |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-15_ai-prompt-active-seed.md` |
| 관련 F-ID | 해당 없음 - AI 프롬프트 운영 seed/backfill 보정 |

## 변경 내용

### Migration

- `qtai-server/admin-server/src/main/resources/db/migration/V45__ensure_active_explanation_prompt.sql`
  - EXPLANATION 프롬프트 행의 `system_prompt` 또는 `user_prompt_template`이 `NULL`/blank이면 기존 코드 기본 프롬프트로 채우도록 보정했다.
  - `temperature`, `max_tokens`, `description`, `activated_at`이 비어 있는 기존 행을 함께 보정했다.
  - ACTIVE EXPLANATION 행이 하나도 없을 때만 baseline ACTIVE 프롬프트를 삽입하도록 했다.
  - 기존 내용 있는 ACTIVE 프롬프트를 덮어쓰거나 retire하지 않도록 했다.

### Local DB

- 로컬 docker 구성은 admin-server Flyway가 꺼져 있어, 작성한 migration SQL을 현재 로컬 MySQL에 1회 직접 적용했다.
- 적용 전: EXPLANATION ACTIVE 행 2건의 `system_prompt`, `user_prompt_template`이 비어 있었다.
- 적용 후: EXPLANATION ACTIVE 행 2건 모두 `system_prompt`, `user_prompt_template`, `temperature=0.2`, `max_tokens=2000`, `activated_at`이 채워졌다.

## 검증 결과

| 구분 | 명령 또는 확인 | 결과 |
| --- | --- | --- |
| 로컬 DB 적용 | `V45__ensure_active_explanation_prompt.sql`을 로컬 MySQL에 적용 | 성공 |
| 로컬 DB 조회 | ACTIVE EXPLANATION 2건, system/user 본문 채움 여부 조회 | `active_explanation_count=2`, `rows_with_system=2`, `rows_with_user=2` |
| bootJar | `.\gradlew :admin-server:bootJar :service-ai:bootJar` | 통과 |
| service-ai 회귀 테스트 | `.\gradlew :service-ai:test --tests com.qtai.domain.ai.internal.ExplanationGenerationJobHandlerTest` | 통과 |
| admin-server 회귀 테스트 | `.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.AiPromptManagementServiceTest` | 통과 |
| Docker 재기동 | `docker compose up -d --build service-admin service-ai` | 성공 |
| Docker health | `docker compose ps service-admin service-ai` | 두 컨테이너 healthy |
| 브라우저 확인 | `http://localhost:5173/ai-prompt-versions` ACTIVE 필터 | EXPLANATION 활성 프롬프트 2건 표시, 콘솔 에러 없음 |
| 상세 확인 | 최신 ACTIVE 행 상세 | 기본 system/user prompt 본문과 `{{versesBlock}}` placeholder 표시 |
| diff 공백 검사 | `git diff --check` | 통과 |

## 수용 기준 확인

- 신규 migration `V45__ensure_active_explanation_prompt.sql` 추가 완료.
- 빈 EXPLANATION prompt 본문 backfill 확인 완료.
- ACTIVE 행이 없을 때만 baseline ACTIVE 행을 삽입하는 SQL 작성 완료.
- 기존 내용 있는 ACTIVE 프롬프트를 덮어쓰거나 retire하지 않도록 처리 완료.
- 로컬 DB에서 ACTIVE EXPLANATION 프롬프트 본문 non-null/non-blank 확인 완료.
- 관리자 화면에서 ACTIVE EXPLANATION 프롬프트 표시 확인 완료.
- 지정된 Gradle 테스트, bootJar, Docker 재기동, `git diff --check` 확인 완료.

## 미실행 또는 제외 검증

- `.\gradlew -p qtai-server build`
  - 이번 변경은 단일 migration과 문서 보정이며, 지정 계획의 `:admin-server:bootJar`, `:service-ai:bootJar`, 관련 단위 테스트로 범위를 좁혀 검증했다.
- Spectral / OpenAPI lint
  - OpenAPI 계약 변경이 없어 실행하지 않았다.
- gitleaks
  - secret 예시를 추가하지 않았고, 변경 범위가 migration과 문서에 한정되어 이번 계획 범위에서는 실행하지 않았다.

## 후속 확인 사항

- 현재 docker-compose는 admin-server Flyway를 비활성화하고 JPA update를 사용한다. seed/backfill migration을 로컬에서 자동 반영하려면 이 운영 정책을 별도 검토해야 한다.
- 운영 DB 적용 시 기존 ACTIVE 프롬프트 본문을 덮어쓰지 않는 migration이라는 점을 PR 설명 또는 release note에 명시한다.
