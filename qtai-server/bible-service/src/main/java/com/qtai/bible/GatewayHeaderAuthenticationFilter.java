package com.qtai.bible;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 게이트웨이 미경유 직접 호출을 차단하는 deny-by-default 필터 (inbound 활성 시에만 등록).
 *
 * <p>아키텍처상 인증(JWT 검증)은 게이트웨이가 수행하고, 검증된 신원을 {@code X-Member-Id}/{@code X-Member-Role}로
 * 주입한다(클라이언트가 보낸 동일 헤더는 게이트웨이가 제거 → 스푸핑 차단, #364).
 *
 * <p>2단 방어선:
 * <ol>
 *   <li><b>신원 헤더 필수</b> — 게이트웨이가 주입하는 {@code X-Member-Id}와 {@code X-Member-Role}이 모두 있어야 한다.
 *       하나라도 없으면(=게이트웨이 미경유) 401.</li>
 *   <li><b>게이트웨이 공유 토큰(선택)</b> — {@code qtai.bible.gateway.shared-token}이 설정되면 게이트웨이가 주입하는
 *       {@code X-Gateway-Token}을 상수시간 비교로 검증한다. 헤더만으로는 직접 호출자가 위조 가능하므로,
 *       토큰 일치를 추가로 요구해 게이트웨이 우회를 막는다(env 주입, 평문 키 미커밋). 게이트웨이 측 토큰 주입은 Inc2 전제.</li>
 * </ol>
 * actuator(헬스체크)는 예외. full Spring Security 대신 경량 필터(starter-security 자동설정 미간섭).
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    static final String HEADER_MEMBER_ID = "X-Member-Id";
    static final String HEADER_MEMBER_ROLE = "X-Member-Role";
    static final String HEADER_GATEWAY_TOKEN = "X-Gateway-Token";

    private final ObjectMapper objectMapper;
    private final String expectedGatewayToken; // null/blank이면 2차 방어선 비활성(설정 전)

    public GatewayHeaderAuthenticationFilter(ObjectMapper objectMapper, String expectedGatewayToken) {
        this.objectMapper = objectMapper;
        this.expectedGatewayToken = expectedGatewayToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        // 1단: 게이트웨이 주입 신원 헤더(2종) 필수
        String memberId = request.getHeader(HEADER_MEMBER_ID);
        String memberRole = request.getHeader(HEADER_MEMBER_ROLE);
        if (!StringUtils.hasText(memberId) || !StringUtils.hasText(memberRole)) {
            writeUnauthorized(response);
            return;
        }

        // 2단: 공유 토큰이 설정된 경우 일치 검증(게이트웨이 우회 차단)
        if (StringUtils.hasText(expectedGatewayToken) && !tokenMatches(request.getHeader(HEADER_GATEWAY_TOKEN))) {
            writeUnauthorized(response);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean tokenMatches(String provided) {
        if (!StringUtils.hasText(provided)) {
            return false;
        }
        // 상수시간 비교(타이밍 공격 방지)
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expectedGatewayToken.getBytes(StandardCharsets.UTF_8));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.error(
                ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
