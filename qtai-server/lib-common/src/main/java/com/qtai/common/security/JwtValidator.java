package com.qtai.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * JWT 검증 전용 컴포넌트 (RS256 공개키만 사용).
 *
 * <p>MSA에서 토큰 <b>발급</b>은 service-user(개인키 보유)만 담당하고,
 * 나머지 서비스는 공개키로 <b>검증만</b> 한다. 따라서 이 클래스는 공개키만 받는다.
 *
 * <p>{@code security.jwt.public-key}가 설정된 경우에만 빈으로 등록된다
 * (스켈레톤/테스트에서 키 없이 부팅 가능).
 *
 * <p>로그에 token 값 절대 남기지 않는다 (CLAUDE.md §9).
 */
@Component
@ConditionalOnProperty(prefix = "security.jwt", name = "public-key")
public class JwtValidator {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final PublicKey publicKey;

    public JwtValidator(@Value("${security.jwt.public-key}") String publicKeyBase64) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            byte[] publicBytes = Base64.getDecoder().decode(publicKeyBase64);
            this.publicKey = kf.generatePublic(new X509EncodedKeySpec(publicBytes));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 공개키 초기화 실패 — security.jwt.public-key 설정을 확인하세요.", e);
        }
    }

    /**
     * 토큰 검증 후 memberId 반환. Refresh Token을 인증에 쓰는 우회를 차단한다.
     *
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public Long validateAndGetMemberId(String token) {
        Claims claims = parseClaims(token);
        if (TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("Refresh Token은 인증에 사용할 수 없습니다.");
        }
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("토큰 subject가 유효한 회원 ID가 아닙니다.");
        }
    }

    /** 토큰에서 role claim 추출. */
    public String extractRole(String token) {
        String role = parseClaims(token).get(CLAIM_ROLE, String.class);
        if (role == null) {
            throw new JwtException("토큰에 role claim이 없습니다.");
        }
        return role;
    }

    private Claims parseClaims(String token) {
        // 로그에 token 값을 남기지 않는다 (CLAUDE.md §9)
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
