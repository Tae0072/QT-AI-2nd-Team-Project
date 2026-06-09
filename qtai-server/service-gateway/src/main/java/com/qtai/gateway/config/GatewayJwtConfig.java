package com.qtai.gateway.config;

import com.qtai.common.security.JwtTokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 게이트웨이 JWT 검증 설정.
 *
 * <p>게이트웨이는 access token을 <b>공개키로 검증만</b> 한다(발급=개인키는 인증 서비스에만,
 * CLAUDE.md §5). 공개키는 환경변수({@code JWT_PUBLIC_KEY})로 주입하며 저장소에 평문 키를 두지 않는다.
 */
@Configuration
public class GatewayJwtConfig {

    @Bean
    JwtTokenVerifier jwtTokenVerifier(@Value("${gateway.jwt.public-key}") String publicKeyBase64) {
        return new JwtTokenVerifier(publicKeyBase64);
    }
}
