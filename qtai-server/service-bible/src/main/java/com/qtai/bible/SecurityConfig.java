package com.qtai.bible;

import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.JwtAuthenticationFilter;
import com.qtai.common.security.SecurityErrorResponseWriter;
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
 *   <li>{@code /api/v1/admin/**} — denyAll. 관리자 기능(admin_role 이중검증 포함)은
 *       콘텐츠 서비스가 아니라 admin-server가 제공한다. ROLE_ADMIN 단독 허용 시
 *       admin_users.admin_role 검증 없이 열리는 우회를 막기 위해 이 서비스에서는 차단한다.</li>
 *   <li>그 외 모든 요청 — 인증 필요(authenticated)</li>
 * </ul>
 *
 * <p>예외 처리(PR #2 이월): 미인증은 401, 인가 실패(denyAll 포함)는 403을 표준
 * {@link com.qtai.common.dto.ApiResponse} 형식으로 반환한다. 기본 entry point는 콘텐츠
 * 누출 없이 403만 던져 모놀리식과 응답 계약이 어긋났는데, lib-common의
 * {@link SecurityErrorResponseWriter}로 통일한다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    public SecurityConfig(ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider,
                          SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilterProvider.getIfAvailable();
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/admin/**").denyAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                securityErrorResponseWriter.write(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, deniedException) ->
                                securityErrorResponseWriter.write(response, ErrorCode.FORBIDDEN)));

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }
}
