# 워크플로우 — Testcontainers MySQL Flyway+validate 가드

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 대상 저장소: QT-AI-2nd-Team-Project (`qtai-server`)
- 기준 문서: `CLAUDE.md` §10, 2단계 마지막
- PR: #181 (머지됨)

## 1. 배경

이번 세션에서 수동으로 잡은 "H2에선 통과하지만 실 MySQL에선 기동 실패" 버그(CLOB, @Lob)를 사람이 매번 확인할 수 없다. CI에서 실 MySQL로 Flyway migrate + Hibernate validate를 자동 수행해 자동 차단한다.

## 2. 작업 범위

- Testcontainers(MySQL 8.0) 기반 `@SpringBootTest` 가드 1건.
- Docker 없는 환경은 자동 skip(로컬 비차단), Docker 있는 CI에서 동작.

## 3. 절차

1. `test/mysql-validation-guard` 브랜치.
2. `build.gradle.kts`에 `testcontainers:junit-jupiter`, `:mysql` 추가(버전은 Spring Boot BOM).
3. `MysqlMigrationValidationTest`: `@Testcontainers(disabledWithoutDocker=true)` + `MySQLContainer` + `@DynamicPropertySource`로 `flyway.enabled=true`, `ddl-auto=validate`, `MySQLDialect` 주입(운영형 컨텍스트). `@ActiveProfiles("test")`로 JWT/외부 더미 재사용.
4. CI 실행 보장 확인(`qt-ai-ci.yml`: ubuntu-latest + `./gradlew test`, Docker 기본 제공).

## 4. 정책 준수 / 한계

- 로컬 Windows+Docker Desktop은 Testcontainers의 docker-java npipe 감지가 안 돼 로컬에선 skip(환경 특이사항, 리눅스 CI 무관). 동작 동치는 #178의 앱 실 MySQL 기동으로 확인.

## 5. 검증 명령

```powershell
cd qtai-server
.\gradlew.bat test --tests "com.qtai.MysqlMigrationValidationTest" --no-daemon   # CI(리눅스)에서 실행
```
