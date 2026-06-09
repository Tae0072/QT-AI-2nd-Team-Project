package com.qtai.user;

import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.JwtAuthenticationFilter;
import com.qtai.common.security.SecurityErrorResponseWriter;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * service-user 보안 설정. JWT 발급 주체이자 사용자 API(member/notification/mission)를 제공한다.
 *
 * <p>무상태(STATELESS) REST. 컨트롤러 메서드 보안({@code @PreAuthorize}) 활성화.
 * 토큰 <b>검증</b>은 lib-common의 {@link JwtAuthenticationFilter}(공개키, {@code security.jwt.public-key}
 * 설정 시 활성)가 담당하고, 토큰 <b>발급</b>은 service-user의 {@code JwtProvider}(개인키)만 담당한다.
 *
 * <p>경로별 정책(CLAUDE.md §5):
 * <ul>
 *   <li>{@code POST /api/v1/auth/kakao}, {@code POST /api/v1/auth/refresh} — permitAll(로그인/재발급, 비인증 허용)</li>
 *   <li>{@code /actuator/health}, {@code /actuator/info}, {@code /h2-console/**} — permitAll</li>
 *   <li>{@code /api/v1/system/**} — {@code ROLE_SYSTEM_BATCH} 강제(배치/AI 내부 작업 주체)</li>
 *   <li>{@code /api/v1/admin/**} — denyAll. 관리자 기능은 admin-server가 제공한다. ROLE_ADMIN 단독
 *       허용 시 {@code admin_users.admin_role} 이중검증 없이 열리는 우회를 막기 위해 이 서비스에서는 차단한다.</li>
 *   <li>그 외 모든 요청 — 인증 필요(authenticated)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    /** 관리자 웹(별도 오리진) 등 CORS 허용 오리진. */
    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    /** H2 콘솔 사용 여부. 콘솔을 켠 환경(로컬)에서만 {@code /h2-console/**}을 permitAll로 연다(운영 노출 차단). */
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    public SecurityConfig(ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider,
                          SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilterProvider.getIfAvailable();
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS는 시큐리티 필터 레벨에서 활성화해야 preflight(OPTIONS)가 401이 되지 않는다.
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // H2 콘솔(local 전용)은 iframe 기반이라 같은 출처 iframe을 허용한다(운영엔 엔드포인트 없음).
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                securityErrorResponseWriter.write(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                securityErrorResponseWriter.write(response, ErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                            // 인증 없이 허용 (CLAUDE.md §5: Flutter SDK가 카카오 토큰을 직접 받아 전달)
                            .requestMatchers(HttpMethod.POST, "/api/v1/auth/kakao").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll();
                    // H2 콘솔은 콘솔이 켜진 환경(로컬)에서만 연다 — 운영에서 실수로 활성화돼도 SecurityConfig가 막는다.
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    // 시스템·배치 내부 API — 필터 레벨에서 ROLE_SYSTEM_BATCH 강제
                    auth.requestMatchers("/api/v1/system/**").hasRole("SYSTEM_BATCH")
                            // 관리자 API — admin-server 책임. 사용자 서비스에서는 차단.
                            .requestMatchers("/api/v1/admin/**").denyAll()
                            .anyRequest().authenticated();
                });

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

    /** CORS 설정 소스 — {@code cors.allowed-origins} 콤마 구분 값으로 허용 오리진을 구성. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
