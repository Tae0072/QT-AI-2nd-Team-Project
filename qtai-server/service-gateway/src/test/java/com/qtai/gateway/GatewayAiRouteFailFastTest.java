package com.qtai.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.qtai.gateway.config.AiServiceRouteConfiguration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GatewayAiRouteFailFastTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiServiceRouteConfiguration.class);

    @Test
    @DisplayName("ai-service route requires target URI when enabled")
    void enabledRouteRequiresTargetUri() {
        contextRunner
                .withPropertyValues("qtai.gateway.ai-service.route-enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("qtai.gateway.ai-service.uri");
                });
    }
}
