package com.qtai.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.SecurityErrorResponseWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * {@link RateLimitFilter} 단위 테스트 — 한도 내 통과 / 초과 429 / 분 경계 창 리셋 /
 * Redis 장애 fail-open / 비대상 경로 / X-Forwarded-For 신뢰 토글을 검증한다 (코드리뷰 TODO 1).
 */
class RateLimitFilterTest {

    private static final String KAKAO_PATH = "/api/v1/auth/kakao";
    private static final Instant BASE = Instant.parse("2026-06-11T03:00:00Z");

    private StringRedisTemplate redisTemplate;
    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private SecurityErrorResponseWriter writer;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        writer = mock(SecurityErrorResponseWriter.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private RateLimitFilter filter(boolean trustForwardedFor, Instant now) {
        RateLimitProperties properties = new RateLimitProperties(true, trustForwardedFor,
                List.of(new RateLimitProperties.Rule(KAKAO_PATH, 10)));
        return new RateLimitFilter(redisTemplate, properties, writer,
                Clock.fixed(now, ZoneId.of("Asia/Seoul")));
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        request.setRemoteAddr("10.0.0.7");
        return request;
    }

    @Test
    @DisplayName("한도 내 요청은 통과하고, 첫 증가에는 60초 EXPIRE를 건다")
    void 한도내_통과() throws Exception {
        when(valueOps.increment(anyString())).thenReturn(1L);
        MockFilterChain chain = new MockFilterChain();

        filter(false, BASE).doFilter(request(KAKAO_PATH), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull(); // 체인 진행됨
        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(60)));
        verify(writer, never()).write(any(), any());
    }

    @Test
    @DisplayName("한도 초과(11번째)는 429 공통 봉투로 거절하고 체인을 중단한다")
    void 한도초과_429() throws Exception {
        when(valueOps.increment(anyString())).thenReturn(11L);
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter(false, BASE).doFilter(request(KAKAO_PATH), response, chain);

        verify(writer).write(response, ErrorCode.RATE_LIMIT_EXCEEDED);
        assertThat(chain.getRequest()).isNull(); // 체인 미진행
    }

    @Test
    @DisplayName("분이 바뀌면 키의 epochMinute이 달라져 창이 리셋된다")
    void 분경계_창리셋() throws Exception {
        when(valueOps.increment(anyString())).thenReturn(1L);
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);

        filter(false, BASE).doFilter(request(KAKAO_PATH), new MockHttpServletResponse(), new MockFilterChain());
        filter(false, BASE.plusSeconds(60)).doFilter(request(KAKAO_PATH), new MockHttpServletResponse(), new MockFilterChain());

        verify(valueOps, org.mockito.Mockito.times(2)).increment(keys.capture());
        assertThat(keys.getAllValues().get(0)).isNotEqualTo(keys.getAllValues().get(1));
    }

    @Test
    @DisplayName("Redis 장애 시 fail-open — 카운트 없이 통과시킨다(로그인 가용성 우선)")
    void redis장애_failopen() throws Exception {
        when(valueOps.increment(anyString())).thenThrow(new QueryTimeoutException("redis down"));
        MockFilterChain chain = new MockFilterChain();

        filter(false, BASE).doFilter(request(KAKAO_PATH), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(writer, never()).write(any(), any());
    }

    @Test
    @DisplayName("대상 경로가 아니면 필터를 건너뛴다(카운트 없음)")
    void 비대상경로_스킵() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter(false, BASE).doFilter(request("/api/v1/me/dashboard"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(valueOps, never()).increment(anyString());
    }

    @Test
    @DisplayName("trust-forwarded-for=true면 X-Forwarded-For 첫 IP를, false면 remoteAddr를 키에 쓴다")
    void xff_신뢰_토글() throws Exception {
        when(valueOps.increment(anyString())).thenReturn(1L);
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);

        MockHttpServletRequest withXff = request(KAKAO_PATH);
        withXff.addHeader("X-Forwarded-For", "203.0.113.9, 70.41.3.18");
        filter(true, BASE).doFilter(withXff, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest spoofed = request(KAKAO_PATH);
        spoofed.addHeader("X-Forwarded-For", "203.0.113.9");
        filter(false, BASE).doFilter(spoofed, new MockHttpServletResponse(), new MockFilterChain());

        verify(valueOps, org.mockito.Mockito.times(2)).increment(keys.capture());
        assertThat(keys.getAllValues().get(0)).contains("203.0.113.9").doesNotContain("10.0.0.7");
        assertThat(keys.getAllValues().get(1)).contains("10.0.0.7").doesNotContain("203.0.113.9");
    }
}
