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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 게이트웨이 미경유 직접 호출을 차단하는 deny-by-default 필터 (inbound 활성 시에만 등록).
 *
 * <p>인증(JWT 검증)은 게이트웨이가 수행하고, 신뢰 경계 진입을 두 방식으로 증명한다:
 * <ul>
 *   <li><b>공유 토큰 환경</b>({@code qtai.bible.gateway.shared-token} 설정 시): 게이트웨이가 모든 bible 라우트에
 *       주입하는 {@code X-Gateway-Token}이 일치하면 신뢰한다. 이 토큰은 ① 게이트웨이가 JWT 인증한 <b>사용자 호출</b>
 *       (신원 헤더 {@code X-Member-Id}/{@code X-Member-Role} 동반)과 ② 내부 <b>SYSTEM 서비스 호출</b>(배치/캐시 경계,
 *       사용자 컨텍스트 없음)을 모두 포함한다. bible는 읽기 전용 참조 데이터라 member 단위 인가가 없고, 신뢰 증명은
 *       토큰이다. 토큰 불일치/누락이면 401. (SYSTEM 호출은 {@code SYSTEM_BATCH} 주체로 취급한다.)</li>
 *   <li><b>토큰 미설정(dev)</b>: 공유 토큰이 없으므로 게이트웨이 주입 신원 헤더({@code X-Member-Id}+{@code X-Member-Role})를
 *       요구한다. SYSTEM 서비스 호출은 토큰 환경에서만 허용된다.</li>
 * </ul>
 * actuator(헬스체크)는 예외. full Spring Security 대신 경량 필터(starter-security 자동설정 미간섭).
 * 로그에 token 값을 남기지 않는다(CLAUDE.md §9).
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayHeaderAuthenticationFilter.class);

    static final String HEADER_MEMBER_ID = "X-Member-Id";
    static final String HEADER_MEMBER_ROLE = "X-Member-Role";
    static final String HEADER_GATEWAY_TOKEN = "X-Gateway-Token";

    private final ObjectMapper objectMapper;
    private final String expectedGatewayToken; // null/blank이면 토큰 미설정(dev) — 신원 헤더 요구

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

        if (StringUtils.hasText(expectedGatewayToken)) {
            // 토큰 환경: 유효한 X-Gateway-Token = 신뢰(게이트웨이 사용자 전달 또는 내부 SYSTEM 서비스 호출).
            if (!tokenMatches(request.getHeader(HEADER_GATEWAY_TOKEN))) {
                writeUnauthorized(response);
                return;
            }
            if (!StringUtils.hasText(request.getHeader(HEADER_MEMBER_ID))) {
                // 사용자 헤더 없음 + 토큰 유효 → SYSTEM_BATCH 주체의 서비스-to-서비스 호출(배치/캐시 경계).
                log.debug("SYSTEM 서비스 호출 허용(토큰 검증, 사용자 헤더 없음) path={}", path);
            }
            chain.doFilter(request, response);
            return;
        }

        // 토큰 미설정(dev): 게이트웨이 주입 신원 헤더(2종) 필수. SYSTEM 호출은 토큰 환경에서만.
        String memberId = request.getHeader(HEADER_MEMBER_ID);
        String memberRole = request.getHeader(HEADER_MEMBER_ROLE);
        if (!StringUtils.hasText(memberId) || !StringUtils.hasText(memberRole)) {
            log.debug("인증 실패: 토큰 미설정 환경에서 신원 헤더 누락 path={}", path);
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
