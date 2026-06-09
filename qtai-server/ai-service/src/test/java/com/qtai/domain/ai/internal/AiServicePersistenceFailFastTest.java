package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.qtai.ai.AiServicePersistenceConfiguration;

class AiServicePersistenceFailFastTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PersistenceFailFastTestConfig.class)
            .withPropertyValues(
                    "spring.profiles.active=test",
                    "qtai.ai.persistence.enabled=true"
            );

    @Test
    void persistenceModeFailsFastWhenDatabaseUrlIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootCause(context.getStartupFailure()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("qtai.ai.persistence.url");
        });
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    @Configuration
    @Import(AiServicePersistenceConfiguration.class)
    static class PersistenceFailFastTestConfig {
    }
}
