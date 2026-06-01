package com.qtai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 MySQL 8.0(Testcontainers) 기반 Flyway migrate + Hibernate validate 가드.
 *
 * <p>배경: 기존 테스트는 H2(MODE=MYSQL)로만 돈다. H2는 {@code CLOB} 같은 MySQL 비호환 DDL을
 * 받아주고, {@code @Lob} 컬럼 타입도 MySQL과 다르게 검증하므로, "H2에선 통과하지만 실제 MySQL에선
 * 기동 실패"하는 버그(2026-06-01 발견: V6/V12 CLOB, ai/audit @Lob)가 그동안 드러나지 않았다.
 *
 * <p>이 테스트는 운영과 동일하게 <b>실 MySQL 8.0에 Flyway로 마이그레이션을 적용</b>하고
 * <b>Hibernate {@code ddl-auto=validate}로 엔티티↔스키마 정합성을 검증</b>한다. 컨텍스트가 뜨면 통과,
 * MySQL 비호환 DDL이나 엔티티-컬럼 타입 불일치가 있으면 기동 실패로 잡아 PR 단계에서 차단한다.
 *
 * <p>Docker가 없는 환경에서는 자동 skip된다({@code disabledWithoutDocker = true}) — 로컬 개발은
 * 막지 않고, Docker가 있는 CI에서 가드로 동작한다.
 *
 * <p>test 프로파일을 활성화해 JWT 테스트 키·외부 연동 더미값을 재사용하되, DB 관련 속성만
 * Testcontainers MySQL로 덮어써 "운영형(Flyway+validate)" 컨텍스트를 만든다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MysqlMigrationValidationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qtai")
            .withUsername("qtai")
            .withPassword("qtai");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        // 운영과 동일: Flyway가 스키마를 만들고 Hibernate는 검증만 한다.
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        // 검증 중 배치 워커가 DB에 접근하지 않도록 비활성화
        registry.add("ai.generation.worker.enabled", () -> "false");
    }

    /** 실 MySQL에 전 마이그레이션 적용 + 엔티티 validate 통과 시 컨텍스트가 뜨면 성공. */
    @Test
    void 실제_MySQL에서_Flyway_적용과_Hibernate_검증을_통과한다() {
    }
}
