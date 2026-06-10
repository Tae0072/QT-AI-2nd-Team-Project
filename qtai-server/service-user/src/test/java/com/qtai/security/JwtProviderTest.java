package com.qtai.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link JwtProvider} 단위 테스트 — 발급/검증 라운드트립과 토큰 타입 오용 차단.
 *
 * <p>평문 키를 저장소에 두지 않기 위해(CLAUDE.md §8) 테스트 시점에 RSA 키쌍을 생성한다.
 */
class JwtProviderTest {

    private static JwtProvider jwtProvider;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        jwtProvider = new JwtProvider(privateKey, publicKey, 1_800_000L, 1_209_600_000L);
    }

    @Test
    void accessToken_발급후_검증하면_memberId와_role을_복원한다() {
        String token = jwtProvider.issueAccessToken(42L, "USER");

        assertThat(jwtProvider.validateAndGetMemberId(token)).isEqualTo(42L);
        assertThat(jwtProvider.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void refreshToken_발급후_검증하면_memberId를_복원한다() {
        String refresh = jwtProvider.issueRefreshToken(7L);

        assertThat(jwtProvider.validateRefreshToken(refresh)).isEqualTo(7L);
    }

    @Test
    void refreshToken을_access경로에_쓰면_거부한다() {
        String refresh = jwtProvider.issueRefreshToken(7L);

        assertThatThrownBy(() -> jwtProvider.validateAndGetMemberId(refresh))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void accessToken을_refresh경로에_쓰면_거부한다() {
        String access = jwtProvider.issueAccessToken(7L, "USER");

        assertThatThrownBy(() -> jwtProvider.validateRefreshToken(access))
                .isInstanceOf(JwtException.class);
    }
}
