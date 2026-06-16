package com.qtai.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * 공개 인증 경로 rate limit 설정 (코드리뷰 TODO 1, P2).
 *
 * <p>yml 예시:
 * <pre>
 * security:
 *   rate-limit:
 *     enabled: true
 *     trust-forwarded-for: false   # nginx gateway 뒤에 설 때만 true (Lead gateway PR과 조율)
 *     rules:
 *       - path: /api/v1/auth/kakao
 *         limit-per-minute: 10
 * </pre>
 *
 * <p>{@code trust-forwarded-for}가 false면 {@code X-Forwarded-For}를 무시하고 remoteAddr만 쓴다 —
 * 직접 노출 환경에서 클라이언트가 헤더를 위조해 한도를 우회하는 것을 막기 위함이다.
 */
@ConfigurationProperties(prefix = "security.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("false") boolean trustForwardedFor,
        List<Rule> rules
) {
    public RateLimitProperties {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    /** 경로(정확 일치)별 분당 허용 횟수. */
    public record Rule(String path, int limitPerMinute) {
    }
}
