package com.qtai.external.bible;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * bible 도메인 호출 방식 설정 (MSA Inc3 — Strangler 소비자 전환).
 *
 * <p>{@code mode=inprocess}(기본)면 모놀리식 in-process {@code BibleService}를 그대로 쓴다(동작 무변경).
 * {@code mode=http}면 {@code external/bible} HTTP 어댑터가 {@code @Primary}로 등록되어 bible-service를 호출한다.
 * 토큰은 env 주입(평문 키 금지) — bible-service의 SYSTEM 주체 인증({@code X-Gateway-Token})과 동일 값.
 */
@ConfigurationProperties(prefix = "qtai.bible.client")
public record BibleClientProperties(
        String mode,
        String baseUrl,
        String gatewayToken,
        Long timeoutMs,
        Integer retryMaxAttempts,
        Long retryBackoffMs,
        Float cbFailureRateThreshold,
        Integer cbSlidingWindowSize,
        Integer cbMinimumCalls,
        Long cbWaitDurationSeconds
) {

    public boolean isHttpMode() {
        return "http".equalsIgnoreCase(mode);
    }

    int retryMaxAttemptsOrDefault() {
        return retryMaxAttempts != null && retryMaxAttempts > 0 ? retryMaxAttempts : 3;
    }

    long retryBackoffMsOrDefault() {
        return retryBackoffMs != null && retryBackoffMs >= 0 ? retryBackoffMs : 100L;
    }

    float cbFailureRateThresholdOrDefault() {
        return cbFailureRateThreshold != null && cbFailureRateThreshold > 0 ? cbFailureRateThreshold : 50f;
    }

    int cbSlidingWindowSizeOrDefault() {
        return cbSlidingWindowSize != null && cbSlidingWindowSize > 0 ? cbSlidingWindowSize : 10;
    }

    int cbMinimumCallsOrDefault() {
        return cbMinimumCalls != null && cbMinimumCalls > 0 ? cbMinimumCalls : 5;
    }

    long cbWaitDurationSecondsOrDefault() {
        return cbWaitDurationSeconds != null && cbWaitDurationSeconds > 0 ? cbWaitDurationSeconds : 10L;
    }

    String requireBaseUrl() {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException(
                    "qtai.bible.client.base-url must be configured when qtai.bible.client.mode=http");
        }
        return baseUrl;
    }

    long timeoutMsOrDefault() {
        return timeoutMs != null && timeoutMs > 0 ? timeoutMs : 3000L;
    }
}
