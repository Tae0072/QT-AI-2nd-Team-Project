# Workflow - 2026-06-09 ai-service-db-migration-skeleton

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-service-db-migration-skeleton` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `feature/ai-service-db-migration-skeleton` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | `ai-service`가 AI DB entity/repository skeleton을 보유했으므로 독립 DB schema 생성 기반이 필요함 |
| 기준 문서 | `2026-06-09_ai-db-ownership-skeleton.md`, `2026-06-09_ai-db-ownership-skeleton_report.md` |
| 대상 경로 | `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`ai-service` 모듈이 자기 DB schema를 만들 수 있도록 Flyway migration skeleton을 추가한다. 이번 단계는 AI 소유 7개 테이블의 DDL과 migration 검증 기반을 만드는 것이며, 실제 운영 DB 연결이나 monolith 데이터 이관은 수행하지 않는다.

기본 실행에서는 persistence가 비활성화되어 DB 연결과 migration이 강제되지 않는다. `qtai.ai.persistence.enabled=true`일 때만 `ai-service` 전용 datasource에 migration을 적용하고, JPA validate로 entity와 DDL 정합성을 검증한다.

## 범위

- `ai-service`에 Flyway 의존성과 migration opt-in 설정을 추가한다.
- `ai-service/src/main/resources/db/migration`에 AI 소유 7개 테이블 DDL을 추가한다.
- migration 대상 테이블은 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs`, `ai_prompt_versions`, `ai_validation_checklist_versions`, `validation_reference_jobs`, `ai_batch_run_logs`다.
- `AiServicePersistenceConfiguration`에서 persistence 활성화 시 Flyway migration을 JPA entity manager 생성 전에 수행한다.
- H2 MySQL mode 기반 테스트로 migration 적용과 JPA validate를 검증한다.
- report에 monolith migration 삭제와 데이터 이관이 제외 범위임을 기록한다.

## 제외 범위

- monolith `qtai-server/src/main/resources/db/migration/**` 수정 또는 삭제
- 운영 DB에 migration 실행
- 기존 데이터 백필, 복제, 이관
- prompt version seed 운영 정책 확정
- business usecase, scheduler, worker 이관
- gateway route, JWKS, service-token 작업
- provider endpoint 연결

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/ai-service/build.gradle.kts` | Flyway core/mysql 의존성 추가 |
| Modify | `qtai-server/ai-service/src/main/resources/application.yml` | migration opt-in property와 locations 기본값 추가 |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServicePersistenceProperties.java` | Flyway enabled/locations 설정 추가 |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServicePersistenceConfiguration.java` | JPA validate 전 migration 실행 initializer 추가 |
| Create | `qtai-server/ai-service/src/main/resources/db/migration/V1__create_ai_owned_tables.sql` | AI 소유 7개 테이블 DDL |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/AiServiceMigrationValidationTest.java` | Flyway migrate + Hibernate validate 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-service-db-migration-skeleton_report.md` | 작업 결과와 검증 결과 기록 |

## 구현 순서

1. `dev` 최신화 후 `feature/ai-service-db-migration-skeleton` 브랜치에서 작업한다.
2. workflow 문서를 저장하고 migration skeleton 범위를 고정한다.
3. `ai-service`에 Flyway 의존성과 persistence migration 설정을 추가한다.
4. AI 소유 7개 테이블용 `V1__create_ai_owned_tables.sql`을 작성한다.
5. persistence 활성화 시 Flyway migration이 JPA entity manager 생성 전 수행되게 한다.
6. H2 MySQL mode에서 `ddl-auto=validate`, migration enabled 조건의 context test를 추가한다.
7. 검증 명령을 실행하고 report 문서에 결과를 기록한다.
8. 지정 파일만 stage 후 `feat(ai): ai-service DB migration skeleton 추가`로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiServiceMigrationValidationTest` | Flyway migration 적용, 7개 table 존재, JPA validate context load |
| 기존 `AiServicePersistenceRepositoryTest` | migration 설정 추가 후 기존 H2 create-drop repository 테스트 회귀 |

## 수용 기준

- [ ] `:ai-service:compileJava`가 통과한다.
- [ ] `:ai-service:test`가 통과한다.
- [ ] `qtai.ai.persistence.enabled=false` 기본 실행에서 migration이 강제되지 않는다.
- [ ] `qtai.ai.persistence.enabled=true`와 H2 MySQL mode에서 Flyway migration이 적용된다.
- [ ] Hibernate validate가 AI 소유 entity와 migration schema 정합성을 확인한다.
- [ ] monolith migration, 데이터 이관, 운영 DB 연결은 변경하지 않는다.
- [ ] 금지 번역본/민감값/placeholder가 추가되지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- Flyway 설정, DDL, JPA validate 테스트가 같은 persistence 경계를 공유한다.
- DDL과 entity 정합성은 한 흐름에서 검증해야 하므로 병렬 분할 이득이 낮다.
- 이번 PR은 migration skeleton만 고정하는 좁은 작업이다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, migration skeleton 구현, 테스트 추가, report 작성, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test
cd ..
git diff --check
$placeholderPattern = "TB" + "D|TO" + "DO|추" + "후 정리|나" + "중에 정리|미" + "정"
rg -n $placeholderPattern "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-service-db-migration-skeleton.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-service-db-migration-skeleton_report.md"
$restrictedPattern = "개역" + "개정|ES" + "V|NI" + "V|성서" + "유니온|두" + "란노|plain " + "secret|private " + "key"
rg -n $restrictedPattern "qtai-server\ai-service" "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-service-db-migration-skeleton.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-service-db-migration-skeleton_report.md"
```

## 후속 작업으로 남긴 항목

- 운영 MySQL 연결 smoke test
- prompt version seed 이관 또는 bootstrap 정책 확정
- monolith DB에서 ai-service DB로 데이터 이관 절차 작성
- ai-service usecase persistence skeleton 추가
- 기존 monolith AI persistence 제거 시점 결정
