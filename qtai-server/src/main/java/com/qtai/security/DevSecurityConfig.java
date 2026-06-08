package com.qtai.security;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 개발(dev) 프로파일 전용 Spring Security 설정.
 *
 * 목적: 정식 JWT 인증 필터(SecurityConfig — 이승욱 작업)가 완성되기 전까지
 *      자유 노트 도메인 개발·테스트를 위한 임시 인증 우회.
 *
 * 운영 사고 방지를 위한 3중 가드:
 *   1. @Profile("dev")                                  — prod·default 프로파일에서는 빈 자체가 생성되지 않음
 *   2. @ConditionalOnProperty("qtai.security.dev-bypass") — 명시적 토글. application-prod.yml에서 false 강제
 *   3. 빈 초기화 시 Active Profiles에 "prod"가 있으면 즉시 IllegalStateException으로 부트 실패
 *
 * 사용:
 *   - application-dev.yml: qtai.security.dev-bypass: true
 *   - application-prod.yml: qtai.security.dev-bypass: false (강제)
 *
 * 정책:
 * - dev 프로파일 + 토글 ON 일 때만 활성화
 * - 모든 요청 permitAll (인증 완전 비활성화)
 * - CSRF disable (REST API라 불필요)
 * - 세션 STATELESS (JWT 환경 가정)
 * - 빈 초기화 시 명시적 경고 로그
 *
 * 운영(prod) 환경에는 영향 없음. 정식 SecurityConfig 완성 후에도 이 파일은 유지.
 */
@Configuration
@EnableWebSecurity
@Profile("dev")
@ConditionalOnProperty(name = "qtai.security.dev-bypass", havingValue = "true")
public class DevSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(DevSecurityConfig.class);

    @Bean
    SecurityFilterChain devFilterChain(HttpSecurity http,
                                       Environment env,
                                       DevUserIdHeaderFilter devUserIdHeaderFilter) throws Exception {
        // 3중 가드 — prod 프로파일이 활성 상태라면 즉시 부트 실패
        if (Arrays.asList(env.getActiveProfiles()).contains("prod")) {
            throw new IllegalStateException(
                    "DevSecurityConfig must NOT activate in 'prod' profile. " +
                    "Check spring.profiles.active and qtai.security.dev-bypass setting."
            );
        }
        log.warn("⚠️ DEV SECURITY BYPASS ENABLED — 모든 엔드포인트가 무인증 노출됩니다. 절대 운영 환경에서 활성화하지 마세요.");
        return http
                // CSRF: REST API는 토큰 기반 인증이라 폼 위조 방지 불필요
                .csrf(csrf -> csrf.disable())
                // [WEB_DEV] 웹(Flutter web) 출처에서 호출 가능하도록 CORS 허용 (dev-bypass 전용)
                .cors(cors -> cors.configurationSource(devCorsConfigurationSource()))
                // 세션: JWT 환경 가정. 서버에 세션 안 만듦
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // dev 환경 핵심: 모든 요청 인증 없이 통과
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                // H2 콘솔(/h2-console)은 iframe 기반이라, 기본값 X-Frame-Options: DENY면 흰 화면이 된다.
                // 같은 출처 iframe만 허용해 dev에서 H2 콘솔을 띄울 수 있게 한다. (dev 전용)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // X-Dev-User-Id 헤더 → SecurityContext memberId 주입. permitAll만으로는
                // @AuthenticationPrincipal에 null이 들어와 NoteController가 401을 던지므로 필수.
                .addFilterBefore(devUserIdHeaderFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * [WEB_DEV] dev-bypass 모드 전용 CORS 소스.
     *
     * <p>Flutter 웹앱(예: {@code http://localhost:3000})에서 dev 서버를 호출할 때
     * 브라우저 CORS preflight가 통과하도록 허용한다. dev 전용이므로 관대하게 둔다.
     * 운영(prod)에서는 이 설정(DevSecurityConfig) 자체가 로드되지 않는다.
     */
    private CorsConfigurationSource devCorsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-Dev-User-Id", "Authorization"));
        config.setMaxAge(3600L);
        config.setAllowCredentials(false); // dev-only; WARNING: bind dev server to 127.0.0.1 / internal network only, never expose externally - X-Dev-User-Id header can impersonate any member
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
