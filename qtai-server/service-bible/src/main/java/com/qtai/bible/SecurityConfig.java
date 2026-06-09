package com.qtai.bible;

import com.qtai.common.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * service-bible 보안 설정.
 *
 * <p>읽기전용 콘텐츠 서비스라 무상태(STATELESS)로 운영하며, 컨트롤러의 메서드 보안
 * ({@code @PreAuthorize})을 활성화한다. JWT 검증 필터가 존재하면(= {@code security.jwt.public-key}
 * 설정 시) 체인에 끼운다.
 *
 * <p>경로별 정책(CLAUDE.md §5):
 * <ul>
 *   <li>{@code /actuator/health}, {@code /actuator/info} — permitAll</li>
 *   <li>{@code /api/v1/admin/**} — ROLE_ADMIN (세부 admin_role 검사는 컨트롤러 메서드 보안 + admin 도메인 연동 시 강화)</li>
 *   <li>그 외 모든 요청 — 인증 필요(authenticated)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilterProvider.getIfAvailable();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated());

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }
}
