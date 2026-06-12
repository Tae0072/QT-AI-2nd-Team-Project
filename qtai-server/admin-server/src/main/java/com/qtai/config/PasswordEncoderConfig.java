package com.qtai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 해시 인코더 설정.
 *
 * <p>관리자 자체 아이디 로그인의 비밀번호 검증에 사용한다. BCrypt 단방향 해시만 저장하며
 * 평문 비밀번호는 저장·로그에 남기지 않는다(CLAUDE.md §8/§9).
 *
 * <p>보안 설정과 독립적으로 항상 등록되도록 별도 설정에 둔다.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
