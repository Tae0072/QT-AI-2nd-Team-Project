package com.qtai.security;

/**
 * JWT 발급·검증·갱신 유틸.
 *
 * Access Token: 짧은 만료 (예: 30분)
 * Refresh Token: 긴 만료 (예: 30일), Redis 에 저장해 무효화 지원
 *
 * 로그에 token 값 절대 남기지 않는다 (CLAUDE.md §9).
 * Redis key 구조 (예): "refresh:{memberId}" → refresh token 값
 */
// TODO: @Component @RequiredArgsConstructor
public class JwtProvider {

    // TODO: @Value("${security.jwt.secret}") String secretKey;
    // TODO: @Value("${security.jwt.access-expiry-ms}") long accessExpiryMs;
    // TODO: @Value("${security.jwt.refresh-expiry-ms}") long refreshExpiryMs;
    // TODO: final RedisTemplate<String, String> redisTemplate;   (token/rate/idempotency 용)

    /** Access Token 발급 */
    // TODO: public String issueAccessToken(Long memberId, String role) { ... }

    /** Refresh Token 발급 + Redis 저장 */
    // TODO: public String issueRefreshToken(Long memberId) { ... }

    /** 토큰 검증 → 유효하면 memberId 반환, 무효면 예외 */
    // TODO: public Long validateAndGetMemberId(String token) { ... }

    /** Refresh Token 으로 Access Token 재발급 (소프트 로그인 포함) */
    // TODO: public String reissueAccessToken(String refreshToken) { ... }

    /** Refresh Token 무효화 (로그아웃·탈퇴 시) */
    // TODO: public void invalidateRefreshToken(Long memberId) { ... }
}
