package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qtai.domain.ai.client.admin.AdminAuthClientHttpAdapter;
import com.qtai.domain.ai.client.audit.AuditLogClientHttpAdapter;
import com.qtai.domain.ai.client.bible.BibleVerseClientHttpAdapter;
import com.qtai.domain.ai.client.http.AiClientConfiguration;
import com.qtai.domain.ai.client.qt.QtContextClientHttpAdapter;
import com.qtai.domain.ai.client.study.StudyPublishClientHttpAdapter;

class AiServiceHttpModeFailFastTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(HttpModeFailFastTestConfig.class)
            .withPropertyValues("spring.profiles.active=test");

    @Test
    void httpModeFailsFastWhenServiceTokenIsMissing() {
        contextRunner
                .withPropertyValues(httpModePropertiesExcept("qtai.ai.client.service-token"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("service-token");
                });
    }

    @Test
    void httpModeFailsFastWhenBaseUrlIsMissing() {
        contextRunner
                .withPropertyValues(httpModePropertiesExcept("qtai.ai.client.bible.base-url"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("bible base-url");
                });
    }

    private static String[] httpModeProperties() {
        return new String[] {
                "qtai.ai.client.mode=http",
                "qtai.ai.client.service-token=test-service-token",
                "qtai.ai.client.qt.base-url=http://localhost:65530",
                "qtai.ai.client.bible.base-url=http://localhost:65531",
                "qtai.ai.client.study.base-url=http://localhost:65532",
                "qtai.ai.client.audit.base-url=http://localhost:65533",
                "qtai.ai.client.admin-auth.base-url=http://localhost:65534"
        };
    }

    private static String[] httpModePropertiesExcept(String excludedPropertyName) {
        return Arrays.stream(httpModeProperties())
                .filter(property -> !property.startsWith(excludedPropertyName + "="))
                .toArray(String[]::new);
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    @Configuration
    @Import({
            AiClientConfiguration.class,
            QtContextClientHttpAdapter.class,
            BibleVerseClientHttpAdapter.class,
            StudyPublishClientHttpAdapter.class,
            AuditLogClientHttpAdapter.class,
            AdminAuthClientHttpAdapter.class
    })
    static class HttpModeFailFastTestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }
}
