package com.qtai.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * DevUserIdHeaderFilter 단위 테스트.
 *
 * 검증 범위:
 * - 유효한 X-Dev-User-Id 헤더 → SecurityContext에 memberId + ROLE_USER 인증 세팅
 * - 무효한 헤더(숫자 아님) → 인증 세팅 안 됨 + 필터체인은 정상 통과
 * - 헤더 미존재 → 인증 세팅 없이 필터체인 그대로 통과
 *
 * 각 테스트 시작 전/후 SecurityContextHolder를 비워 격리 보장.
 */
class DevUserIdHeaderFilterTest {

    private static final String HEADER_NAME = "X-Dev-User-Id";

    private DevUserIdHeaderFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DevUserIdHeaderFilter();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 X-Dev-User-Id 헤더 → SecurityContext에 memberId + ROLE_USER 인증 세팅")
    void doFilter_유효헤더_인증세팅() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, "10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then — 인증이 세팅됨
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(10L);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");

        // then — 필터체인 통과
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("무효한 X-Dev-User-Id 헤더(숫자 아님) → 인증 세팅 안 되고 필터체인 통과")
    void doFilter_무효헤더_인증세팅안됨_체인통과() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, "abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then — 인증 세팅 안 됨
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        // then — 필터체인은 정상 통과
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("X-Dev-User-Id 헤더 미존재 → 인증 세팅 없이 필터체인 그대로 통과")
    void doFilter_헤더없음_그대로통과() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
