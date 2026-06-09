package com.qtai.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link ServiceEndpointsProperties} 바인딩 검증.
 *
 * <p>{@link RestClientConfig}의 {@code @EnableConfigurationProperties}로 등록되며,
 * 기본값(로컬 포트)과 {@code qtai.services.*} 프로퍼티(=env) 오버라이드가 동작하는지 고정한다.
 */
class ServiceEndpointsPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(RestClientConfig.class);

    @Test
    @DisplayName("기본값은 로컬 멀티모듈 포트를 가리킨다")
    void 기본값() {
        runner.run(context -> {
            ServiceEndpointsProperties properties = context.getBean(ServiceEndpointsProperties.class);
            assertThat(properties.getUserBaseUrl()).isEqualTo("http://localhost:8081");
            assertThat(properties.getBibleBaseUrl()).isEqualTo("http://localhost:8082");
            assertThat(properties.getNoteBaseUrl()).isEqualTo("http://localhost:8083");
            assertThat(properties.getAiBaseUrl()).isEqualTo("http://localhost:8084");
            assertThat(properties.getAdminBaseUrl()).isEqualTo("http://localhost:8090");
        });
    }

    @Test
    @DisplayName("qtai.services.* 프로퍼티(env)로 base URL을 오버라이드한다")
    void 오버라이드() {
        runner.withPropertyValues("qtai.services.bible-base-url=http://service-bible:8082")
                .run(context -> {
                    ServiceEndpointsProperties properties = context.getBean(ServiceEndpointsProperties.class);
                    assertThat(properties.getBibleBaseUrl()).isEqualTo("http://service-bible:8082");
                });
    }
}
