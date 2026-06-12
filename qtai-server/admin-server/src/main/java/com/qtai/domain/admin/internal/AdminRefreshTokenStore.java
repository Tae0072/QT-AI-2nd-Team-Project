package com.qtai.domain.admin.internal;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 관리자 Refresh Token 회전·무효화 저장소(Redis).
 *
 * <p>memberId 단위로 <b>현재 유효한 refresh token 1개</b>만 보관한다. 토큰 갱신 시 새 토큰을 저장해
 * 기존 토큰을 자동 무효화(회전)하며, 사용된(또는 탈취된) 옛 토큰은 더 이상 일치하지 않아 거부된다.
 *
 * <p>key: {@code admin:refresh:{memberId}}. TTL은 refresh token 만료와 동일. 로그에 토큰 값을 남기지 않는다(§9).
 */
@Slf4j
@Component
public class AdminRefreshTokenStore {

    private static final String KEY_PREFIX = "admin:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public AdminRefreshTokenStore(
            StringRedisTemplate redisTemplate,
            @Value("${security.jwt.refresh-expiry-ms}") long refreshExpiryMs) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMillis(refreshExpiryMs);
    }

    /** 현재 유효한 refresh token으로 저장(회전: 기존 토큰 덮어쓰기). */
    public void save(Long memberId, String refreshToken) {
        redisTemplate.opsForValue().set(KEY_PREFIX + memberId, refreshToken, ttl);
        log.debug("관리자 refresh token 저장(회전) — memberId={}", memberId);
    }

    /** 제시된 토큰이 저장된 현재 토큰과 일치하는지(회전·재사용 검증). */
    public boolean matches(Long memberId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + memberId);
        return stored != null && stored.equals(refreshToken);
    }

    /** 저장된 refresh token 삭제(로그아웃/탈퇴 등). */
    public void delete(Long memberId) {
        redisTemplate.delete(KEY_PREFIX + memberId);
    }
}
