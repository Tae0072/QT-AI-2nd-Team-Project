package com.qtai.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.SecurityErrorResponseWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * {@link RateLimitFilter} 단위 테스트 — 한도 내 통과 / 초과 429 / 분 경계 창 리셋 /
 * Redis 장애 fail-open / 비대상 경로 / X-Forwarded-For 신뢰 토글(마지막 IP)을 검증한다.
 *
 * <p>INCR+EXPIRE는 Lua 스크립트 단일 호출(원자)로 실행된다 — PR #486 리뷰 후속 ③.
 */
class RateLimitFilterTest {

    private static final String KAKAO_PATH = "/api/v1/auth/kakao";
    private static final Instant BASE = Instant.parse("2026-06-11T03:00:00Z");

    private StringRedisTemplate redisTemplate;
    private SecurityErrorResponseWriter writer;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        writer = mock(SecurityErrorResponseWriter.class);
    }

    @SuppressWarnings("unchecked")
    private void stubCount(Long count) {
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any()))
                .thenReturn(count);
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

    @SuppressWarnings("unchecked")
    private List<List<String>> capturedKeys(int invocations) {
        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate, times(invocations))
                .execute(ArgumentMatchers.<RedisScript<Long>>any(), keys.capture(), any());
        return (List<List<String>>) (List<?>) keys.getAllValues();
    }

    @Test
    @DisplayName("한도 내 요청은 통과하고, 카운터는 TTL 60초 인자와 함께 원자 스크립트로 호출된다")
    void 한도내_통과() throws Exception {
        stubCount(1L);
        MockFilterChain chain = new MockFilterChain();

        filter(false, BASE).doFilter(request(KAKAO_PATH), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull(); // 체인 진행됨
        verify(redisTemplate)
                .execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), eq("60"));
        verify(writer, never()).write(any(), any());
    }

    @Test
    @DisplayName("한도 초과(11번째)는 429 공통 봉투로 거절하고 체인을 중단한다")
    void 한도초과_429() throws Exception {
        stubCount(11L);
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter(false, BASE).doFilter(request(KAKAO_PATH), response, chain);

        verify(writer).write(response, ErrorCode.RATE_LIMIT_EXCEEDED);
        assertThat(chain.getRequest()).isNull(); // 체인 미진행
    }

    @Test
    @DisplayName("분이 바뀌면 키의 epochMinute이 달라져 창이 리셋된다")
    void 분경계_창리셋() throws Exception {
        stubCount(1L);

        filter(false, BASE).doFilter(request(KAKAO_PATH), new MockHttpServletResponse(), new MockFilterChain());
        filter(false, BASE.plusSeconds(60)).doFilter(request(KAKAO_PATH), new MockHttpServletResponse(), new MockFilterChain());

        List<List<String>> keys = capturedKeys(2);
        assertThat(keys.get(0).get(0)).isNotEqualTo(keys.get(1).get(0));
    }

    @Test
    @DisplayName("Redis 장애 시 fail-open — 카운트 없이 통과시킨다(로그인 가용성 우선)")
    void redis장애_failopen() throws Exception {
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any()))
                .thenThrow(new QueryTimeoutException("redis down"));
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
        verify(redisTemplate, never())
                .execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any());
    }

    @Test
    @DisplayName("trust=true면 XFF '마지막' IP를 신뢰한다 — 선두 위조 값으로 한도를 우회할 수 없다")
    void xff_마지막_IP_신뢰() throws Exception {
        stubCount(1L);

        // 클라이언트가 선두에 위조 IP를 넣고, 게이트웨이가 실제 peer(70.41.3.18)를 append한 상황
        MockHttpServletRequest appended = request(KAKAO_PATH);
        appended.addHeader("X-Forwarded-For", "203.0.113.9, 70.41.3.18");
        filter(true, BASE).doFilter(appended, new MockHttpServletResponse(), new MockFilterChain());

        // trust=false면 XFF를 통째로 무시하고 remoteAddr 사용
        MockHttpServletRequest spoofed = request(KAKAO_PATH);
        spoofed.addHeader("X-Forwarded-For", "203.0.113.9");
        filter(false, BASE).doFilter(spoofed, new MockHttpServletResponse(), new MockFilterChain());

        List<List<String>> keys = capturedKeys(2);
        assertThat(keys.get(0).get(0)).contains("70.41.3.18").doesNotContain("203.0.113.9");
        assertThat(keys.get(1).get(0)).contains("10.0.0.7").doesNotContain("203.0.113.9");
    }
}
