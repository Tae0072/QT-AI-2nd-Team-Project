package com.qtai.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link SystemTokenValidator} 단위 테스트 (HS256 공유 시크릿).
 *
 * <p>다른 서비스의 lib-common {@code SystemTokenProvider}가 발급하는 토큰과 동일 규약(type=system,
 * role=SYSTEM_BATCH)을 admin-server가 검증하는지 확인한다. 시크릿은 테스트 전용 더미 값(실제 시크릿 아님).
 */
class SystemTokenValidatorTest {

    private static final String SECRET = "qtai-system-batch-shared-secret-0123456789";
    private static final String OTHER_SECRET = "completely-different-secret-key-9876543210";

    private final SystemTokenValidator validator = new SystemTokenValidator(SECRET);

    private static String token(String secret, String type, String role) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("0").claim("role", role).claim("type", type)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    @Test
    @DisplayName("유효한 시스템 토큰은 시스템 주체 memberId(0)를 반환한다")
    void valid_system_token() {
        long memberId = validator.validateAndGetSystemMemberId(token(SECRET, "system", "SYSTEM_BATCH"));

        assertThat(memberId).isEqualTo(0L);
        assertThat(validator.systemRole()).isEqualTo("SYSTEM_BATCH");
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 거부된다")
    void rejects_other_secret() {
        assertThatThrownBy(() ->
                validator.validateAndGetSystemMemberId(token(OTHER_SECRET, "system", "SYSTEM_BATCH")))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("type이 system이 아니면 거부된다")
    void rejects_non_system_type() {
        assertThatThrownBy(() ->
                validator.validateAndGetSystemMemberId(token(SECRET, "access", "SYSTEM_BATCH")))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("시스템 토큰이 아닙니다");
    }

    @Test
    @DisplayName("role이 SYSTEM_BATCH가 아니면 거부된다")
    void rejects_non_system_role() {
        assertThatThrownBy(() ->
                validator.validateAndGetSystemMemberId(token(SECRET, "system", "ADMIN")))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("SYSTEM_BATCH");
    }
}
