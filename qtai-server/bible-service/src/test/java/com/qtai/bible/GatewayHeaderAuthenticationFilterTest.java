package com.qtai.bible;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게이트웨이 헤더 deny-by-default 필터 단위 테스트(스프링 컨텍스트 미사용).
 */
class GatewayHeaderAuthenticationFilterTest {

    private static final ObjectMapper OM = new ObjectMapper().findAndRegisterModules();

    private GatewayHeaderAuthenticationFilter filter(String expectedToken) {
        return new GatewayHeaderAuthenticationFilter(OM, expectedToken);
    }

    private MockHttpServletRequest req(String path) {
        return new MockHttpServletRequest("GET", path);
    }

    // ── 1단: 신원 헤더(2종) 필수 ──

    @Test
    @DisplayName("X-Member-Id 없음 → 401 + M0002, 미전달")
    void missing_member_id_returns_401() throws Exception {
        MockHttpServletRequest request = req("/api/v1/bible/books");
        request.addHeader("X-Member-Role", "USER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(null).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"success\":false").contains("M0002");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("X-Member-Role 없음 → 401 (게이트웨이는 id·role 둘 다 주입)")
    void missing_member_role_returns_401() throws Exception {
        MockHttpServletRequest request = req("/api/v1/bible/books");
        request.addHeader("X-Member-Id", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(null).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("id·role 둘 다 있음(토큰 비활성) → 통과")
    void with_both_identity_headers_passes() throws Exception {
        MockHttpServletRequest request = req("/api/v1/bible/books");
        request.addHeader("X-Member-Id", "42");
        request.addHeader("X-Member-Role", "USER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(null).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    // ── 2단: 공유 토큰(설정 시) ──

    @Test
    @DisplayName("토큰 설정됨 + X-Gateway-Token 없음 → 401 (게이트웨이 우회 차단)")
    void shared_token_required_when_configured() throws Exception {
        MockHttpServletRequest request = req("/api/v1/bible/books");
        request.addHeader("X-Member-Id", "42");
        request.addHeader("X-Member-Role", "USER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter("secret-gw-token").doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("토큰 설정됨 + 불일치 토큰 → 401")
    void shared_token_mismatch_returns_401() throws Exception {
        MockHttpServletRequest request = req("/api/v1/bible/books");
        request.addHeader("X-Member-Id", "42");
        request.addHeader("X-Member-Role", "USER");
        request.addHeader("X-Gateway-Token", "wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter("secret-gw-token").doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("토큰 설정됨 + 일치 토큰 → 통과")
    void shared_token_match_passes() throws Exception {
        MockHttpServletRequest request = req("/api/v1/bible/books");
        request.addHeader("X-Member-Id", "42");
        request.addHeader("X-Member-Role", "USER");
        request.addHeader("X-Gateway-Token", "secret-gw-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter("secret-gw-token").doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("토큰 설정됨 + 유효 토큰 + 사용자 헤더 없음 → 통과(SYSTEM 서비스 호출)")
    void systemCall_validTokenWithoutIdentityHeaders_passes() throws Exception {
        MockHttpServletRequest request = req("/api/v1/bible/books");
        request.addHeader("X-Gateway-Token", "secret-gw-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter("secret-gw-token").doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull(); // SYSTEM 호출 전달
    }

    @Test
    @DisplayName("토큰 미설정 환경에서는 사용자 헤더 없는 SYSTEM 호출 불가 → 401")
    void systemCall_withoutTokenConfigured_returns401() throws Exception {
        MockHttpServletRequest request = req("/api/v1/bible/books");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(null).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("토큰 유효 + 부분 신원 헤더(role 누락)여도 토큰이 게이트라 통과")
    void validToken_withPartialIdentityHeader_passes() throws Exception {
        MockHttpServletRequest request = req("/api/v1/bible/books");
        request.addHeader("X-Member-Id", "42"); // role 누락(부분 헤더)
        request.addHeader("X-Gateway-Token", "secret-gw-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter("secret-gw-token").doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("토큰 미설정 + 부분 신원 헤더(id만)는 401 (dev는 2종 헤더 필수)")
    void noToken_withPartialIdentityHeader_returns401() throws Exception {
        MockHttpServletRequest request = req("/api/v1/bible/books");
        request.addHeader("X-Member-Id", "42"); // role 누락
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(null).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    // ── actuator 예외 ──

    @Test
    @DisplayName("actuator(헬스체크)는 헤더 없이도 통과")
    void actuator_is_exempt() throws Exception {
        MockHttpServletRequest request = req("/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter("secret-gw-token").doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}
