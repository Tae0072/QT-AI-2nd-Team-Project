package com.qtai.common.security;

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
 * {@link SystemTokenProvider}·{@link SystemTokenValidator} 단위 테스트 (HS256 공유 시크릿).
 *
 * <p>발급→검증 라운드트립과 실패(위조·만료·잘못된 타입) 분기를 검증한다. 시크릿은 테스트에서만 쓰는
 * 더미 값이며, HS256은 256비트(32바이트) 이상을 요구한다.
 */
class SystemTokenTest {

    // 테스트 전용 더미 시크릿(32바이트 이상). 운영 시크릿은 env로만 주입하며 저장소에 두지 않는다.
    private static final String SECRET = "qtai-system-batch-shared-secret-0123456789";
    private static final String OTHER_SECRET = "completely-different-secret-key-9876543210";

    private final SystemTokenProvider provider = new SystemTokenProvider(SECRET, 60_000L);
    private final SystemTokenValidator validator = new SystemTokenValidator(SECRET);

    @Test
    @DisplayName("발급한 시스템 토큰은 검증을 통과하고 시스템 주체 memberId(0)를 반환한다")
    void issue_then_validate_roundtrip() {
        String token = provider.issueSystemToken();

        long memberId = validator.validateAndGetSystemMemberId(token);

        assertThat(memberId).isEqualTo(0L);
        assertThat(validator.systemRole()).isEqualTo("SYSTEM_BATCH");
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 검증에서 거부된다")
    void rejects_token_signed_with_other_secret() {
        SecretKey otherKey = Keys.hmacShaKeyFor(OTHER_SECRET.getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .subject("0").claim("role", "SYSTEM_BATCH").claim("type", "system")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(otherKey, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> validator.validateAndGetSystemMemberId(forged))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("type이 system이 아닌 토큰은 거부된다(사용자/refresh 토큰 우회 차단)")
    void rejects_non_system_type() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String notSystem = Jwts.builder()
                .subject("0").claim("role", "SYSTEM_BATCH").claim("type", "access")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> validator.validateAndGetSystemMemberId(notSystem))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("시스템 토큰이 아닙니다");
    }

    @Test
    @DisplayName("role이 SYSTEM_BATCH가 아닌 토큰은 거부된다")
    void rejects_non_system_role() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String wrongRole = Jwts.builder()
                .subject("0").claim("role", "ADMIN").claim("type", "system")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> validator.validateAndGetSystemMemberId(wrongRole))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("SYSTEM_BATCH");
    }

    @Test
    @DisplayName("만료된 토큰은 거부된다(단명 토큰)")
    void rejects_expired_token() {
        // 만료 시각을 과거로 둔 발급기 → 즉시 만료된 토큰.
        SystemTokenProvider expiredProvider = new SystemTokenProvider(SECRET, -1_000L);
        String expired = expiredProvider.issueSystemToken();

        assertThatThrownBy(() -> validator.validateAndGetSystemMemberId(expired))
                .isInstanceOf(JwtException.class);
    }
}
