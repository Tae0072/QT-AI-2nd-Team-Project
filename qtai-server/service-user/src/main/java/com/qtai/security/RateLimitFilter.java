package com.qtai.security;

import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 공개 인증 경로 IP 단위 고정창(1분) rate limit 필터 (코드리뷰 TODO 1, P2).
 *
 * <p>대상: {@code POST /api/v1/auth/kakao}·{@code /api/v1/auth/refresh}·{@code /api/v1/admin/auth/kakao}
 * 등 permitAll 경로({@link RateLimitProperties}로 주입). 카카오 토큰 무차별 대입·refresh 남용의
 * 1차 방어선이다. 별도 라이브러리 없이 기존 Redis로 시작한다(필요 시 bucket4j 검토 — 금지기술 아님).
 *
 * <p>동작:
 * <ul>
 *   <li>키: {@code rl:{path}:{clientIp}:{epochMinute}} — INCR 후 첫 증가면 60초 EXPIRE.</li>
 *   <li>한도 초과 시 공통 에러 봉투({@link ErrorCode#RATE_LIMIT_EXCEEDED}, 429)로 즉시 응답.</li>
 *   <li><b>fail-open:</b> Redis 장애 시 카운트를 포기하고 통과시킨다 — 로그인 가용성이 한도 정확성보다
 *       우선이다. 원인은 예외 클래스명만 warn 로그로 남긴다(IP·토큰·개인정보 로그 금지, CLAUDE.md §9).</li>
 *   <li>클라이언트 IP: 기본은 {@code remoteAddr}. nginx gateway(Lead 작업) 뒤에 설 때만
 *       {@code security.rate-limit.trust-forwarded-for=true}로 {@code X-Forwarded-For} 첫 IP를 신뢰한다.</li>
 * </ul>
 *
 * <p>등록: 시큐리티 체인에서만 실행한다 — 서블릿 컨테이너 자동 등록은 SecurityConfig의
 * {@code FilterRegistrationBean(enabled=false)}로 막아 이중 카운트를 방지한다.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "rl:";
    private static final Duration WINDOW = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final SecurityErrorResponseWriter errorResponseWriter;
    private final Clock clock;
    private final Map<String, Integer> limitsByPath;

    public RateLimitFilter(StringRedisTemplate redisTemplate,
                           RateLimitProperties properties,
                           SecurityErrorResponseWriter errorResponseWriter,
                           Clock clock) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.errorResponseWriter = errorResponseWriter;
        this.clock = clock;
        this.limitsByPath = properties.rules().stream()
                .collect(Collectors.toUnmodifiableMap(
                        RateLimitProperties.Rule::path,
                        RateLimitProperties.Rule::limitPerMinute));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !limitsByPath.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = limitsByPath.get(path);

        Long count = incrementOrNull(path, clientIp(request));
        if (count != null && count > limit) {
            errorResponseWriter.write(response, ErrorCode.RATE_LIMIT_EXCEEDED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** INCR + 첫 증가 시 60초 EXPIRE. Redis 장애 시 null 반환(fail-open). */
    private Long incrementOrNull(String path, String clientIp) {
        long epochMinute = clock.instant().getEpochSecond() / 60;
        String key = KEY_PREFIX + path + ":" + clientIp + ":" + epochMinute;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW);
            }
            return count;
        } catch (DataAccessException e) {
            // fail-open: Redis 장애가 로그인 가용성을 깨지 않게 한다. IP·토큰은 로그에 남기지 않는다(§9).
            log.warn("rate limit 카운터 실패(fail-open 통과): path={}, cause={}",
                    path, e.getClass().getSimpleName());
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (properties.trustForwardedFor()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
