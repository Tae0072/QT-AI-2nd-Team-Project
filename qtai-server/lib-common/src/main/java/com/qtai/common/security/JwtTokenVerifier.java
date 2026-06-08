package com.qtai.common.security;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * RS256 JWT 검증 유틸 (공개키 전용).
 *
 * <p>MSA에서 각 서비스/게이트웨이는 공개키로 토큰을 <b>검증만</b> 한다. 발급(개인키)은
 * 인증 서비스(mypage/member)에만 둔다. 토큰 구조는 발급자({@code com.qtai.security.JwtProvider})와
 * 동일하다: {@code sub=memberId}, {@code role} claim, {@code type=access|refresh}.
 *
 * <p>로그에 token 값을 남기지 않는다 (CLAUDE.md §9).
 */
public final class JwtTokenVerifier {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final PublicKey publicKey;

    public JwtTokenVerifier(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public JwtTokenVerifier(String publicKeyBase64) {
        this(parsePublicKey(publicKeyBase64));
    }

    /**
     * Access Token 검증 후 인증 주체를 반환한다.
     * 만료·변조·refresh 타입·필수 claim 누락 시 {@link JwtException}을 던진다.
     *
     * @param token Bearer 토큰(헤더 제거 후)
     */
    public AuthenticatedUser verifyAccessToken(String token) {
        Claims claims = parse(token);
        // Refresh Token을 인증 경로에 사용하는 권한 우회 차단
        if (TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("Refresh Token은 인증에 사용할 수 없습니다.");
        }
        long memberId;
        try {
            memberId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("토큰 subject가 유효한 회원 ID가 아닙니다.");
        }
        String role = claims.get(CLAIM_ROLE, String.class);
        if (role == null) {
            throw new JwtException("토큰에 role claim이 없습니다.");
        }
        return new AuthenticatedUser(memberId, role);
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static PublicKey parsePublicKey(String publicKeyBase64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(publicKeyBase64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 공개키 초기화 실패 — 공개키 설정을 확인하세요.", e);
        }
    }
}
