package com.qtai.domain.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * {@link AdminLoginAttemptGuard} 단위 테스트.
 *
 * <p>Redis 분기 로직(잠금 차단, 실패 카운트 → 임계치 잠금, 첫 실패 시 창 TTL, 성공 리셋)을
 * {@link StringRedisTemplate} mock으로 직접 검증한다(프로젝트 RateLimitFilterTest와 동일 패턴).
 */
class AdminLoginAttemptGuardTest {

    private static final String FAIL_KEY = "admin:login:fail:admin";
    private static final String LOCK_KEY = "admin:login:lock:admin";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private AdminLoginAttemptGuard guard;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        guard = new AdminLoginAttemptGuard(redisTemplate);
    }

    @Test
    @DisplayName("잠금 키 존재 → ADMIN_LOGIN_RATE_LIMITED(429)")
    void assertNotLocked_whenLocked_throws() {
        when(redisTemplate.hasKey(LOCK_KEY)).thenReturn(true);

        assertThatThrownBy(() -> guard.assertNotLocked("admin"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ADMIN_LOGIN_RATE_LIMITED));
    }

    @Test
    @DisplayName("잠금 키 없음 → 통과")
    void assertNotLocked_whenNotLocked_passes() {
        when(redisTemplate.hasKey(LOCK_KEY)).thenReturn(false);

        assertThatCode(() -> guard.assertNotLocked("admin")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("첫 실패 → 카운터에 창 TTL 설정, 잠금 없음")
    void recordFailure_firstAttempt_setsWindowTtl() {
        when(valueOps.increment(FAIL_KEY)).thenReturn(1L);

        guard.recordFailure("admin");

        verify(redisTemplate).expire(FAIL_KEY, AdminLoginAttemptGuard.WINDOW);
        verify(valueOps, never()).set(org.mockito.ArgumentMatchers.eq(LOCK_KEY),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("임계치 미만 실패 → 잠금 미설정")
    void recordFailure_belowThreshold_noLock() {
        when(valueOps.increment(FAIL_KEY)).thenReturn((long) (AdminLoginAttemptGuard.MAX_ATTEMPTS - 1));

        guard.recordFailure("admin");

        verify(valueOps, never()).set(org.mockito.ArgumentMatchers.eq(LOCK_KEY),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("임계치 도달 실패 → 잠금 설정 + 카운터 삭제")
    void recordFailure_atThreshold_setsLock() {
        when(valueOps.increment(FAIL_KEY)).thenReturn((long) AdminLoginAttemptGuard.MAX_ATTEMPTS);

        guard.recordFailure("admin");

        verify(valueOps).set(LOCK_KEY, "1", AdminLoginAttemptGuard.LOCK);
        verify(redisTemplate).delete(FAIL_KEY);
    }

    @Test
    @DisplayName("성공 리셋 → 카운터·잠금 키 삭제")
    void reset_deletesBothKeys() {
        guard.reset("admin");

        verify(redisTemplate).delete(FAIL_KEY);
        verify(redisTemplate).delete(LOCK_KEY);
    }
}
