package com.qtai.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 개발(dev) 프로파일 전용 Spring Security 설정.
 *
 * 목적: 정식 JWT 인증 필터(SecurityConfig — 이승욱 작업)가 완성되기 전까지
 *      자유 노트 도메인 개발·테스트를 위한 임시 인증 우회.
 *
 * 정책:
 * - dev 프로파일에서만 활성화
 * - 모든 요청 permitAll (인증 완전 비활성화)
 * - CSRF disable (REST API라 불필요)
 * - 세션 STATELESS (JWT 환경 가정)
 *
 * 운영(prod) 환경에는 영향 없음. 정식 SecurityConfig 완성 후에도 이 파일은 유지.
 */
@Configuration
@EnableWebSecurity
@Profile("dev")
public class DevSecurityConfig {

    @Bean
    SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF: REST API는 토큰 기반 인증이라 폼 위조 방지 불필요
                .csrf(csrf -> csrf.disable())
                // 세션: JWT 환경 가정. 서버에 세션 안 만듦
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // dev 환경 핵심: 모든 요청 인증 없이 통과
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .build();
    }
}
