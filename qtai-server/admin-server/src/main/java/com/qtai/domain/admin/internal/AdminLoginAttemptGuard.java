package com.qtai.domain.admin.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 관리자 로그인 브루트포스 방어(계정 단위 시도 제한).
 *
 * <p>Redis에 username 단위 실패 카운터를 두고, {@link #MAX_ATTEMPTS}회 연속 실패 시
 * {@link #LOCK} 동안 잠근다. 로그인 성공 시 카운터·잠금을 해제한다.
 *
 * <p>키는 username을 그대로 쓰지 않고 보안상 큰 의미는 없으나, 평문 비밀번호·토큰은 로그에 남기지 않는다(§9).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminLoginAttemptGuard {

    static final int MAX_ATTEMPTS = 5;
    static final Duration WINDOW = Duration.ofMinutes(10);
    static final Duration LOCK = Duration.ofMinutes(15);

    private static final String FAIL_KEY = "admin:login:fail:";
    private static final String LOCK_KEY = "admin:login:lock:";

    private final StringRedisTemplate redisTemplate;

    /** 잠금 상태면 {@link ErrorCode#ADMIN_LOGIN_RATE_LIMITED}로 차단한다. */
    public void assertNotLocked(String username) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY + username))) {
            log.warn("관리자 로그인 잠금 상태 — 시도 차단");
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_RATE_LIMITED);
        }
    }

    /** 로그인 실패 1회 기록. 임계치 도달 시 잠금을 건다. */
    public void recordFailure(String username) {
        String failKey = FAIL_KEY + username;
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(failKey, WINDOW);
        }
        if (count != null && count >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(LOCK_KEY + username, "1", LOCK);
            redisTemplate.delete(failKey);
            log.warn("관리자 로그인 연속 실패 임계치 도달 — {}분 잠금", LOCK.toMinutes());
        }
    }

    /** 로그인 성공 시 카운터·잠금 해제. */
    public void reset(String username) {
        redisTemplate.delete(FAIL_KEY + username);
        redisTemplate.delete(LOCK_KEY + username);
    }
}
