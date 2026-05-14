package com.qtai.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive 보안 설정.
 *
 * <p>비로그인 허용 경로: 오늘 QT 미리보기, 한/영 성경, 본문 설명, 인증 콜백, JWKS.
 * 그 외는 RS256 JWT를 요구한다 (DECISIONS.md §3).
 *
 * <p>TODO(강태오): Refresh blacklist 필터, Rate Limit (Redis), CORS 화이트리스트 추가.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        // 비로그인 허용
                        .pathMatchers("/.well-known/**", "/auth/**", "/actuator/**").permitAll()
                        .pathMatchers("/bible/**", "/api/v1/explanations/**").permitAll()
                        .pathMatchers("/api/v1/qt/today").permitAll()
                        .pathMatchers("/api/v1/shares", "/api/v1/shares/*/comments").permitAll()
                        // 그 외 인증 필수
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}))
                .build();
    }
}
