# Report - 2026-06-09 ai-service-db-migration-skeleton

## 작업 요약

`ai-service` 모듈에 AI 소유 DB schema 생성을 위한 Flyway migration skeleton을 추가했다. persistence가 비활성화된 기본 실행에서는 DB 연결과 migration이 강제되지 않고, `qtai.ai.persistence.enabled=true` 조건에서만 `ai-service` 전용 datasource에 migration을 적용한다.

이번 작업은 DDL skeleton과 검증 기반 추가가 목적이며, 운영 DB 연결과 실제 데이터 이관은 수행하지 않았다.

## 변경 내용

- `ai-service`에 Flyway core/mysql 의존성을 추가했다.
- `qtai.ai.persistence.flyway-enabled`, `qtai.ai.persistence.flyway-locations` 설정을 추가했다.
- Spring Boot 기본 Flyway auto-configuration은 제외하고, `AiServicePersistenceConfiguration`에서 ai-service datasource에만 migration을 적용하도록 격리했다.
- JPA entity manager 생성 전에 migration initializer가 먼저 실행되도록 구성했다.
- `V1__create_ai_owned_tables.sql`에 AI 소유 7개 테이블 DDL을 추가했다.
- H2 MySQL mode에서 Flyway migrate와 Hibernate validate를 검증하는 테스트를 추가했다.

## Migration 대상

- `ai_generation_jobs`
- `ai_generated_assets`
- `ai_validation_logs`
- `ai_prompt_versions`
- `ai_validation_checklist_versions`
- `validation_reference_jobs`
- `ai_batch_run_logs`

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 통과 |
| `.\gradlew.bat :ai-service:test` | 통과 |
| `git diff --check` | 통과 |
| placeholder 문구 검사 | 통과 |
| 금지 번역/민감 키워드 검사 | 통과 |

## 테스트 요약

- `AiServiceMigrationValidationTest`에서 Flyway migration 적용 후 7개 AI 소유 테이블과 `flyway_schema_history`가 생성됨을 검증했다.
- 같은 테스트에서 Hibernate `ddl-auto=validate` context load로 entity와 migration schema 정합성을 검증했다.
- 기존 persistence repository/context 테스트가 migration 설정 추가 후에도 통과함을 확인했다.
- `AiServiceMigrationDisabledContextTest`에서 `qtai.ai.persistence.flyway-enabled=false` 조건으로 context가 로드되고 `flyway_schema_history`가 생성되지 않음을 검증했다.

## REQUEST_CHANGES 대응

- `V1__create_ai_owned_tables.sql`은 PR diff에 포함되어 있으며, 이번 보강 커밋에서도 DDL 파일 헤더를 추가해 diff에 명시적으로 다시 포함했다.
- 재확인 명령 기준 `git diff --stat origin/dev...HEAD -- qtai-server/ai-service/src/main/resources/db/migration/V1__create_ai_owned_tables.sql` 결과는 `155 insertions`이며, `git diff origin/dev...HEAD -- ...`에서 SQL 본문 전체가 표시된다.
- migration 실패 시 민감 설정값 없이 `locations`와 예외 타입을 error log context로 남기도록 보강했다.
- `flyway-enabled=false` 경로 context load 테스트를 추가했다.
- `validation_reference_jobs.storage_uri`, `index_storage_uri`는 후속 admin/API mapping PR에서 직접 노출 차단 테스트 또는 ArchUnit 정책 테스트를 수용 기준으로 둔다.
- Flyway migration 실패 catch 범위를 `FlywayException`으로 좁히고, initializer bean 반환값을 전용 marker 타입으로 변경했다.

## 제외 확인

- monolith migration은 수정하지 않았다.
- 운영 DB에 migration을 실행하지 않았다.
- 기존 데이터 백필, 복제, 이관은 수행하지 않았다.
- prompt version seed 운영 정책은 변경하지 않았다.
- business usecase, scheduler, worker, gateway, provider 연결은 변경하지 않았다.

## 후속 작업

- 운영 MySQL 연결 smoke test
- prompt version seed 이관 또는 bootstrap 정책 확정
- monolith DB에서 ai-service DB로 데이터 이관 절차 작성
- ai-service usecase persistence skeleton 추가
- 기존 monolith AI persistence 제거 시점 결정
