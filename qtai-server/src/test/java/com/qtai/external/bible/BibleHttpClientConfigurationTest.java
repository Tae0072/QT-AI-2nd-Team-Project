package com.qtai.external.bible;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BibleHttpClientConfiguration의 mode 게이팅·빈 등록 검증 (ApplicationContextRunner — 경량 컨텍스트).
 */
class BibleHttpClientConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withConfiguration(UserConfigurations.of(BibleHttpClientConfiguration.class));

    @Test
    void httpMode_registersHttpAdaptersAndClient() {
        runner.withPropertyValues(
                        "qtai.bible.client.mode=http",
                        "qtai.bible.client.base-url=http://bible-service",
                        "qtai.bible.client.gateway-token=gw-test-token") // gitleaks:allow
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(BibleServiceClient.class);
                    assertThat(context.getBean(ListBibleBooksUseCase.class))
                            .isInstanceOf(ListBibleBooksHttpAdapter.class);
                    assertThat(context.getBean(GetBibleVerseUseCase.class))
                            .isInstanceOf(GetBibleVerseHttpAdapter.class);
                });
    }

    @Test
    void inprocessMode_doesNotRegisterAdapters() {
        runner.withPropertyValues("qtai.bible.client.mode=inprocess")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(BibleServiceClient.class);
                    assertThat(context).doesNotHaveBean(ListBibleBooksHttpAdapter.class);
                    assertThat(context).doesNotHaveBean(GetBibleVerseHttpAdapter.class);
                });
    }

    @Test
    void httpMode_missingGatewayToken_failsFast() {
        runner.withPropertyValues(
                        "qtai.bible.client.mode=http",
                        "qtai.bible.client.base-url=http://bible-service")
                .run(context -> assertThat(context).hasFailed());
    }
}
