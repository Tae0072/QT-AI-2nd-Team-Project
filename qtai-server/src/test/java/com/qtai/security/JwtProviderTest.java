package com.qtai.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtProvider 단위 테스트.
 *
 * 테스트용 RSA 키 쌍을 동적으로 생성하여 사용한다.
 */
class JwtProviderTest {

    private static JwtProvider jwtProvider;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        jwtProvider = new JwtProvider(privateKeyBase64, publicKeyBase64, 1800000L, 1209600000L);
    }

    // ── Access Token ──

    @Test
    void issueAccessToken_정상_발급_및_검증() {
        String token = jwtProvider.issueAccessToken(1L, "USER");

        Long memberId = jwtProvider.validateAndGetMemberId(token);
        String role = jwtProvider.extractRole(token);

        assertThat(memberId).isEqualTo(1L);
        assertThat(role).isEqualTo("USER");
    }

    @Test
    void issueAccessToken_ADMIN_role_검증() {
        String token = jwtProvider.issueAccessToken(99L, "ADMIN");

        assertThat(jwtProvider.validateAndGetMemberId(token)).isEqualTo(99L);
        assertThat(jwtProvider.extractRole(token)).isEqualTo("ADMIN");
    }

    // ── Refresh Token ──

    @Test
    void issueRefreshToken_정상_발급_및_검증() {
        String token = jwtProvider.issueRefreshToken(1L);

        Long memberId = jwtProvider.validateRefreshToken(token);

        assertThat(memberId).isEqualTo(1L);
    }

    // ── 교차 사용 차단 ──

    @Test
    void refreshToken을_accessToken_경로에_사용하면_예외() {
        String refreshToken = jwtProvider.issueRefreshToken(1L);

        assertThatThrownBy(() -> jwtProvider.validateAndGetMemberId(refreshToken))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Refresh Token은 인증에 사용할 수 없습니다");
    }

    @Test
    void accessToken을_refreshToken_경로에_사용하면_예외() {
        String accessToken = jwtProvider.issueAccessToken(1L, "USER");

        assertThatThrownBy(() -> jwtProvider.validateRefreshToken(accessToken))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Refresh Token이 아닙니다");
    }

    // ── 변조·만료 ──

    @Test
    void 변조된_토큰_검증_실패() {
        String token = jwtProvider.issueAccessToken(1L, "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtProvider.validateAndGetMemberId(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void 만료된_토큰_검증_실패() throws Exception {
        // 만료 시간 0ms로 설정한 JwtProvider 생성
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        String privKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String pubKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        JwtProvider expiredProvider = new JwtProvider(privKey, pubKey, 0L, 0L);
        String token = expiredProvider.issueAccessToken(1L, "USER");

        // 약간의 시간 경과 후 만료됨
        Thread.sleep(10);

        assertThatThrownBy(() -> expiredProvider.validateAndGetMemberId(token))
                .isInstanceOf(JwtException.class);
    }

    // ── role claim 누락 ──

    @Test
    void refreshToken에서_role_추출하면_예외() {
        String refreshToken = jwtProvider.issueRefreshToken(1L);

        assertThatThrownBy(() -> jwtProvider.extractRole(refreshToken))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("role claim이 없습니다");
    }
}
