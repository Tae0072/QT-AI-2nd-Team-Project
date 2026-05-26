package com.qtai.domain.member.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * RefreshTokenStore 단위 테스트.
 *
 * StringRedisTemplate을 mock으로 주입하여 save/find/delete 동작을 검증한다.
 */
class RefreshTokenStoreTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private RefreshTokenStore refreshTokenStore;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        refreshTokenStore = new RefreshTokenStore(redisTemplate);
    }

    @Test
    void save_키_형식과_TTL_확인() {
        Duration ttl = Duration.ofDays(14);

        refreshTokenStore.save(1L, "refresh-token-value", ttl);

        verify(valueOps).set(eq("refresh:1"), eq("refresh-token-value"), eq(ttl));
    }

    @Test
    void find_존재하는_토큰_반환() {
        when(valueOps.get("refresh:1")).thenReturn("stored-token");

        String result = refreshTokenStore.find(1L);

        assertThat(result).isEqualTo("stored-token");
    }

    @Test
    void find_존재하지_않으면_null_반환() {
        when(valueOps.get("refresh:999")).thenReturn(null);

        String result = refreshTokenStore.find(999L);

        assertThat(result).isNull();
    }

    @Test
    void delete_키_삭제_확인() {
        refreshTokenStore.delete(1L);

        verify(redisTemplate).delete("refresh:1");
    }

    @Test
    void save_다른_회원_키_분리_확인() {
        Duration ttl = Duration.ofDays(14);

        refreshTokenStore.save(1L, "token-a", ttl);
        refreshTokenStore.save(2L, "token-b", ttl);

        verify(valueOps).set(eq("refresh:1"), eq("token-a"), eq(ttl));
        verify(valueOps).set(eq("refresh:2"), eq("token-b"), eq(ttl));
    }
}
