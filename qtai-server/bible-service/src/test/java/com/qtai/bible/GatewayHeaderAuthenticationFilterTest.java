package com.qtai.bible;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
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

    private GatewayHeaderAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayHeaderAuthenticationFilter(new ObjectMapper().findAndRegisterModules());
    }

    @Test
    @DisplayName("X-Member-Id 헤더 없음(게이트웨이 미경유) → 401 + M0002, 다운스트림 미전달")
    void missing_member_id_returns_401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bible/books");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"success\":false").contains("M0002");
        assertThat(chain.getRequest()).isNull(); // 체인으로 전달 안 됨
    }

    @Test
    @DisplayName("게이트웨이 주입 X-Member-Id 있음 → 통과")
    void with_member_id_passes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bible/books");
        request.addHeader("X-Member-Id", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull(); // 다운스트림 전달
    }

    @Test
    @DisplayName("actuator(헬스체크)는 헤더 없이도 통과")
    void actuator_is_exempt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}
