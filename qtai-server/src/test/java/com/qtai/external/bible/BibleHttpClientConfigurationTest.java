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
        // 부팅 실패에 그치지 않고 원인(어느 설정 누락인지)까지 검증 — 가드가 의도한 이유로 막혔음을 보장.
        runner.withPropertyValues(
                        "qtai.bible.client.mode=http",
                        "qtai.bible.client.base-url=http://bible-service")
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("gateway-token"));
    }

    @Test
    void httpMode_missingBaseUrl_failsFast() {
        // 컷오버 오설정 가드: mode=http인데 base-url 미설정이면 부팅 시점에 fast-fail —
        // 잘못 전환된 환경이 조용히 잘못된 호출을 하지 않도록 막는다. 실패 원인까지 검증해 token 가드와 대칭.
        runner.withPropertyValues(
                        "qtai.bible.client.mode=http",
                        "qtai.bible.client.gateway-token=gw-test-token") // gitleaks:allow
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("base-url"));
    }
}
