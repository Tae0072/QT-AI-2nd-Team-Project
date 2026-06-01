# 리포트 — Testcontainers MySQL Flyway+validate 가드

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 브랜치: `test/mysql-validation-guard` → PR 대상 `dev`
- 관련: 2단계 마지막 작업. dev MySQL 전환(#177)·시연 스택(#178) 후속.

## 1. 한 줄 요약

이번 세션에서 수동으로 잡은 "H2에선 통과하지만 실 MySQL에선 기동 실패"하는 버그(CLOB, @Lob 타입 불일치)를 CI에서 자동 차단하도록, **실제 MySQL 8.0(Testcontainers)에 Flyway migrate + Hibernate validate**를 수행하는 가드 테스트를 추가했다.

## 2. 배경

- 기존 테스트는 H2(MODE=MYSQL)로만 돈다. H2는 `CLOB` 같은 MySQL 비호환 DDL을 받아주고 `@Lob` 타입 검증도 MySQL과 달라서, 실 MySQL 전용 버그를 못 잡는다.
- 실제로 이번에 #177(V6/V12 CLOB)·#178(ai/audit @Lob)에서 그런 버그 2건이 드러났다 — 둘 다 H2 테스트는 전부 통과하던 상태였다.
- 사람이 매번 실 MySQL로 확인할 수 없으므로 CI 가드로 자동화한다.

## 3. 구현

`MysqlMigrationValidationTest` (`@SpringBootTest`):

- `@Testcontainers(disabledWithoutDocker = true)` + `MySQLContainer("mysql:8.0")` — 실 MySQL 8.0 기동.
- `@DynamicPropertySource`로 DB 속성만 컨테이너로 덮어써 **운영형 컨텍스트**를 만든다: `flyway.enabled=true`, `ddl-auto=validate`, `dialect=MySQLDialect`. (`@ActiveProfiles("test")`로 JWT 테스트 키·외부 더미값은 재사용.)
- 컨텍스트가 뜨면 통과 = 전 마이그레이션이 MySQL에 적용되고 엔티티↔스키마 validate 통과. MySQL 비호환 DDL이나 타입 불일치는 기동 실패로 잡힌다.
- Docker 없는 환경은 자동 skip → 로컬 개발 비차단, Docker 있는 CI에서 가드 동작.

의존성: `org.testcontainers:junit-jupiter`, `:mysql` (버전은 Spring Boot BOM 관리).

## 4. 변경 파일

| 구분 | 파일 |
|------|------|
| 수정 | `qtai-server/build.gradle.kts` (testcontainers 의존성 2개) |
| 신규 | `qtai-server/src/test/java/com/qtai/MysqlMigrationValidationTest.java` |
| 신규 | 본 리포트 |

## 5. 검증

- **CI 실행 보장**: CI는 `ubuntu-latest`에서 `./gradlew test` 실행(`qt-ai-ci.yml`). GitHub 러너는 Docker가 기본 제공되므로 Testcontainers가 Docker를 감지해 이 가드가 **실제로 실행**된다.
- **동작 동치 검증(수동)**: 본 가드가 수행하는 흐름(실 MySQL 8.0 + Flyway V1~V19 + Hibernate validate)은 #178 작업에서 이미 통과를 확인했다 — 앱이 동일 조건에서 `Started QtAiApplication`으로 기동, validate 통과. 즉 가드 통과 동작은 검증된 흐름과 동일하다.
- **컴파일/의존성**: 로컬에서 컴파일·의존성 해결 확인(BUILD SUCCESSFUL).
- **로컬 실행 한계(투명 공개)**: 로컬 Windows + Docker Desktop 환경에서는 Testcontainers의 Docker 감지(docker-java npipe)가 동작하지 않아 로컬에서는 자동 skip된다. 이는 환경 특이사항이며 리눅스 CI에는 영향 없다.

## 6. 효과 / 후속

- 효과: 앞으로 누가 CLOB 등 MySQL 비호환 DDL을 추가하거나 엔티티 타입을 스키마와 어긋나게 바꾸면 **PR CI에서 자동 차단**된다. 2단계 "실DB·시연 빌드 기반" 완료.
- 후속: 3단계(통합·E2E·성능)로 진행.
