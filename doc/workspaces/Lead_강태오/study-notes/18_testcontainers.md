# Testcontainers — 진짜 DB로 통합 테스트

> **왜 배워야 하나:** 노션에서 Docker 기초는 배웠지만, "테스트가 돌 때 자동으로 진짜 DB 컨테이너를 띄워서 검사하는" Testcontainers는 다루지 않았다. QT-AI는 H2(가짜 DB)로만 테스트하면 못 잡는 MySQL 전용 버그(CLOB, @Lob)를 CI에서 자동으로 막기 위해 Testcontainers를 도입했다.

---

## 1. 문제 상황

- 평소 테스트는 H2(인메모리 가짜 DB)로 빠르게 돈다.
- 하지만 H2는 MySQL을 "흉내"만 내서, 실제 MySQL에서만 터지는 문제(예: `CLOB` 문법, `@Lob` 타입 검증)를 놓친다.
- 그렇다고 사람이 매번 손으로 MySQL을 띄워 확인할 순 없다.

## 2. Testcontainers가 하는 일

테스트가 실행될 때 **Docker로 진짜 MySQL 8.0 컨테이너를 잠깐 띄우고**, 테스트가 끝나면 자동으로 지운다.

```
테스트 시작 → (Docker) mysql:8.0 컨테이너 起動 → 테스트 → 컨테이너 삭제
```

## 3. 설정 (Gradle)

```kotlin
// build.gradle.kts — 버전은 Spring Boot BOM이 관리해줘서 생략 가능
testImplementation("org.testcontainers:junit-jupiter")
testImplementation("org.testcontainers:mysql")
```

## 4. 실제 코드 (QT-AI 가드)

```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)   // Docker 없으면 자동 skip
class MysqlMigrationValidationTest {

    @Container
    static final MySQLContainer<?> MYSQL =
        new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qtai").withUsername("qtai").withPassword("qtai");

    // 컨테이너 주소를 스프링 설정에 "동적으로" 주입
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.flyway.enabled", () -> "true");          // Flyway로 스키마 생성
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); // 엔티티 검증
        r.add("spring.jpa.properties.hibernate.dialect",
              () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Test
    void 실제_MySQL에서_Flyway적용과_검증을_통과한다() {
        // 컨텍스트가 뜨면 통과 = 마이그레이션 적용 + 엔티티 검증 성공
    }
}
```

핵심 포인트:
- `@Container` + `MySQLContainer`: 진짜 MySQL을 띄운다.
- `@DynamicPropertySource`: 컨테이너 주소는 매번 바뀌므로, 실행 시점에 스프링 설정에 끼워 넣는다(고정값으로 못 씀).
- `disabledWithoutDocker = true`: Docker 없는 PC에선 이 테스트만 **건너뛴다**(다른 테스트는 정상).

## 5. 어디서 실제로 돌까?

- **CI(GitHub Actions, 리눅스)**: Docker가 기본 제공 → 테스트 실행 → MySQL 전용 버그를 PR 단계에서 자동 차단.
- **로컬(Windows + Docker Desktop)**: Docker 감지가 까다로워 skip되기도 함 → 그래도 CI가 막아주므로 OK.

## 6. H2 테스트 vs Testcontainers 비교

| | H2 테스트 | Testcontainers(MySQL) |
|---|---|---|
| 속도 | 매우 빠름 | 컨테이너 起動 ~수십 초 |
| 정확도 | MySQL과 다를 수 있음 | 운영과 동일 |
| 용도 | 대부분의 단위·통합 테스트 | DB 호환성 가드 |

→ 둘을 **섞어 쓴다**: 평소엔 H2로 빠르게, 핵심 DB 정합성은 Testcontainers로 진짜 확인.

## 7. QT-AI에서의 적용

- `MysqlMigrationValidationTest`가 "실 MySQL + Flyway + Hibernate validate"를 자동 수행.
- 이번 세션에 손으로 잡은 CLOB·@Lob 류 버그를 앞으로 CI가 자동으로 막는다.

## 8. 참고 자료

- Testcontainers: https://testcontainers.com/
- Spring Boot + Testcontainers: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
