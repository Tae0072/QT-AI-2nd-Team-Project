package com.qtai.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Date;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenVerifierTest {

    private static PrivateKey privateKey;
    private static JwtTokenVerifier verifier;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPair pair = newRsaKeyPair();
        privateKey = pair.getPrivate();
        verifier = new JwtTokenVerifier(pair.getPublic());
    }

    private static KeyPair newRsaKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    /** 발급자(JwtProvider)와 동일 구조의 토큰을 테스트에서 직접 서명한다. */
    private static String sign(PrivateKey key, String type, Long sub, String role, long ttlMs) {
        Date now = new Date();
        JwtBuilder b = Jwts.builder()
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key, Jwts.SIG.RS256);
        if (sub != null) {
            b.subject(String.valueOf(sub));
        }
        if (role != null) {
            b.claim("role", role);
        }
        return b.compact();
    }

    @Test
    @DisplayName("유효한 access 토큰 → memberId·role 반환")
    void verifiesValidAccessToken() {
        String token = sign(privateKey, "access", 42L, "USER", 60_000);

        AuthenticatedUser user = verifier.verifyAccessToken(token);

        assertThat(user.memberId()).isEqualTo(42L);
        assertThat(user.role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("refresh 토큰을 access 경로에 쓰면 거부")
    void rejectsRefreshToken() {
        String token = sign(privateKey, "refresh", 42L, null, 60_000);

        assertThatThrownBy(() -> verifier.verifyAccessToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("만료된 토큰 → 거부")
    void rejectsExpiredToken() {
        String token = sign(privateKey, "access", 42L, "USER", -1_000);

        assertThatThrownBy(() -> verifier.verifyAccessToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰 → 거부(서명 불일치)")
    void rejectsTokenSignedByOtherKey() throws Exception {
        PrivateKey otherKey = newRsaKeyPair().getPrivate();
        String token = sign(otherKey, "access", 42L, "USER", 60_000);

        assertThatThrownBy(() -> verifier.verifyAccessToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("role claim 없는 access 토큰 → 거부")
    void rejectsTokenWithoutRole() {
        String token = sign(privateKey, "access", 42L, null, 60_000);

        assertThatThrownBy(() -> verifier.verifyAccessToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("subject(sub) 없는 access 토큰 → 거부(NPE 누출 방지 가드)")
    void rejectsTokenWithoutSubject() {
        String token = sign(privateKey, "access", null, "USER", 60_000);

        assertThatThrownBy(() -> verifier.verifyAccessToken(token))
                .isInstanceOf(JwtException.class);
    }
}
