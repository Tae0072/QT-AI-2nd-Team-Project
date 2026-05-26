package com.qtai.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
 * <p>permitAll 경로:
 * <ul>
 *   <li>POST /api/v1/auth/kakao — 카카오 로그인 (F-01)</li>
 *   <li>POST /api/v1/auth/refresh — Access Token 재발급</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!dev")
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API — CSRF 불필요, 세션 미사용
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 허용 (CLAUDE.md §5)
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/kakao").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()

                        // 관리자 API — ADMIN role 필요 (admin_role 세부 권한은 서비스 레이어에서 추가 검증)
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // 나머지 모든 API — 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
