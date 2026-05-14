package com.qtai.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 설정 (Redis-WS 또는 Redis-Cache 위에서 동작).
 *
 * <p>TODO(강태오): IP + userId 조합 keyResolver, AI 세션은 더 강한 제한 적용.
 */
@Configuration
public class RateLimitConfig {

    /**
     * 익명 사용자는 IP, 인증 사용자는 sub(claim)으로 키 발급.
     */
    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .defaultIfEmpty("anonymous");
    }

    /**
     * 기본 RateLimiter: 분당 60req, burst 100.
     */
    @Bean
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(60, 100);
    }
}
