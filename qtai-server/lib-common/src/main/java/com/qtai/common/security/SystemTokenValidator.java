package com.qtai.common.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 서비스 간 시스템(배치·스케줄러) 토큰 검증기 (HS256 공유 시크릿).
 *
 * <p>공통 {@link JwtAuthenticationFilter}가 RS256 사용자 토큰 검증 실패 시 이 검증기로 폴백한다.
 * type=system, role=SYSTEM_BATCH인 유효한 토큰만 통과시켜 시스템 주체(memberId=0)를 반환한다.
 *
 * <p>{@code security.jwt.system-secret}이 설정된 경우에만 빈으로 등록된다. 시크릿은 env로만 주입하고
 * 토큰 값·시크릿을 로그에 남기지 않는다(CLAUDE.md §7·§9).
 */
@Component
@ConditionalOnProperty(prefix = "security.jwt", name = "system-secret")
public class SystemTokenValidator {

    private final SecretKey secretKey;

    public SystemTokenValidator(@Value("${security.jwt.system-secret}") String systemSecret) {
        this.secretKey = Keys.hmacShaKeyFor(systemSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 시스템 토큰을 검증하고 시스템 주체 memberId(0)를 반환한다.
     *
     * @return {@link SystemTokenClaims#SYSTEM_MEMBER_ID}
     * @throws JwtException 서명 불일치·만료·시스템 토큰이 아닌 경우
     */
    public long validateAndGetSystemMemberId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!SystemTokenClaims.TOKEN_TYPE_SYSTEM.equals(claims.get(SystemTokenClaims.CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("시스템 토큰이 아닙니다.");
        }
        if (!SystemTokenClaims.ROLE.equals(claims.get(SystemTokenClaims.CLAIM_ROLE, String.class))) {
            throw new JwtException("시스템 토큰 role이 SYSTEM_BATCH가 아닙니다.");
        }
        return SystemTokenClaims.SYSTEM_MEMBER_ID;
    }

    /** 시스템 토큰 주체의 role 상수. */
    public String systemRole() {
        return SystemTokenClaims.ROLE;
    }
}
