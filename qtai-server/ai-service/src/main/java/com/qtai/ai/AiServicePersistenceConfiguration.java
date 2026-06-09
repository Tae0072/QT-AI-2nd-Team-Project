package com.qtai.ai;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(name = "qtai.ai.persistence.enabled", havingValue = "true")
@EnableConfigurationProperties(AiServicePersistenceProperties.class)
@EntityScan("com.qtai.domain.ai.internal")
@EnableJpaAuditing
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.qtai.domain.ai.internal",
        entityManagerFactoryRef = "aiServiceEntityManagerFactory",
        transactionManagerRef = "aiServiceTransactionManager"
)
public class AiServicePersistenceConfiguration {

    @Bean(name = "aiServiceDataSource")
    DataSource aiServiceDataSource(AiServicePersistenceProperties properties) {
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

    @Bean(name = "aiServiceEntityManagerFactory")
    LocalContainerEntityManagerFactoryBean aiServiceEntityManagerFactory(
            @Qualifier("aiServiceDataSource") DataSource dataSource,
            AiServicePersistenceProperties properties
    ) {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("com.qtai.domain.ai.internal");
        factory.setJpaPropertyMap(jpaProperties(properties));
        return factory;
    }

    @Bean(name = "aiServiceTransactionManager")
    PlatformTransactionManager aiServiceTransactionManager(
            @Qualifier("aiServiceEntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    private static Map<String, Object> jpaProperties(AiServicePersistenceProperties properties) {
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
