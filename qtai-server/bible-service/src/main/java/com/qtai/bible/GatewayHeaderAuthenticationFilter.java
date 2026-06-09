package com.qtai.bible;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 게이트웨이 미경유 직접 호출을 차단하는 deny-by-default 필터 (inbound 활성 시에만 등록).
 *
 * <p>아키텍처상 인증(JWT 검증)은 게이트웨이가 수행하고, 검증된 신원을 {@code X-Member-Id}로 주입한다
 * (클라이언트가 보낸 동일 헤더는 게이트웨이가 제거 → 스푸핑 차단, #364). bible-service는 이 헤더가
 * 없으면(=게이트웨이를 거치지 않은 직접 호출) 401로 거부한다. 헬스체크({@code /actuator})는 예외.
 *
 * <p>full Spring Security 대신 경량 필터를 쓰는 이유: 본 서비스는 토큰을 직접 검증하지 않고
 * 게이트웨이 신뢰 헤더만 확인하므로, starter-security 자동설정(기본 체인·생성 비밀번호)이 불필요하다.
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    static final String HEADER_MEMBER_ID = "X-Member-Id";

    private final ObjectMapper objectMapper;

    public GatewayHeaderAuthenticationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }
        String memberId = request.getHeader(HEADER_MEMBER_ID);
        if (memberId == null || memberId.isBlank()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            ApiResponse<Void> body = ApiResponse.error(
                    ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }
        chain.doFilter(request, response);
    }
}
