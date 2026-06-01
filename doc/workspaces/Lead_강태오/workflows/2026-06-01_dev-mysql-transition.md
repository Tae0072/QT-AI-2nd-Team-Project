# 워크플로우 — dev 프로파일 MySQL 전환 + Flyway MySQL 호환

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 대상 저장소: QT-AI-2nd-Team-Project (`qtai-server`)
- 기준 문서: `CLAUDE.md` §1(DB), 2단계 "실DB·시연 빌드 기반"
- PR: #177 (머지됨)

## 1. 배경

dev/prod가 실DB(MySQL)를 써야 MySQL 고유 문제를 시연/운영 전에 잡을 수 있다. 기존엔 dev도 H2였다. "표준 분리" 방침: test/local은 H2 유지(빠름·인프라 불필요), dev/prod는 MySQL.

## 2. 작업 범위

- `application-dev.yml`을 H2 → MySQL로 전환(env 주입, 기본값 Docker MySQL).
- Flyway 마이그레이션이 실제 MySQL에서 적용되는지 검증.
- prod는 이미 MySQL, test/local은 H2 유지(변경 없음).

## 3. 절차

1. `feature/infra-dev-mysql` 브랜치.
2. `application-dev.yml`: datasource를 `jdbc:mysql://...`(MySQLDialect, validate)로 변경, dev-bypass 유지.
3. 실제 MySQL 8.0 컨테이너에 Flyway migrate(V1~V19) 실행 → 호환성 검증.
4. 발견된 비호환(`CLOB`은 MySQL 문법 아님) 수정: V6/V12 `CLOB` → `LONGTEXT`(Hibernate @Lob→MySQL 기본 매핑과 일치, H2도 지원).
5. 실 MySQL 재적용 성공 + H2 전체 테스트 회귀 통과 후 PR.

## 4. 정책 준수

- 수정한 마이그레이션은 ai·audit 도메인(강상민) 스키마 → PR 공유.
- 영구 적용 이력 환경이 없어(test=Flyway off, dev/local=in-memory, prod=미배포) 기존 마이그레이션 직접 수정이 checksum 충돌 없이 안전.

## 5. 검증 명령

```powershell
docker run --rm -v "<migration>:/flyway/sql" flyway/flyway:10 -url="jdbc:mysql://host.docker.internal:<port>/qtai" -user=qtai -password=qtai migrate
cd qtai-server; .\gradlew.bat test --no-daemon
```
