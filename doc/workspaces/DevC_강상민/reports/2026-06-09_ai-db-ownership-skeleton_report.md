# Report - 2026-06-09 ai-db-ownership-skeleton

## 작업 요약

`ai-service` 모듈에 AI DB 소유권 분리 skeleton을 추가했다. 기본 실행에서는 persistence가 비활성화되고, `qtai.ai.persistence.enabled=true`와 DB 설정이 들어온 경우에만 AI 소유 JPA entity/repository가 등록된다.

운영 Flyway migration, 실제 데이터 이관, monolith AI persistence 삭제는 수행하지 않았다.

## 변경 내용

- `ai-service`에 JPA, MySQL runtime, H2 test 의존성을 추가했다.
- `application.yml`에 `qtai.ai.persistence.*` placeholder를 추가했다.
- 기본 auto DataSource/JPA configuration은 제외하고, `AiServicePersistenceConfiguration`에서 opt-in으로만 datasource, entity manager, transaction manager, repository scan을 구성했다.
- monolith AI internal에서 AI 소유 DB skeleton에 필요한 entity, repository, enum, guard만 복제했다.
- 기본 mode, opt-in mode, fail-fast, repository 동작 테스트를 추가했다.

## AI 소유 DB

이번 skeleton에 포함한 AI 소유 테이블은 다음 7개다.

- `ai_generation_jobs`
- `ai_generated_assets`
- `ai_validation_logs`
- `ai_prompt_versions`
- `ai_validation_checklist_versions`
- `validation_reference_jobs`
- `ai_batch_run_logs`

## 비소유 DB

다음 테이블은 `ai-service`가 직접 소유하거나 조회하지 않는 것으로 report에 고정한다.

- `verse_explanations`
- `qt_passages`
- `bible_verses`
- `audit_logs`
- `admin_users`
- `members`

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 통과 |
| `.\gradlew.bat :ai-service:test` | 통과 |
| `git diff --check` | 통과 |
| placeholder 문구 검사 | 통과 |
| 금지 번역/민감 키워드 검사 | 통과 |
| 다른 도메인 internal/repository/entity import 검사 | 통과 |

## 테스트 요약

- 기본 `qtai.ai.persistence.enabled=false`에서 AI persistence repository bean이 등록되지 않음을 검증했다.
- opt-in H2 context에서 7개 repository bean이 등록됨을 검증했다.
- JPA metamodel에 7개 AI 소유 entity가 포함됨을 검증했다.
- 대표 save/query 흐름으로 generation job, generated asset, validation log, prompt version, checklist version, validation reference job, batch run log repository를 검증했다.
- `ai_batch_run_logs.createdAt` auditing 값이 저장 시 채워짐을 검증했다.
- `enabled=true`인데 DB URL이 누락되면 fail-fast 되는지 검증했다.

## 제외 확인

- 운영 Flyway migration은 추가하지 않았다.
- prompt seed 운영 이관은 수행하지 않았다.
- 실제 MySQL 연결과 데이터 마이그레이션은 수행하지 않았다.
- 기존 monolith AI persistence 코드는 삭제하지 않았다.
- provider endpoint, gateway, JWKS, service-token 작업은 수행하지 않았다.

## 후속 작업

- ai-service 운영 Flyway migration 작성
- prompt version seed 이관 또는 bootstrap 정책 확정
- monolith와 ai-service DB 데이터 이관 절차 작성
- 실제 MySQL 연결 smoke test
- ai-service live 전환 후 monolith AI persistence 제거 시점 결정
