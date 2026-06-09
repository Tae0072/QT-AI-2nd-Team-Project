package com.qtai.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.ErrorCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtValidator jwtValidator;

    @Mock
    private SecurityErrorResponseWriter errorResponseWriter;

    @Mock
    private SystemTokenValidator systemTokenValidator;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    /** 시스템 토큰 검증기를 주입할지(null이면 폴백 비활성) 선택해 필터를 만든다. */
    @SuppressWarnings("unchecked")
    private JwtAuthenticationFilter filterWith(SystemTokenValidator systemValidator) {
        ObjectProvider<SystemTokenValidator> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(systemValidator);
        return new JwtAuthenticationFilter(jwtValidator, errorResponseWriter, provider);
    }

    @BeforeEach
    void setUp() {
        // 기본: 시스템 토큰 검증기 미등록(security.jwt.system-secret 미설정 환경과 동일) → RS256만.
        filter = filterWith(null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("토큰이 없으면 인증 없이 통과")
    void 토큰이_없으면_인증없이_통과() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("유효한 사용자(RS256) 토큰이면 SecurityContext에 인증 설정")
    void 유효토큰이면_SecurityContext에_인증설정() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtValidator.validateAndGetMemberId("good-token")).thenReturn(7L);
        when(jwtValidator.extractRole("good-token")).thenReturn("USER");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(7L, auth.getPrincipal());
        assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("시스템 검증기 없이 사용자 토큰 검증 실패면 401·체인 중단 (기존 동작 유지)")
    void 검증실패면_401응답_그리고_체인중단() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtValidator.validateAndGetMemberId("bad-token")).thenThrow(new JwtException("invalid"));

        filter.doFilter(request, response, filterChain);

        verify(errorResponseWriter).write(eq(response), eq(ErrorCode.UNAUTHORIZED));
        verify(filterChain, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("사용자 토큰이 정상이면 시스템 토큰 폴백은 시도하지 않는다")
    void 사용자토큰_정상이면_시스템폴백_안함() throws Exception {
        JwtAuthenticationFilter f = filterWith(systemTokenValidator);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer user-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtValidator.validateAndGetMemberId("user-token")).thenReturn(7L);
        when(jwtValidator.extractRole("user-token")).thenReturn("USER");

        f.doFilter(request, response, filterChain);

        assertEquals(7L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(systemTokenValidator, never()).validateAndGetSystemMemberId(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("사용자(RS256) 검증 실패 시 시스템 토큰(HS256)으로 폴백해 SYSTEM_BATCH 인증")
    void 사용자검증_실패시_시스템토큰_폴백성공() throws Exception {
        JwtAuthenticationFilter f = filterWith(systemTokenValidator);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sys-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtValidator.validateAndGetMemberId("sys-token")).thenThrow(new JwtException("not a user token"));
        when(systemTokenValidator.validateAndGetSystemMemberId("sys-token")).thenReturn(0L);
        when(systemTokenValidator.systemRole()).thenReturn("SYSTEM_BATCH");

        f.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(0L, auth.getPrincipal());
        assertEquals("ROLE_SYSTEM_BATCH", auth.getAuthorities().iterator().next().getAuthority());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("사용자·시스템 검증 모두 실패면 401·체인 중단")
    void 사용자도_시스템도_실패면_401() throws Exception {
        JwtAuthenticationFilter f = filterWith(systemTokenValidator);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtValidator.validateAndGetMemberId("bad-token")).thenThrow(new JwtException("invalid user"));
        when(systemTokenValidator.validateAndGetSystemMemberId("bad-token"))
                .thenThrow(new JwtException("invalid system"));

        f.doFilter(request, response, filterChain);

        verify(errorResponseWriter).write(eq(response), eq(ErrorCode.UNAUTHORIZED));
        verify(filterChain, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
