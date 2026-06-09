package com.qtai.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** JwtValidator(검증 전용, 공개키) 단위 테스트. RSA 키쌍을 즉석 생성해 토큰을 발급/검증한다. */
class JwtValidatorTest {

    private static PrivateKey privateKey;
    private static String publicKeyBase64;

    private JwtValidator validator;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        privateKey = pair.getPrivate();
        publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    @BeforeEach
    void setUp() {
        validator = new JwtValidator(publicKeyBase64);
    }

    private String issue(String sub, String role, String type, long expiresInMs) {
        var builder = Jwts.builder()
                .subject(sub)
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiresInMs))
                .signWith(privateKey, Jwts.SIG.RS256);
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.compact();
    }

    @Test
    void 정상_access_토큰이면_memberId와_role을_반환() {
        String token = issue("42", "USER", "access", 60_000);

        assertEquals(42L, validator.validateAndGetMemberId(token));
        assertEquals("USER", validator.extractRole(token));
    }

    @Test
    void refresh_토큰은_인증에_사용할_수_없다() {
        String token = issue("42", "USER", "refresh", 60_000);

        assertThrows(JwtException.class, () -> validator.validateAndGetMemberId(token));
    }

    @Test
    void 만료된_토큰이면_예외() {
        String token = issue("42", "USER", "access", -1_000);

        assertThrows(JwtException.class, () -> validator.validateAndGetMemberId(token));
    }

    @Test
    void 변조된_토큰이면_예외() {
        String token = issue("42", "USER", "access", 60_000);
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThrows(JwtException.class, () -> validator.validateAndGetMemberId(tampered));
    }

    @Test
    void role_claim이_없으면_예외() {
        String token = issue("42", null, "access", 60_000);

        assertThrows(JwtException.class, () -> validator.extractRole(token));
    }
}
