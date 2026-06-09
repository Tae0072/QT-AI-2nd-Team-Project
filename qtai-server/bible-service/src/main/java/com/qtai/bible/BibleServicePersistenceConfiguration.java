package com.qtai.bible;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * bible-service 전용 DB(DB-per-service) 구성. {@code qtai.bible.persistence.enabled=true}일 때만 활성화.
 *
 * <p>skeleton 단계는 비활성(false)이라 모놀리식이 bible 테이블을 계속 소유하고, 본 구성은 구조만 보유한다.
 * DB 분리(Inc4)에서 전용 DataSource/EntityManagerFactory/Flyway로 bible_books·bible_verses를 이관한다.
 * ai-service의 {@code AiServicePersistenceConfiguration} 패턴과 동일.
 */
@Configuration
@ConditionalOnProperty(name = "qtai.bible.persistence.enabled", havingValue = "true")
@EnableConfigurationProperties(BibleServicePersistenceProperties.class)
@EntityScan("com.qtai.domain.bible.internal")
@EnableJpaAuditing
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.qtai.domain.bible.internal",
        entityManagerFactoryRef = "bibleServiceEntityManagerFactory",
        transactionManagerRef = "bibleServiceTransactionManager"
)
public class BibleServicePersistenceConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BibleServicePersistenceConfiguration.class);

    @Bean(name = "bibleServiceDataSource")
    DataSource bibleServiceDataSource(BibleServicePersistenceProperties properties) {
        DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .url(properties.requireUrl());
        if (properties.hasDriverClassName()) {
            builder.driverClassName(properties.driverClassName());
        }
        if (properties.hasUsername()) {
            builder.username(properties.username());
        }
        if (properties.hasPassword()) {
            builder.password(properties.password());
        }
        return builder.build();
    }

    @Bean(name = "bibleServiceFlywayMigrationInitializer")
    BibleServiceFlywayMigrationMarker bibleServiceFlywayMigrationInitializer(
            @Qualifier("bibleServiceDataSource") DataSource dataSource,
            BibleServicePersistenceProperties properties
    ) {
        if (properties.flywayEnabled()) {
            String locations = properties.flywayLocationsOrDefault();
            try {
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations(locations)
                        .load()
                        .migrate();
            } catch (FlywayException exception) {
                log.error(
                        "Bible service Flyway migration failed. locations={}, exceptionType={}",
                        locations,
                        exception.getClass().getName(),
                        exception
                );
                throw exception;
            }
        }
        return new BibleServiceFlywayMigrationMarker();
    }

    @Bean(name = "bibleServiceEntityManagerFactory")
    @DependsOn("bibleServiceFlywayMigrationInitializer")
    LocalContainerEntityManagerFactoryBean bibleServiceEntityManagerFactory(
            @Qualifier("bibleServiceDataSource") DataSource dataSource,
            BibleServicePersistenceProperties properties
    ) {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("com.qtai.domain.bible.internal");
        factory.setJpaPropertyMap(jpaProperties(properties));
        return factory;
    }

    @Bean(name = "bibleServiceTransactionManager")
    PlatformTransactionManager bibleServiceTransactionManager(
            @Qualifier("bibleServiceEntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    private static Map<String, Object> jpaProperties(BibleServicePersistenceProperties properties) {
        Map<String, Object> result = new HashMap<>();
        result.put("hibernate.hbm2ddl.auto", properties.ddlAutoOrDefault());
        result.put("hibernate.show_sql", "false");
        result.put("hibernate.format_sql", "false");
        if (properties.hasDialect()) {
            result.put("hibernate.dialect", properties.dialect());
        }
        return result;
    }
}

record BibleServiceFlywayMigrationMarker() {
}
