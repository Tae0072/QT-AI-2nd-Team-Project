# Flyway — DB 마이그레이션과 SQL 방언 차이

> **왜 배워야 하나:** 노션에서 DB/SQL/JDBC는 배웠지만, "스키마 변경을 버전으로 관리하는 도구(Flyway)"와 "같은 SQL이 DB마다 다르게 동작하는 방언(dialect) 문제"는 다루지 않았다. QT-AI는 Flyway로 테이블을 만들고, 이번에 H2에선 통과하지만 실제 MySQL에선 실패하는 버그(CLOB)를 겪었다.

---

## 1. Flyway가 뭔가?

테이블 구조(스키마)를 직접 손으로 바꾸지 않고, **번호가 붙은 SQL 파일**로 관리하는 도구다.

```
src/main/resources/db/migration/
  V1__create_members.sql
  V2__create_bible.sql
  ...
  V19__seed_sample_mission_definitions.sql
```

- 파일명 규칙: `V{번호}__{설명}.sql` (언더바 2개).
- 앱이 시작될 때 Flyway가 **아직 적용 안 된 번호**를 순서대로 실행한다.
- 어디까지 적용했는지는 `flyway_schema_history` 테이블에 기록된다. 그래서 같은 마이그레이션이 두 번 실행되지 않는다.

## 2. 왜 좋은가?

| 직접 ALTER TABLE | Flyway |
|---|---|
| 누가 언제 뭘 바꿨는지 모름 | SQL 파일이 곧 변경 이력 |
| 팀원마다 DB가 달라짐 | 같은 파일 → 같은 스키마 |
| 운영 반영 빠뜨림 | 앱 시작 시 자동 적용 |

## 3. 핵심 설정 (Spring Boot)

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true   # 이미 있는 DB에 처음 적용할 때 기준선 생성
  jpa:
    hibernate:
      ddl-auto: validate        # Flyway가 만든 스키마를 Hibernate가 "검증만" (자세한 건 17번 노트)
```

> 핵심 분업: **Flyway가 스키마를 만들고, Hibernate(JPA)는 검증만 한다.** Hibernate가 테이블을 만들게 두지 않는다.

## 4. 함정 — SQL 방언(dialect) 차이

같은 SQL이 DB마다 다르게 해석된다. 이번에 실제로 겪은 사례:

```sql
-- V6, V12에 있던 코드
payload_json   CLOB        -- H2에선 OK, MySQL에선 문법 에러(1064)!
```

- `CLOB`은 표준 SQL/H2 타입이지만 **MySQL에는 없는 타입**이다.
- 그동안 테스트가 H2(`MODE=MYSQL` 흉내 모드)로만 돌아서 안 걸렸고, 실제 MySQL에 올리니 `Migrating to V6 ... ERROR 1064`로 실패했다.
- 수정: `CLOB` → `LONGTEXT` (MySQL·H2 둘 다 지원하는 큰 텍스트 타입).

MySQL에서 자주 쓰는 타입(마이그레이션에서 본 것):

```sql
id          BIGINT AUTO_INCREMENT PRIMARY KEY,
created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at  DATETIME ON UPDATE CURRENT_TIMESTAMP,
body        LONGTEXT,                                   -- 긴 텍스트(CLOB 대신)
... ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 5. 이미 만든 마이그레이션은 고쳐도 되나?

- 한 번 **운영 DB에 적용된** 마이그레이션 파일은 **수정 금지**다. Flyway가 파일 내용으로 체크섬을 계산해두기 때문에, 내용이 바뀌면 다음 실행 때 "체크섬 불일치"로 실패한다.
- 바꿔야 하면 **새 번호(V20)로 ALTER 문을 추가**하는 게 원칙.
- 단, 아직 어디에도 영구 적용되지 않았다면(개발 초기) 기존 파일 수정이 안전하다. 이번 CLOB 수정이 그런 경우였다.

## 6. QT-AI에서의 적용

- `qtai-server/src/main/resources/db/migration/` 에 V1~V19+ 가 있다.
- `dev`/`prod` 프로파일은 Flyway ON + `ddl-auto=validate`(실DB), `test`/`local`은 H2.
- 이번에 실제 MySQL로 검증해 CLOB 버그를 잡았고, CI에서 자동으로 잡도록 Testcontainers 가드를 추가했다(18번 노트).

## 7. 자주 쓰는 확인

```bash
# 실제 MySQL에 마이그레이션 적용해보기 (flyway 공식 이미지)
docker run --rm -v "<migration폴더>:/flyway/sql" flyway/flyway:10 \
  -url="jdbc:mysql://localhost:3306/qtai" -user=qtai -password=qtai migrate
```

## 8. 참고 자료

- Flyway 공식: https://documentation.red-gate.com/fd
- MySQL 데이터 타입: https://dev.mysql.com/doc/refman/8.0/en/data-types.html
