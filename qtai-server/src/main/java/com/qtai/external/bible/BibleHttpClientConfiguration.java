package com.qtai.external.bible;

import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * bible HTTP 어댑터 구성 — {@code qtai.bible.client.mode=http}일 때만 활성(기본 inprocess=동작 무변경).
 *
 * <p>활성 시 {@link ListBibleBooksUseCase}/{@link GetBibleVerseUseCase}의 HTTP 어댑터를 {@code @Primary}로
 * 등록해, 모놀리식 소비자(qt/note/study)의 in-process {@code BibleService} 호출을 bible-service HTTP로 우회한다.
 * 소비자 코드(인터페이스 의존)는 변경하지 않는다(Strangler).
 */
@Configuration
@ConditionalOnProperty(name = "qtai.bible.client.mode", havingValue = "http")
@EnableConfigurationProperties(BibleClientProperties.class)
public class BibleHttpClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BibleHttpClientConfiguration.class);

    @Bean
    CircuitBreaker bibleServiceCircuitBreaker(BibleClientProperties properties) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.cbFailureRateThresholdOrDefault())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(properties.cbSlidingWindowSizeOrDefault())
                .minimumNumberOfCalls(properties.cbMinimumCallsOrDefault())
                .waitDurationInOpenState(Duration.ofSeconds(properties.cbWaitDurationSecondsOrDefault()))
                // 일시 장애(재시도 소진 = EXTERNAL_API_FAILURE)만 실패로 기록 — 4xx 역매핑은 무시.
                .recordException(e -> e instanceof BusinessException be
                        && be.getErrorCode() == ErrorCode.EXTERNAL_API_FAILURE)
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("bibleService", config);
        // 상태 전이(CLOSED↔OPEN↔HALF_OPEN) 관측 — 장애 격리 발동/복구 가시화.
        circuitBreaker.getEventPublisher().onStateTransition(event -> log.info(
                "bible-service CircuitBreaker 상태 전이: {} → {}",
                event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
        return circuitBreaker;
    }

    @Bean
    BibleServiceClient bibleServiceClient(BibleClientProperties properties, ObjectMapper objectMapper,
                                          CircuitBreaker bibleServiceCircuitBreaker) {
        if (!StringUtils.hasText(properties.gatewayToken())) {
            throw new IllegalStateException(
                    "qtai.bible.client.gateway-token must be configured when qtai.bible.client.mode=http "
                            + "(SYSTEM 호출 인증 토큰).");
        }
        Duration timeout = Duration.ofMillis(properties.timeoutMsOrDefault());
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(timeout)
                .withReadTimeout(timeout);
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.requireBaseUrl())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
        return new BibleServiceClient(restClient, properties.gatewayToken(), objectMapper,
                properties.retryMaxAttemptsOrDefault(), properties.retryBackoffMsOrDefault(),
                bibleServiceCircuitBreaker);
    }

    @Bean
    @Primary
    ListBibleBooksUseCase listBibleBooksHttpAdapter(BibleServiceClient client) {
        return new ListBibleBooksHttpAdapter(client);
    }

    @Bean
    @Primary
    GetBibleVerseUseCase getBibleVerseHttpAdapter(BibleServiceClient client) {
        return new GetBibleVerseHttpAdapter(client);
    }
}
