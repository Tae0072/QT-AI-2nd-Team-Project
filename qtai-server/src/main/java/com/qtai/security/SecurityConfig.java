package com.qtai.security;

import java.util.Arrays;
import java.util.List;

import com.qtai.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * Spring Security 설정.
 *
 * <p>인증 정책 (CLAUDE.md §5, §1):
 * <ul>
 *   <li>서버사이드 /oauth2/** 경로 사용하지 않음</li>
 *   <li>Flutter SDK가 카카오 토큰을 직접 받아 POST /api/v1/auth/kakao 로 전달</li>
 *   <li>모든 기능 화면은 로그인 후 접근 (비로그인 본문 열람 미지원)</li>
 *   <li>관리자 API는 ADMIN role + admin_users.admin_role 모두 확인 (admin_role은 서비스 레이어에서 추가 검증)</li>
 *   <li>SYSTEM_BATCH 주체: 배치·AI 내부 작업용</li>
 * </ul>
 *
 * <p>활성 조건 (P1-7 프로파일 갭 수정):
 * {@code qtai.security.dev-bypass}가 false이거나 미설정일 때 활성.
 * dev 프로파일이라도 dev-bypass=false면 이 정식 체인이 켜져 JWT 인증이 동작한다
 * (기존 {@code @Profile("!dev")}는 dev+bypass=false 조합에서 시큐리티 체인이 0개가 되어
 * Boot 기본 폼로그인으로 폴백하던 갭이 있었다). dev+bypass=true일 때만 DevSecurityConfig가 켜진다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(name = "qtai.security.dev-bypass", havingValue = "false", matchIfMissing = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    /** 관리자 웹(별도 오리진) 등 CORS 허용 오리진. WebConfig 대신 시큐리티 필터 레벨에서 처리. */
    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API — CSRF 불필요, 세션 미사용
                .csrf(AbstractHttpConfigurer::disable)
                // CORS — 시큐리티 필터 레벨에서 활성화해야 preflight(OPTIONS)가 401이 되지 않는다.
                // (기존엔 MVC WebConfig의 CORS만 있어 시큐리티 필터 뒤라 preflight가 막혔다)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // H2 콘솔(local 전용)은 iframe 기반이라 같은 출처 iframe을 허용해야 흰 화면이 안 된다.
                // 운영(prod)은 H2 콘솔 엔드포인트가 없으므로 영향 없음.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                securityErrorResponseWriter.write(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                securityErrorResponseWriter.write(response, ErrorCode.FORBIDDEN)))

                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 허용 (CLAUDE.md §5)
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/kakao").permitAll()
                        // 웹 카카오 로그인(서버 OAuth, B안 · DRAFT) — 강사/Lead 검토 후 정식화
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/kakao/web").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        // H2 콘솔 (local 프로파일 전용 — prod엔 엔드포인트 없음)
                        .requestMatchers("/h2-console/**").permitAll()

                        // 시스템·배치 내부 API — 필터 레벨에서 ROLE_SYSTEM_BATCH 강제(컨트롤러 수동 검사에만 의존하지 않음)
                        .requestMatchers("/api/v1/system/**").hasRole("SYSTEM_BATCH")

                        // 관리자 API — ADMIN role 필요 (admin_role 세부 권한은 서비스 레이어에서 추가 검증)
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // 나머지 모든 API — 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
