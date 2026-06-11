package com.qtai.ai;

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
 * service-ai 보안 설정.
 *
 * <p>AI 서비스는 무상태(STATELESS)로 운영하며, 컨트롤러의 메서드 보안({@code @PreAuthorize})을
 * 활성화한다. JWT 검증 필터가 존재하면(= {@code security.jwt.public-key} 설정 시) 체인에 끼운다.
 *
 * <p>경로별 정책(CLAUDE.md §5):
 * <ul>
 *   <li>{@code /actuator/health}, {@code /actuator/info} — permitAll</li>
 *   <li>{@code /api/v1/admin/**} — denyAll. 관리자 AI 화면(asset 검수·체크리스트 등)은
 *       admin_role 이중검증을 포함해 admin-server가 제공하므로 service-ai에서는 열지 않는다.</li>
 *   <li>{@code /api/v1/system/**} — 인증 필요. 시스템/배치 작업은 SYSTEM_BATCH 주체로 기록되며
 *       호출자 인증을 요구한다(상세 주체 검증은 컨트롤러 레이어 책임).</li>
 *   <li>{@code /api/v1/ai/**}(F-15 단발 Q&A 등) 및 그 외 — 인증 필요(authenticated)</li>
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
                        .requestMatchers("/api/v1/admin/**").denyAll()
                        .anyRequest().authenticated());

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }
}
