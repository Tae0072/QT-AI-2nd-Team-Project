# 리포트 — dev 프로파일 MySQL 전환 + Flyway MySQL 호환 수정

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 브랜치: `feature/infra-dev-mysql` → PR 대상 `dev`
- 관련: 2단계 "실DB·시연 빌드 기반" 첫 작업 (H2→MySQL 전환)

## 1. 한 줄 요약

dev 프로파일을 H2에서 **실제 MySQL 8.0**으로 전환했고, 그 과정에서 **실제 MySQL에 마이그레이션을 직접 돌려** `CLOB`(MySQL 비호환) 구문 버그를 발견·수정했다. 마이그레이션 V1~V19 전부 실 MySQL에서 적용 성공, H2 테스트(532건) 회귀 통과.

## 2. 배경 — 프로파일별 DB 정책 (표준 분리)

| 프로파일 | DB | 용도 |
|----------|----|----|
| `test` | H2 (create-drop, Flyway off) | 단위/슬라이스 테스트 — 빠름, 인프라 불필요 (변경 없음) |
| `local` | H2 (in-memory, MODE=MYSQL) | 노트북 빠른 개발 — 인프라 불필요 (변경 없음) |
| `dev` | **MySQL 8.0** ← 이번 변경 | Docker Compose 환경 / 시연 — 실DB |
| `prod` | MySQL 8.0 | 운영 (이미 MySQL, 변경 없음) |

기존엔 dev도 H2였다. dev/prod가 실DB를 쓰게 해야 MySQL 고유 문제를 시연/운영 전에 잡을 수 있다.

## 3. 발견·수정한 버그 (중요)

검증을 위해 **실제 MySQL 8.0 컨테이너에 Flyway로 직접 마이그레이션**한 결과:

```
Migrating ... to version "6 - create auth ai explanation tables"
ERROR: Error Code: 1064 ... near 'CLOB NOT NULL'
```

- 원인: 마이그레이션 4개 컬럼이 `CLOB` 타입 사용. **`CLOB`은 MySQL 문법이 아니다.** H2의 `MODE=MYSQL` 호환 모드에서만 통과해와서, 이 마이그레이션들은 **실제 MySQL에서 한 번도 적용된 적이 없었다**(prod 미배포 + dev/local은 H2였음).
- 위치: `V6`(payload_json, checklist_json), `V12`(before_json, after_json) — 모두 `@Lob` JSON 컬럼.
- 수정: `CLOB` → **`LONGTEXT`**. Hibernate의 `@Lob` String → MySQL 기본 매핑이 정확히 `LONGTEXT`이고, H2(MODE=MYSQL)도 `LONGTEXT`를 지원하므로 dev/local/prod 모두 호환.
- 안전성: 영구 적용 이력이 있는 환경이 없어(test=Flyway off, dev/local=in-memory 재적용, prod=미배포) 기존 마이그레이션 파일 직접 수정이 checksum 충돌 없이 안전.

## 4. 변경 파일

| 구분 | 파일 | 내용 |
|------|------|------|
| 수정 | `application-dev.yml` | datasource를 H2 → MySQL(env 주입, 기본값=Docker MySQL), Dialect H2→MySQL. dev-bypass 유지 |
| 수정 | `db/migration/V6__create_auth_ai_explanation_tables.sql` | `CLOB`→`LONGTEXT` (payload_json, checklist_json) |
| 수정 | `db/migration/V12__create_audit_logs.sql` | `CLOB`→`LONGTEXT` (before_json, after_json) |
| 신규 | 본 리포트 |

## 5. 검증 (실제 MySQL + H2 회귀)

1. **실 MySQL 8.0 컨테이너에 Flyway migrate**: 수정 전 V6에서 CLOB 에러 → 수정 후 **V1~V19 19개 전부 적용 성공** (`now at version v19`).
2. **H2 전체 테스트 회귀**: `./gradlew test` → **532건 전부 통과(실패·에러 0)**. 컨텍스트 로드 테스트(#167) 포함 통과 — 엔티티 매핑·마이그레이션 커버리지 가드 모두 정상.
3. 검증 인프라: `docker compose`의 MySQL 8.0 (검증은 포트 충돌 회피용 임시 컨테이너 13306으로 수행, 레포 파일 불변).

## 6. 범위 / 후속

- 본 PR: dev 프로파일 MySQL 전환 + 마이그레이션 MySQL 호환 수정까지.
- **후속(2단계 잔여)**: ① Docker Compose 시연 빌드(앱+MySQL+Redis 한 번에 기동 검증) ② Testcontainers(MySQL) 기반 Flyway migrate+validate 가드 — 이번에 수동으로 한 실DB 검증을 CI에 자동화. 이번 CLOB류 버그를 앞으로 자동 차단.
- 수정한 마이그레이션 V6/V12는 ai·audit 도메인(강상민) 스키마이므로 PR에서 공유.
