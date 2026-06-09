package com.qtai.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 서비스 간 시스템(배치·스케줄러) 호출용 단명 HS256 토큰 발급기.
 *
 * <p>배치/스케줄러 호출은 전달할 사용자 JWT가 없으므로, 모든 서비스가 공유하는 시크릿
 * ({@code security.jwt.system-secret})으로 HS256 단명 토큰을 발급한다(sub=0, role=SYSTEM_BATCH).
 * 사용자 토큰 발급(RS256, service-user)과는 분리된 경로다.
 *
 * <p>{@code security.jwt.system-secret}이 설정된 경우에만 빈으로 등록된다(미설정 환경 부팅 가능).
 * 시크릿은 env로만 주입하고 로그에 남기지 않는다(CLAUDE.md §7·§9). HS256 시크릿은 256비트(32바이트) 이상이어야 한다.
 */
@Component
@ConditionalOnProperty(prefix = "security.jwt", name = "system-secret")
public class SystemTokenProvider {

    private final SecretKey secretKey;
    private final long expiryMs;

    public SystemTokenProvider(
            @Value("${security.jwt.system-secret}") String systemSecret,
            @Value("${security.jwt.system-token-expiry-ms:60000}") long expiryMs
    ) {
        // 시크릿 값 자체는 로그/예외 메시지에 남기지 않는다.
        this.secretKey = Keys.hmacShaKeyFor(systemSecret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = expiryMs;
    }

    /**
     * 단명 SYSTEM_BATCH 토큰을 발급한다.
     *
     * @return HS256 서명된 JWT (sub=0, role=SYSTEM_BATCH, type=system)
     */
    public String issueSystemToken() {
        Date now = new Date();
        return Jwts.builder()
                .subject(SystemTokenClaims.SUBJECT)
                .claim(SystemTokenClaims.CLAIM_ROLE, SystemTokenClaims.ROLE)
                .claim(SystemTokenClaims.CLAIM_TOKEN_TYPE, SystemTokenClaims.TOKEN_TYPE_SYSTEM)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }
}
