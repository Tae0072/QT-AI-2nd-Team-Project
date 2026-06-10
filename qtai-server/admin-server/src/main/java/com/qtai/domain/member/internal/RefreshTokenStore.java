package com.qtai.domain.member.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 기반 Refresh Token 저장소.
 *
 * key: "refresh:{memberId}", value: refresh token 문자열.
 * TTL은 refresh token 만료 시간과 동일하게 설정한다.
 *
 * 로그에 토큰 값을 남기지 않는다 (CLAUDE.md 9).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";
    private final StringRedisTemplate redisTemplate;

    /**
     * Refresh token을 저장한다.
     *
     * @param memberId 회원 PK
     * @param refreshToken 저장할 refresh token
     * @param ttl 만료까지 남은 시간
     */
    public void save(Long memberId, String refreshToken, Duration ttl) {
        String key = KEY_PREFIX + memberId;
        redisTemplate.opsForValue().set(key, refreshToken, ttl);
        log.debug("Refresh token 저장 완료: memberId={}", memberId);
    }

    /**
     * 저장된 Refresh token을 조회한다.
     *
     * @param memberId 회원 PK
     * @return 저장된 refresh token (없으면 null)
     */
    public String find(Long memberId) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + memberId);
    }

    /**
     * Refresh token을 삭제한다 (로그아웃/탈퇴 시).
     *
     * @param memberId 회원 PK
     */
    public void delete(Long memberId) {
        redisTemplate.delete(KEY_PREFIX + memberId);
        log.debug("Refresh token 삭제 완료: memberId={}", memberId);
    }
}
