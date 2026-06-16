# Workflow - 2026-06-15 ai-prompt-active-seed

| 항목 | 내용 |
| --- | --- |
| 담당자 | DevC 강상민 |
| 작업 브랜치 | `bugfix/ai-prompt-active-seed` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 - AI 프롬프트 운영 seed/backfill 보정 |
| 트리거 | 관리자 AI 프롬프트 관리 화면에서 활성 EXPLANATION 프롬프트 본문이 비어 있고, 생성 흐름이 활성 프롬프트 데이터에 의존하는 상태 확인 |
| 기준 문서 | `doc/프로젝트 문서/04_API_명세서.md`, `doc/프로젝트 문서/09_Git_규칙.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md` |
| 해당 경로 | `qtai-server/admin-server/src/main/resources/db/migration/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

기존 코드 기본값으로 쓰이던 EXPLANATION 생성 프롬프트를 DB의 활성 프롬프트 데이터로 보장한다. 이미 활성 프롬프트가 있으면 관리자 선택을 덮어쓰지 않고, 본문이 비어 있는 기존 EXPLANATION 행만 보정한다.

로컬 docker 구성은 admin-server Flyway가 꺼져 있으므로 migration 파일 작성 후 현재 로컬 MySQL에도 동일 보정 SQL을 1회 적용해 관리자 화면에서 ACTIVE EXPLANATION 프롬프트가 표시되는지 확인한다.

## 범위

- `ai_prompt_versions`에 대한 신규 Flyway migration을 추가한다.
- `EXPLANATION` 행의 `system_prompt` 또는 `user_prompt_template`이 `NULL`이거나 blank이면 기존 코드 기본 프롬프트로 채운다.
- `EXPLANATION`의 `ACTIVE` 행이 하나도 없을 때만 baseline ACTIVE 프롬프트를 삽입한다.
- 로컬 MySQL에 동일 SQL을 적용해 현재 개발 화면을 검증한다.
- workflow와 report 문서를 작성한다.

## 제외 범위

- 관리자 UI, API DTO, OpenAPI 계약은 변경하지 않는다.
- 검사용 프롬프트(`AiReviewValidationService`)는 변경하지 않는다.
- 기존 내용 있는 ACTIVE 프롬프트를 retire하거나 덮어쓰지 않는다.
- AI 생성 로직, 검증 로직, provider 호출 로직은 변경하지 않는다.
- 금지 번역본 seed/test/fixture/response 데이터와 prompt/provider raw response, secret/token/password 예시는 추가하지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/admin-server/src/main/resources/db/migration/V45__ensure_active_explanation_prompt.sql` | EXPLANATION 프롬프트 본문 backfill 및 ACTIVE baseline 보장 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-15_ai-prompt-active-seed.md` | 작업 범위, 검증 계획, 제외 범위 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-15_ai-prompt-active-seed_report.md` | 작업 결과, 검증 결과, 로컬 DB 보정 결과 기록 |

## 구현 순서

1. `origin/dev` 기준 `bugfix/ai-prompt-active-seed` 브랜치에서 작업 중인지 확인한다.
2. 로컬 DB의 `ai_prompt_versions`에서 EXPLANATION ACTIVE 행과 prompt 본문 존재 여부를 확인한다.
3. `V44__extend_ai_prompt_versions_and_evaluation_runs.sql`와 `AiPromptVersion`의 기본 프롬프트 값을 기준으로 `V45__ensure_active_explanation_prompt.sql`을 작성한다.
4. migration은 먼저 blank prompt 본문을 backfill하고, 그 다음 ACTIVE EXPLANATION 행이 없을 때만 baseline 행을 삽입한다.
5. 로컬 MySQL에 신규 migration SQL과 동일한 보정 SQL을 1회 적용한다.
6. 로컬 DB에서 EXPLANATION ACTIVE 행의 `system_prompt`, `user_prompt_template`, `activated_at`이 채워졌는지 확인한다.
7. 지정된 Gradle 테스트와 bootJar를 실행한다.
8. `service-admin`, `service-ai` 컨테이너를 재빌드/재기동한다.
9. 브라우저에서 `http://localhost:5173/ai-prompt-versions`를 확인한다.
10. report 문서에 변경 내용과 검증 결과를 기록한다.
11. 변경 파일만 stage하고 Conventional Commits 형식으로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 검증 |
| --- | --- |
| 해당 없음 | 이번 변경은 SQL seed/backfill 보정이며 Java API/서비스 동작 계약을 바꾸지 않는다. 기존 생성 핸들러와 프롬프트 관리 서비스 테스트를 회귀 검증으로 실행한다. |

## 수용 기준

- [ ] 신규 migration이 `V45__ensure_active_explanation_prompt.sql`로 추가된다.
- [ ] `EXPLANATION` prompt 행의 빈 `system_prompt`, `user_prompt_template`이 기존 기본 프롬프트로 채워진다.
- [ ] ACTIVE EXPLANATION 행이 없을 때만 baseline ACTIVE 행을 삽입한다.
- [ ] 기존 내용 있는 ACTIVE 프롬프트를 덮어쓰거나 retire하지 않는다.
- [ ] 로컬 DB에서 ACTIVE EXPLANATION 프롬프트 본문이 non-null/non-blank임을 확인한다.
- [ ] `http://localhost:5173/ai-prompt-versions`에서 ACTIVE EXPLANATION 프롬프트가 표시된다.
- [ ] 지정된 Gradle 테스트와 bootJar, `git diff --check`가 통과한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 migration과 작업 문서에 집중되어 있다.
- SQL 보정, 로컬 DB 적용, 화면 확인이 순차적으로 연결되어 있어 한 에이전트가 상태를 추적하는 편이 안전하다.
- 병렬 작업보다 migration 적용 전후 DB 상태를 일관되게 확인하는 것이 중요하다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, migration 구현, 로컬 DB 보정, 검증, report 작성, 커밋을 직접 수행한다.

## 검증 계획

```powershell
.\gradlew :admin-server:bootJar :service-ai:bootJar
.\gradlew :service-ai:test --tests com.qtai.domain.ai.internal.ExplanationGenerationJobHandlerTest
.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.AiPromptManagementServiceTest
docker compose up -d --build service-admin service-ai
git diff --check
```

브라우저 검증은 `http://localhost:5173/ai-prompt-versions`에서 상태 필터를 ACTIVE로 두고 EXPLANATION 프롬프트 행과 상세 본문 표시를 확인한다.

## 후속 작업으로 남길 항목

- docker-compose에서 admin-server Flyway 비활성 정책과 JPA update 정책이 장기 운영에 적합한지 별도 검토한다.
- seed/backfill migration을 실제 운영 DB에 적용할 때는 기존 ACTIVE 프롬프트 본문 보존 여부를 release note에 명시한다.
