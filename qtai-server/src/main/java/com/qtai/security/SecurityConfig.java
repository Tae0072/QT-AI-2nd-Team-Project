package com.qtai.security;

/**
 * Spring Security 설정.
 *
 * 정책 (역할 분담 회의록 합의 + CLAUDE.md §5):
 * - 모든 기능 화면은 로그인 후 접근 (비로그인 본문 열람 미지원)
 * - 인증되지 않은 사용자는 카카오 로그인 시작만 가능
 * - 서버사이드 /oauth2/** 경로 사용하지 않음
 *   Flutter SDK 가 카카오 토큰을 직접 받아 POST /api/v1/auth/kakao 로 전달
 * - 관리자 API 는 ADMIN role + admin_users.admin_role 모두 확인
 * - SYSTEM_BATCH 주체: 배치·AI 내부 작업용 (사용자 계정 사용 안 함)
 */
// TODO: @Configuration @EnableWebSecurity @RequiredArgsConstructor
public class SecurityConfig {

    // TODO: final JwtAuthenticationFilter jwtAuthenticationFilter;

    // TODO: @Bean
    //        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    //            http
    //              .csrf(csrf -> csrf.disable())
    //              .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
    //              .authorizeHttpRequests(auth -> auth
    //                  .requestMatchers(HttpMethod.POST, "/api/v1/auth/kakao").permitAll()
    //                  .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
    //                  .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    //                  .anyRequest().authenticated()
    //              )
    //              .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    //            return http.build();
    //        }

    // TODO: @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
