package com.qtai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * 운영 사고 방지를 위한 2중 가드:
 *   1. @Profile("dev")                                  — prod·default 프로파일에서는 빈 자체가 생성되지 않음
 *   2. @ConditionalOnProperty("qtai.security.dev-bypass") — 명시적 토글. 기본값 false
 *
 * 사용:
 *   - application-dev.yml: qtai.security.dev-bypass: true
 *   - application-prod.yml: qtai.security.dev-bypass: false (강제, 이승욱 인프라 영역에서 보강 예정)
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
    SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        log.warn("⚠️ DEV SECURITY BYPASS ENABLED — 모든 엔드포인트가 무인증 노출됩니다. 절대 운영 환경에서 활성화하지 마세요.");
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
