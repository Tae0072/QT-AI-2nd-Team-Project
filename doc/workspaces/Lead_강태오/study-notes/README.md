# Study Notes — 배우지 않은 기술/방법 정리

> **생성일:** 2026-05-28
> **목적:** 노션 기술 블로그에서 이미 배운 내용과 QT-AI 프로젝트에서 실제 사용하는 기술을 비교하여, 아직 배우지 않은 기술/방법을 개별 파일로 정리한 폴더
> **비교 기준:**
> - 노션 기술 블로그: Java, Network, DB, 웹 프론트, Spring(v1-v4), MSA(Docker, K8s, Kafka, Flutter), Git
> - QT-AI 프로젝트 스택: Java 21, Spring Boot 3.3, Gradle, Spring Modulith, ArchUnit, JWT, H2, Caffeine, GitHub Actions, Docker Compose, OpenAPI, JaCoCo, Gitleaks 등

## 파일 목록

| # | 파일명 | 기술/방법 | 프로젝트 사용 위치 |
|---|--------|-----------|-------------------|
| 1 | `01_spring-boot-3.md` | Spring Boot 3.x 자동 설정 | qtai-server 전체 |
| 2 | `02_spring-modulith.md` | Spring Modulith 모듈 경계 | 도메인 간 경계 검증 |
| 3 | `03_archunit.md` | ArchUnit 아키텍처 테스트 | 패키지 import 금지 검증 |
| 4 | `04_gradle-kotlin-dsl.md` | Gradle Kotlin DSL 빌드 | build.gradle.kts |
| 5 | `05_junit5-spring-test.md` | JUnit 5 + Spring Test | 전체 테스트 코드 |
| 6 | `06_jwt-authentication.md` | JWT 토큰 인증 | member 도메인 인증 |
| 7 | `07_h2-database.md` | H2 인메모리 테스트 DB | 테스트 환경 |
| 8 | `08_caffeine-cache.md` | Caffeine 앱 캐시 | Today QT 캐시 |
| 9 | `09_github-actions.md` | GitHub Actions CI/CD | .github/workflows/ |
| 10 | `10_openapi-spectral.md` | OpenAPI + Spectral 검증 | API 명세 자동 검사 |
| 11 | `11_docker-compose.md` | Docker Compose 멀티 컨테이너 | 로컬/배포 환경 |
| 12 | `12_spring-event-publisher.md` | Spring Event 발행/구독 | 도메인 간 이벤트 |
| 13 | `13_conventional-commits.md` | Conventional Commits 규칙 | Git 커밋 메시지 |
| 14 | `14_gitleaks.md` | Gitleaks 시크릿 스캔 | PR 전 보안 검사 |
| 15 | `15_jacoco.md` | JaCoCo 코드 커버리지 | 품질 게이트 검증 |
| 16 | `16_flyway-db-migration.md` | Flyway 마이그레이션 + SQL 방언(CLOB→LONGTEXT) | db/migration, dev/prod MySQL |
| 17 | `17_jpa-hibernate-schema-validation.md` | JPA/Hibernate 스키마 검증(ddl-auto=validate, @Lob, Dialect) | 전체 엔티티, dev/prod 기동 검증 |
| 18 | `18_testcontainers.md` | Testcontainers 실DB 통합 테스트 | MysqlMigrationValidationTest |
| 19 | `19_spring-profiles-and-config.md` | Spring 프로파일·외부 설정 주입(.env, @DynamicPropertySource) | application-*.yml, 통합 테스트 |
| 20 | `20_dockerfile-multistage-and-line-endings.md` | Dockerfile 멀티스테이지 + CRLF/LF·.gitattributes | qtai-server/Dockerfile |
| 21 | `21_git-merge-strategies.md` | git 고급 병합(merge -s ours, 스쿼시 후유증) | dev→master 릴리스(#173) |

> **2026-06-01 추가(16~21):** dev 프로파일 MySQL 전환·Docker Compose 시연 빌드·통합 E2E·dev→master 릴리스 작업에서 등장한, 노션·기존 노트에 없던 기술을 정리.
