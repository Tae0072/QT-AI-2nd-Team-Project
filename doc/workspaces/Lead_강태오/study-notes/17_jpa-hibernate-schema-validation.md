# JPA / Hibernate — 스키마 검증과 @Lob 매핑

> **왜 배워야 하나:** 노션에서 JDBC(자바-DB 연결)는 배웠지만, 그 위에서 객체-테이블을 자동 매핑하는 JPA/Hibernate, 그리고 "엔티티와 실제 테이블이 맞는지 검사하는 validate"는 다루지 않았다. 이번에 `@Lob` 매핑이 MySQL과 안 맞아 앱이 아예 안 뜨는 버그를 겪었다.

---

## 1. JPA / Hibernate 한 줄 정리

- **JPA**: 자바 객체(Entity)를 DB 테이블에 자동으로 매핑하는 표준(인터페이스).
- **Hibernate**: 그 JPA를 실제로 구현한 라이브러리(Spring Boot 기본).

```java
@Entity
@Table(name = "qt_passages")
public class QtPassage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;          // → qt_passages.id (BIGINT AUTO_INCREMENT)

    @Column(name = "qt_date", nullable = false, unique = true)
    private LocalDate qtDate; // → qt_passages.qt_date (DATE, UNIQUE)
}
```

## 2. ddl-auto — Hibernate가 스키마를 어떻게 다루나

`spring.jpa.hibernate.ddl-auto` 값에 따라 동작이 다르다:

| 값 | 동작 | 어디서 쓰나 |
|----|------|------------|
| `create-drop` | 시작 시 테이블 새로 만들고 종료 시 삭제 | **test/local**(H2) — 빠른 테스트 |
| `validate` | 테이블을 **만들지 않고**, 엔티티와 실제 컬럼이 맞는지 **검증만** | **dev/prod**(MySQL) — Flyway가 만든 스키마 확인 |
| `update` | 차이를 자동 반영(위험, 운영 금지) | 거의 안 씀 |
| `none` | 아무것도 안 함 | — |

> QT-AI 분업: **Flyway가 스키마 생성 → Hibernate는 `validate`로 "내 엔티티랑 맞아?"만 확인.** 안 맞으면 앱이 기동에 실패해서 배포 전에 잡힌다.

## 3. validate가 잡아준 실제 버그 (@Lob)

이번에 앱이 MySQL에서 이 에러로 안 떴다:

```
Schema-validation: wrong column type encountered in column [payload_json]
  in table [ai_generated_assets];
  found [longtext], but expecting [tinytext (Types#CLOB)]
```

**원인:**
```java
@Lob                              // ← 문제
@Column(name = "payload_json")
private String payloadJson;
```
- `@Lob`을 붙이면 Hibernate가 "이건 LOB(대용량) 타입"이라고 본다.
- 그런데 MySQL 방언에서 `@Lob String`이 기대하는 타입과, 마이그레이션이 만든 `LONGTEXT`가 어긋났다.
- H2에선 우연히 맞아서 그동안 안 걸렸다(테스트가 H2라서).

**수정:** `@Lob`을 떼고 컬럼 타입을 명시:
```java
@Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
private String payloadJson;
```
- 코드베이스의 다른 큰 텍스트 컬럼들도 `@Column(columnDefinition = "TEXT")` 패턴을 쓰고 있었다(일관성).

## 4. Dialect(방언) — DB마다 타입 변환 규칙이 다르다

```yaml
# dev/prod (MySQL)
spring.jpa.properties.hibernate.dialect: org.hibernate.dialect.MySQLDialect
# test/local (H2)
spring.jpa.properties.hibernate.dialect: org.hibernate.dialect.H2Dialect
```
- Dialect는 "자바 타입 ↔ DB 타입" 변환 규칙 모음이다.
- **같은 엔티티라도 H2Dialect와 MySQLDialect가 다르게 검증**한다 → 그래서 H2에서만 테스트하면 MySQL 문제를 놓친다.

## 5. 교훈

- 테스트(H2)와 실제(MySQL)가 다르면, "테스트 통과 ≠ 운영 정상"이 될 수 있다.
- 그래서 ① Flyway를 실제 MySQL로 적용해보고(16번), ② Hibernate validate를 실제 MySQL로 돌려보는(18번) 자동 검증이 필요하다.

## 6. QT-AI에서의 적용

- 거의 모든 엔티티가 `@Entity` + `@Column`으로 매핑.
- dev/prod는 `ddl-auto=validate`라, 엔티티와 마이그레이션이 어긋나면 기동 실패로 즉시 드러난다.
- `@Lob` 대신 `columnDefinition`으로 타입을 명시하는 게 이 프로젝트의 안전한 패턴.

## 7. 참고 자료

- Hibernate ORM: https://hibernate.org/orm/documentation/
- Spring Data JPA: https://docs.spring.io/spring-data/jpa/reference/
