package com.qtai.ai;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Configuration
@ConditionalOnProperty(
        name = {"qtai.ai.inbound.enabled", "qtai.ai.persistence.enabled"},
        havingValue = "true"
)
@ComponentScan(
        basePackages = "com.qtai.domain.ai.internal",
        includeFilters = {
                @ComponentScan.Filter(Service.class),
                @ComponentScan.Filter(Repository.class)
        },
        useDefaultFilters = false
)
public class AiServiceUseCaseConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
