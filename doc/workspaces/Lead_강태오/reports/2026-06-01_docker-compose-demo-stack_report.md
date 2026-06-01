# 리포트 — Docker Compose 시연 스택(앱+MySQL+Redis) + 기동 차단 버그 2건 수정

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 브랜치: `feature/infra-compose-demo` → PR 대상 `dev`
- 관련: 2단계 "실DB·시연 빌드 기반" 2번 작업 (Docker Compose 시연 빌드). dev MySQL 전환(#177) 후속.

## 1. 한 줄 요약

앱+MySQL+Redis를 한 번에 기동하는 시연용 Docker Compose 스택을 구성하고 실제로 띄워 검증했다. 그 과정에서 **시연 빌드/기동을 막던 버그 2건**(Dockerfile의 CRLF gradlew, 엔티티 `@Lob`↔MySQL 타입 불일치)을 발견·수정했다. **앱이 실제 MySQL에서 처음으로 정상 기동**(`Started QtAiApplication`)했고, H2 테스트 532건 회귀 통과.

## 2. 구성

`docker-compose.yml`(루트)을 풀스택 시연 스택으로 정비:

- `mysql`(8.0, utf8mb4) + `redis`(7) — 각각 healthcheck 추가
- `qtai-server` — `qtai-server/Dockerfile`로 빌드, `depends_on: service_healthy`로 DB/Redis 준비 후 기동, `dev` 프로파일(실 MySQL)
- 비밀값(JWT 키)은 레포 평문 금지(§8) → `.env`로 주입. `.env.example` 제공, `.env`는 .gitignore 등록됨.

실행: `cp .env.example .env`(키 입력) → `docker compose up --build`.

## 3. 발견·수정한 버그 (검증으로 잡음)

### (1) Dockerfile — CRLF gradlew로 빌드 실패
- 증상: 컨테이너 빌드 시 `./gradlew bootJar` → `exit code 127`(실행 불가).
- 원인: Windows 체크아웃의 `gradlew`가 CRLF 줄바꿈 → 리눅스 `/bin/sh`가 해석 실패. **현재 Dockerfile은 시연 빌드가 실패하는 상태였다.**
- 수정: Dockerfile에서 `sed -i 's/\r$//' gradlew` 정규화 후 빌드. 추가로 `.gitattributes`에 `gradlew/*.sh eol=lf` 강제(재발 방지).

### (2) 엔티티 `@Lob` ↔ MySQL 타입 불일치로 기동 실패
- 증상: Flyway는 통과했으나 Hibernate `validate` 실패 — `wrong column type ... [payload_json] found [longtext], but expecting [tinytext (Types#CLOB)]`.
- 원인: `@Lob String`이 MySQLDialect에서 CLOB(tinytext)을 기대 → 마이그레이션의 LONGTEXT와 불일치. (H2Dialect에선 CLOB을 기대해 그동안 H2에선 통과해왔다.) **실 MySQL에서 앱이 한 번도 기동된 적 없던 진짜 이유.**
- 수정: `AiGeneratedAsset.payloadJson`, `AiValidationLog.checklistJson`, `AuditLog.beforeJson/afterJson` 4개 필드를 `@Lob` → `@Column(columnDefinition = "LONGTEXT")`로 변경(코드베이스의 다른 큰 텍스트 컬럼과 동일 패턴). 마이그레이션 LONGTEXT(#177)와 정렬.

> #177(CLOB→LONGTEXT 마이그레이션) + 본 PR(@Lob→LONGTEXT 엔티티) 두 수정이 모두 있어야 실 MySQL 기동이 된다.

## 4. 변경 파일

| 구분 | 파일 |
|------|------|
| 수정 | `docker-compose.yml`(앱 서비스+healthcheck+depends_on 추가), `qtai-server/Dockerfile`(CRLF 정규화) |
| 신규 | `.gitattributes`, `.env.example` |
| 수정 | `ai/internal/AiGeneratedAsset.java`, `ai/internal/AiValidationLog.java`, `audit/internal/AuditLog.java` (@Lob→LONGTEXT) |
| 신규 | 본 리포트 |

## 5. 검증 (실제 컨테이너 기동)

1. **통합 기동**: mysql/redis healthy 후 앱 기동 → Flyway V1~V19 적용 + Hibernate validate 통과 → `Tomcat started on port 8080` / `Started QtAiApplication in 9.6s`. (수정 전: payload_json 타입 불일치로 기동 실패 → 수정 후 정상)
2. **CRLF 수정**: 수정 전 컨테이너 빌드 `exit 127` → 수정 후 gradlew 정상 실행·컴파일.
3. **H2 회귀**: `./gradlew test` → **532건 전부 통과(실패 0)**. 엔티티 변경이 기존 테스트에 영향 없음.

## 6. 범위 / 후속 / 공유

- 수정한 엔티티는 ai·audit 도메인(강상민) → PR에서 공유.
- 참고: 이 머신에서 컨테이너 내 gradle 빌드가 다소 느렸음(리소스). 기능은 정상(동일 jar로 기동 검증). CI는 Gradle 캐시 사용 권장.
- **후속(2단계 마지막)**: Testcontainers(MySQL) Flyway migrate+validate 가드 — 이번에 수동으로 잡은 (CLOB/@Lob류) 기동 차단 버그를 CI에서 자동 차단.
