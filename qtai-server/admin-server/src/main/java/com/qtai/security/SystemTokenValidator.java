package com.qtai.security;

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
 * <p>admin-server는 lib-common 비의존 standalone(모놀리식 통째 복사)이라 lib-common의
 * {@code SystemTokenValidator}를 직접 쓸 수 없다. 다른 서비스(service-ai 등)가 lib-common
 * {@code SystemTokenProvider}로 발급하는 단명 시스템 토큰을 admin-server도 동일 규약(HS256 공유 시크릿,
 * {@code type=system}, {@code role=SYSTEM_BATCH})으로 검증하도록 같은 로직을 admin-server 보안 패키지에 둔다.
 * 발급기(SystemTokenProvider)는 admin-server에 두지 않는다 — admin-server는 시스템 토큰을 받기만 한다.
 *
 * <p>{@link JwtAuthenticationFilter}가 RS256 사용자 토큰 검증 실패 시 이 검증기로 폴백한다.
 * {@code security.jwt.system-secret}이 설정된 경우에만 빈으로 등록된다. 시크릿은 env로만 주입하고
 * 토큰 값·시크릿을 로그에 남기지 않는다(CLAUDE.md §7·§9). HS256 시크릿은 256비트(32바이트) 이상이어야 한다.
 */
@Component
@ConditionalOnProperty(prefix = "security.jwt", name = "system-secret")
public class SystemTokenValidator {

    /** 시스템 토큰 주체 memberId(0) — 사용자가 아니므로 0으로 고정. */
    public static final long SYSTEM_MEMBER_ID = 0L;
    /** 시스템 토큰 role claim 값. */
    public static final String ROLE = "SYSTEM_BATCH";

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_SYSTEM = "system";

    private final SecretKey secretKey;

    public SystemTokenValidator(@Value("${security.jwt.system-secret}") String systemSecret) {
        this.secretKey = Keys.hmacShaKeyFor(systemSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 시스템 토큰을 검증하고 시스템 주체 memberId(0)를 반환한다.
     *
     * @return {@link #SYSTEM_MEMBER_ID}
     * @throws JwtException 서명 불일치·만료·시스템 토큰이 아닌 경우
     */
    public long validateAndGetSystemMemberId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!TOKEN_TYPE_SYSTEM.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("시스템 토큰이 아닙니다.");
        }
        if (!ROLE.equals(claims.get(CLAIM_ROLE, String.class))) {
            throw new JwtException("시스템 토큰 role이 SYSTEM_BATCH가 아닙니다.");
        }
        return SYSTEM_MEMBER_ID;
    }

    /** 시스템 토큰 주체의 role 상수. */
    public String systemRole() {
        return ROLE;
    }
}
