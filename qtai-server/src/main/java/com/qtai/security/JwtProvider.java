package com.qtai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 발급·검증 유틸 (RS256).
 *
 * <p>RS256(비대칭 키) 방식을 사용한다.
 * MSA 전환 시 공개키만 각 서비스에 배포하면 검증 가능하고,
 * 개인키는 인증 서비스에만 보관한다.
 *
 * <p>토큰 구조:
 * <ul>
 *   <li>Access Token : sub=memberId, role claim, 30분 만료</li>
 *   <li>Refresh Token : sub=memberId, 30일 만료 (무효화는 호출자에서 Redis 관리)</li>
 * </ul>
 *
 * <p>로그에 token 값 절대 남기지 않는다 (CLAUDE.md §9).
 */
@Slf4j
@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String CLAIM_TOKEN_TYPE = "type";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessExpiryMs;
    private final long refreshExpiryMs;

    public JwtProvider(
            @Value("${security.jwt.private-key}") String privateKeyBase64,
            @Value("${security.jwt.public-key}") String publicKeyBase64,
            @Value("${security.jwt.access-expiry-ms}") long accessExpiryMs,
            @Value("${security.jwt.refresh-expiry-ms}") long refreshExpiryMs
    ) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            byte[] privateBytes = Base64.getDecoder().decode(privateKeyBase64);
            byte[] publicBytes = Base64.getDecoder().decode(publicKeyBase64);
            this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
            this.publicKey = kf.generatePublic(new X509EncodedKeySpec(publicBytes));
        } catch (Exception e) {
            throw new IllegalStateException("JWT RSA 키 초기화 실패 — application.yml 키 설정을 확인하세요.", e);
        }
        this.accessExpiryMs = accessExpiryMs;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    /**
     * Access Token 발급.
     *
     * @param memberId 회원 PK
     * @param role     회원 권한 (예: "USER", "ADMIN")
     * @return RS256 서명된 JWT 문자열
     */
    public String issueAccessToken(Long memberId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiryMs))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Refresh Token 발급.
     * 무효화(로그아웃·탈퇴)는 호출자에서 Redis로 관리한다.
     *
     * @param memberId 회원 PK
     * @return RS256 서명된 JWT 문자열
     */
    public String issueRefreshToken(Long memberId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiryMs))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * 토큰 검증 후 memberId 반환.
     * 만료·변조된 토큰이면 {@link JwtException}을 던진다.
     *
     * @param token Bearer 토큰 (헤더 제거 후)
     * @return 회원 PK
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public Long validateAndGetMemberId(String token) {
        Claims claims = parseClaims(token);
        // Refresh Token을 Access Token 경로에 사용하는 권한 우회 차단
        if (TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("Refresh Token은 인증에 사용할 수 없습니다.");
        }
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("토큰 subject가 유효한 회원 ID가 아닙니다.");
        }
    }

    /**
     * 토큰에서 role claim 추출.
     *
     * @param token Bearer 토큰
     * @return role 문자열 (예: "USER", "ADMIN")
     */
    public String extractRole(String token) {
        String role = parseClaims(token).get(CLAIM_ROLE, String.class);
        if (role == null) {
            throw new JwtException("토큰에 role claim이 없습니다.");
        }
        return role;
    }

    /**
     * Refresh Token 유효성 확인.
     * type claim이 "refresh"인지 검증한다.
     *
     * @param token Refresh 토큰
     * @return memberId (유효한 경우)
     * @throws JwtException 유효하지 않은 경우
     */
    public Long validateRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("Refresh Token이 아닙니다.");
        }
        return Long.parseLong(claims.getSubject());
    }

    // -------------------------------------------------------------------------
    // private
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        // 로그에 token 값을 남기지 않는다 (CLAUDE.md §9)
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
