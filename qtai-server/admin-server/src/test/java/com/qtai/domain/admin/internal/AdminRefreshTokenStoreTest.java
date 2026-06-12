package com.qtai.domain.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * {@link AdminRefreshTokenStore} 단위 테스트.
 *
 * <p>refresh 토큰 회전·재사용 검증 로직(저장=회전, 일치 여부, 삭제)을 {@link StringRedisTemplate}
 * mock으로 직접 검증한다.
 */
class AdminRefreshTokenStoreTest {

    private static final long REFRESH_TTL_MS = 1_209_600_000L; // 14일
    private static final String KEY = "admin:refresh:10";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private AdminRefreshTokenStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        store = new AdminRefreshTokenStore(redisTemplate, REFRESH_TTL_MS);
    }

    @Test
    @DisplayName("save → memberId 키에 토큰을 만료 TTL로 저장(회전)")
    void save_storesWithTtl() {
        store.save(10L, "new-refresh");

        verify(valueOps).set(KEY, "new-refresh", Duration.ofMillis(REFRESH_TTL_MS));
    }

    @Test
    @DisplayName("matches → 저장본과 동일하면 true")
    void matches_sameToken_true() {
        when(valueOps.get(KEY)).thenReturn("cur-refresh");

        assertThat(store.matches(10L, "cur-refresh")).isTrue();
    }

    @Test
    @DisplayName("matches → 저장본 없음(null)이면 false")
    void matches_noStored_false() {
        when(valueOps.get(KEY)).thenReturn(null);

        assertThat(store.matches(10L, "any")).isFalse();
    }

    @Test
    @DisplayName("matches → 저장본과 다르면(옛/재사용) false")
    void matches_differentToken_false() {
        when(valueOps.get(KEY)).thenReturn("cur-refresh");

        assertThat(store.matches(10L, "old-refresh")).isFalse();
    }

    @Test
    @DisplayName("delete → 키 삭제")
    void delete_removesKey() {
        store.delete(10L);

        verify(redisTemplate).delete(KEY);
    }
}
