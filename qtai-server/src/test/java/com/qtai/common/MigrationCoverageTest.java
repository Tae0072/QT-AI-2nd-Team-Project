package com.qtai.common;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Flyway 마이그레이션 ↔ JPA 엔티티 커버리지 가드(정적 검증).
 *
 * <p>배경: dev/prod 프로파일은 {@code ddl-auto=validate} + Flyway다. "엔티티는 추가했으나
 * 마이그레이션을 빠뜨린" 경우(= 2026-05-29 reports/mission/admin 누락으로 기동 실패, #145) 기존
 * test 프로파일(flyway off + create-drop)은 이를 잡지 못한다.
 *
 * <p>이 테스트는 Docker/DB 없이, 모든 {@code @Entity}의 매핑 테이블명이 {@code db/migration}의 어떤
 * {@code CREATE TABLE}에든 존재하는지 정적으로 검사한다. 누락 시 실패해 PR 단계에서 차단한다.
 *
 * <p>한계: 컬럼/타입 단위 정합성과 누락 빈(컨텍스트 와이어링)은 검증하지 않는다 — 그 영역은
 * Testcontainers(MySQL) 기반 Flyway migrate+validate / @SpringBootTest 컨텍스트 로드로 후속 보강.
 */
class MigrationCoverageTest {

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?`?([a-zA-Z_][a-zA-Z0-9_]*)`?");

    @Test
    void 모든_엔티티_테이블이_Flyway_CREATE_TABLE에_존재한다() throws Exception {
        Set<String> entityTables = scanEntityTables();
        Set<String> migratedTables = scanMigratedTables();

        // 거짓 통과 방지: 스캔이 비면(클래스패스/리소스 경로 오류) 검증 자체가 무의미하므로 실패시킨다.
        assertThat(entityTables).as("@Entity 스캔 결과가 비어 있음 — 스캐너 구성 오류 의심").isNotEmpty();
        assertThat(migratedTables).as("db/migration CREATE TABLE 스캔 결과가 비어 있음 — 리소스 경로 오류 의심").isNotEmpty();

        Set<String> missing = new TreeSet<>(entityTables);
        missing.removeAll(migratedTables);

        assertThat(missing)
                .as("@Entity 인데 Flyway 마이그레이션에 CREATE TABLE이 없는 테이블 — "
                        + "validate 프로파일(dev/prod)에서 기동 실패를 유발한다. db/migration에 마이그레이션을 추가하세요.")
                .isEmpty();
    }

    /** com.qtai 하위 모든 @Entity의 매핑 테이블명(소문자) 수집. */
    private Set<String> scanEntityTables() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        Set<String> tables = new TreeSet<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("com.qtai")) {
            Class<?> clazz = Class.forName(bd.getBeanClassName());
            Table table = clazz.getAnnotation(Table.class);
            String name = (table != null && !table.name().isBlank())
                    ? table.name()
                    : toSnakeCase(clazz.getSimpleName());
            tables.add(name.toLowerCase());
        }
        return tables;
    }

    /** classpath의 db/migration/*.sql에서 CREATE TABLE 대상 테이블명(소문자) 수집. */
    private Set<String> scanMigratedTables() throws IOException {
        Set<String> tables = new TreeSet<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (Resource r : resolver.getResources("classpath*:db/migration/*.sql")) {
            String sql = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = CREATE_TABLE.matcher(sql);
            while (m.find()) {
                tables.add(m.group(1).toLowerCase());
            }
        }
        return tables;
    }

    /** Hibernate 기본 명명 전략(@Table 미지정 시)과 동일하게 카멜→스네이크 변환. */
    private String toSnakeCase(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
